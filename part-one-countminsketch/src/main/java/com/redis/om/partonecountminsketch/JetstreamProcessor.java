package com.redis.om.partonecountminsketch;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class JetstreamProcessor {

    private final RedisService redisService;

    public JetstreamProcessor(RedisService redisService) {
        this.redisService = redisService;
    }

    public void process(String rawText) {
        List<String> words = cleanAndSplitWords(rawText);
        String timeBucket = getCurrentTimeBucket();
        ensureCms(timeBucket);
        processWords(words, timeBucket);
    }

    private List<String> cleanAndSplitWords(String rawText) {
        return Arrays.stream(rawText.split("\\s+"))
                .map(word -> word.replaceAll("(?<!^)#|[\\p{Punct}\\p{S}&&[^#]]", "")
                                 .replaceAll("^\\d+$", "")
                                 .toLowerCase()
                                 .trim())
                .filter(w -> !w.isEmpty())
                .toList();
    }

    private String getCurrentTimeBucket() {
        return LocalDateTime.now().withSecond(0).withNano(0).toString();
    }

    private void processWords(List<String> words, String timeBucket) {
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            updateMetrics(word, timeBucket);

            if (i > 0) updateMetrics(words.get(i - 1) + " " + word, timeBucket);
            if (i < words.size() - 1) updateMetrics(word + " " + words.get(i + 1), timeBucket);
        }
    }

    private void updateMetrics(String term, String timeBucket) {
        redisService.sAdd("words-set", term);
        redisService.zIncrBy("words-bucket-zset:" + timeBucket, term, 1);
        redisService.cmsIncrBy("words-bucket-cms:" + timeBucket, term, 1);
    }

    private void ensureCms(String timeBucket) {
        String key = "words-bucket-cms:" + timeBucket;
        if (!redisService.exists(key)) {
            redisService.createCms(key, 2000, 10);
        }
    }
}