package com.redis.om.partthreetopk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class JetstreamProcessor {

    private final static Logger logger = LoggerFactory.getLogger(JetstreamProcessor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResourceLoader resourceLoader;
    private final RedisService redisService;

    public JetstreamProcessor(ResourceLoader resourceLoader, RedisService redisService) {
        this.resourceLoader = resourceLoader;
        this.redisService = redisService;
        loadStopwords();
    }

    public void process(String rawText) {
        List<String> words = Arrays.stream(rawText.split("\\s+"))
                .map(this::cleanWord)
                .filter(w -> !w.isEmpty() && !redisService.isInBloomFilter("stopwords-bf", w))
                .toList();

        String timeBucket = getCurrentTimeBucket();
        ensureCms(timeBucket);
        processWords(words, timeBucket);
    }

    private String cleanWord(String word) {
        return word.replaceAll("(?<!^)#|[\\p{Punct}\\p{S}&&[^#]]", "")
                .replaceAll("^\\d+$", "")
                .toLowerCase()
                .trim();
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

    private void loadStopwords() {
        try {
            var resource = resourceLoader.getResource("classpath:stopwords-en.json");
            List<String> words = objectMapper.readValue(resource.getInputStream(), List.class);

            if (!redisService.exists("stopwords-bf")) {
                redisService.createBloomFilter("stopwords-bf", 1300, 0.01);
                redisService.addMultiToBloomFilter("stopwords-bf", words.toArray(new String[0]));
                redisService.sAdd("stopwords-set", words.toArray(new String[0]));
                logger.info("Stopwords loaded: {}", words.size());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load stopwords", e);
        }
    }
}
