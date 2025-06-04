package com.redis.om.partthreetopk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
public class SpikeDetector {

    private static final Logger logger = LoggerFactory.getLogger(SpikeDetector.class);
    private final RedisService redisService;

    public SpikeDetector(RedisService redisService) {
        this.redisService = redisService;
    }

    @Scheduled(fixedRate = 60_000)
    public void computeSpikes() {
        logger.info("Calculating spikes...");
        String now = bucketKey(0);
        String prev1 = bucketKey(-1);
        String prev2 = bucketKey(-2);
        String prev3 = bucketKey(-3);

        Set<String> terms = redisService.sMembers("words-set");

        // Ensure TopK exists
        String currentBucket = getCurrentTimeBucket();
        String topKKey = "spiking-topk:" + currentBucket;
        if (!redisService.exists(topKKey)) {
            redisService.initTopK(topKKey, 10, 3000, 12, 0.9);
        }

        for (String term : terms) {
            long current = redisService.cmsQuery(now, term);
            long pastAvg = (redisService.cmsQuery(prev1, term)
                    + redisService.cmsQuery(prev2, term)
                    + redisService.cmsQuery(prev3, term)) / 3;

            if (pastAvg > 0) {
                double spikeScore = (current - pastAvg) / (double) pastAvg;

                // Push into ZSET for full scoring
                redisService.zIncrBy("spiking-zset:" + currentBucket, term, spikeScore);

                // Push into TopK (rank only, no score)
                redisService.topkIncrBy(topKKey, term, (int) spikeScore);
            }
        }

        logger.info("Spikes calculated and stored.");
    }

    private String bucketKey(int minutesAgo) {
        return "words-bucket-cms:" + LocalDateTime.now()
                .minusMinutes(minutesAgo)
                .withSecond(0).withNano(0);
    }

    private String getCurrentTimeBucket() {
        return LocalDateTime.now().withSecond(0).withNano(0).toString();
    }
}