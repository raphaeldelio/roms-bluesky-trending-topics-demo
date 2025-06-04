package com.redis.om.parttwobloomfilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for detecting and filtering duplicate messages using Bloom filters.
 */
@Service
public class DeduplicationService {
    private static final Logger logger = LoggerFactory.getLogger(DeduplicationService.class);
    private static final String DEDUP_BLOOM_FILTER = "message-dedup-bf";
    private static final String DEDUP_SET = "message-dedup-set";
    private static final int EXPECTED_ITEMS = 100000; // Adjust based on expected volume
    private static final double FALSE_POSITIVE_RATE = 0.01; // 1% false positive rate
    
    private final RedisService redisService;
    
    public DeduplicationService(RedisService redisService) {
        this.redisService = redisService;
        initializeBloomFilter();
    }
    
    /**
     * Initialize the bloom filter for message deduplication if it doesn't exist.
     */
    private void initializeBloomFilter() {
        if (!redisService.exists(DEDUP_BLOOM_FILTER)) {
            logger.info("Creating deduplication bloom filter");
            redisService.createBloomFilter(DEDUP_BLOOM_FILTER, EXPECTED_ITEMS, FALSE_POSITIVE_RATE);
        }
    }

    public boolean isNewMessage(String uri) {
        // Check if the message hash exists in the bloom filter
        boolean isDuplicate = redisService.isInBloomFilter(DEDUP_BLOOM_FILTER, uri);
        
        if (!isDuplicate) {
            // If it's not a duplicate, add it to the bloom filter
            redisService.addMultiToBloomFilter(DEDUP_BLOOM_FILTER, uri);
            redisService.sAdd(DEDUP_SET, uri);
            return true;
        }
        
        logger.debug("Duplicate message detected: {}",uri);
        return false;
    }
}