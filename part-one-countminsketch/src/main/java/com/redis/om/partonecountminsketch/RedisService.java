package com.redis.om.partonecountminsketch;

import com.redis.om.spring.client.RedisModulesClient;
import com.redis.om.spring.ops.RedisModulesOperations;
import com.redis.om.spring.ops.pds.BloomOperations;
import com.redis.om.spring.ops.pds.CountMinSketchOperations;
import com.redis.om.spring.ops.pds.TopKOperations;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.Set;

@Service
public class RedisService {
    private final JedisPooled jedisPooled;
    private final RedisModulesOperations<String> redisModulesOperations;
    private final CountMinSketchOperations<String> countMinSketchOperations;
    private final BloomOperations<String> bloomOperations;
    private final TopKOperations<String> topKOperations;

    public RedisService(RedisModulesClient redisModulesClient, RedisModulesOperations<String> redisModulesOperations) {
        this.redisModulesOperations = redisModulesOperations;
        this.jedisPooled = new JedisPooled();
        this.countMinSketchOperations = redisModulesOperations.opsForCountMinSketch();
        this.bloomOperations = redisModulesOperations.opsForBloom();
        this.topKOperations = redisModulesOperations.opsForTopK();
    }

    public boolean exists(String key) {
        return jedisPooled.exists(key);
    }

    // Set methods
    public void sAdd(String key, String... value) {
        jedisPooled.sadd(key, value);
    }

    public Set<String> sMembers(String key) {
        return jedisPooled.smembers(key);
    }

    // Sorted set methods
    public void zIncrBy(String key, String member, double score) {
        jedisPooled.zincrby(key, score, member);
    }

    // Count-Min Sketch methods
    public void createCms(String key, int width, int depth) {
        countMinSketchOperations.cmsInitByDim(key, width, depth);
    }

    public void cmsIncrBy(String key, String item, int count) {
        countMinSketchOperations.cmsIncrBy(key, item, count);
    }

    public Long cmsQuery(String key, String item) {
        try {
            return countMinSketchOperations.cmsQuery(key, item).stream().findFirst().orElse(0L);
        } catch (JedisDataException e) {
            return 0L;
        }
    }

    // Bloom Filter
    public void createBloomFilter(String key, int expectedItems, double falsePositiveRate) {
        bloomOperations.createFilter(key, expectedItems, falsePositiveRate);
    }

    public void addMultiToBloomFilter(String key, String... items) {
        bloomOperations.addMulti(key, items);
    }

    public boolean isInBloomFilter(String key, String item) {
        return bloomOperations.exists(key, item);
    }

    // TopK methods
    public void initTopK(String key, int topK, int width, int depth, double decay) {
        topKOperations.createFilter(key, topK, width, depth, decay);
    }

    public void topkIncrBy(String key, String term, int incrBy) {
        topKOperations.incrementBy(key, term, incrBy);
    }
}
