package com.redis.om.parttwobloomfilter;

import com.redis.om.spring.ops.RedisModulesOperations;
import com.redis.om.spring.ops.pds.BloomOperations;
import com.redis.om.spring.ops.pds.CountMinSketchOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

@Service
public class RedisService {
    private final static Logger logger = LoggerFactory.getLogger(RedisService.class);
    private final JedisPooled jedisPooled;
    private final CountMinSketchOperations<String> countMinSketchOperations;
    private final BloomOperations<String> bloomOperations;

    public RedisService(RedisModulesOperations<String> redisModulesOperations) {
        this.jedisPooled = new JedisPooled("localhost", 6379);
        this.countMinSketchOperations = redisModulesOperations.opsForCountMinSketch();
        this.bloomOperations = redisModulesOperations.opsForBloom();
    }

    public boolean exists(String key) {
        return jedisPooled.exists(key);
    }

    // Set methods
    public void sAdd(String key, String... value) {
        jedisPooled.sadd(key, value);
    }

    // Sorted set methods
    public void zIncrBy(String key, String member, double score) {
        jedisPooled.zincrby(key, score, member);
    }

    // Count-Min Sketch methods
    public void createCms(String key, int width, int depth) {
        try {
            countMinSketchOperations.cmsInitByDim(key, width, depth);
        } catch (Exception e) {
            logger.error("Error creating Count-Min Sketch: ", e);
        }
    }

    public void cmsIncrBy(String key, String item, int count) {
        try {
            countMinSketchOperations.cmsIncrBy(key, item, count);
        } catch (Exception e) {
            logger.error("Error creating Count-Min Sketch: ", e);
        }
    }

    // Bloom Filter
    public void createBloomFilter(String key, int expectedItems, double falsePositiveRate) {
        try {
            bloomOperations.createFilter(key, expectedItems, falsePositiveRate);
        } catch (Exception e) {
            logger.error("Error creating Bloom Filter: ", e);
        }
    }

    public void addMultiToBloomFilter(String key, String... items) {
        try {
            bloomOperations.addMulti(key, items);
        } catch (Exception e) {
            logger.error("Error adding items to Bloom Filter: ", e);
        }
    }

    public boolean isInBloomFilter(String key, String item) {
        try {
            return bloomOperations.exists(key, item);
        } catch (Exception e) {
            logger.error("Error checking Bloom Filter: ", e);
            return false;
        }
    }
}
