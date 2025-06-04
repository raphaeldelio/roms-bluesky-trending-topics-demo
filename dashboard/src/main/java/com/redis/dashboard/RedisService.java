package com.redis.dashboard;

import com.redis.om.spring.ops.RedisModulesOperations;
import com.redis.om.spring.ops.pds.BloomOperations;
import com.redis.om.spring.ops.pds.CountMinSketchOperations;
import com.redis.om.spring.ops.pds.TopKOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.Tuple;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RedisService {
    private final static Logger logger = LoggerFactory.getLogger(RedisService.class);
    private final JedisPooled jedisPooled;
    private final CountMinSketchOperations<String> countMinSketchOperations;
    private final BloomOperations<String> bloomOperations;
    private final TopKOperations<String> topKOperations;

    public RedisService(RedisModulesOperations<String> redisModulesOperations) {
        this.jedisPooled = new JedisPooled("localhost", 6379);
        this.countMinSketchOperations = redisModulesOperations.opsForCountMinSketch();
        this.bloomOperations = redisModulesOperations.opsForBloom();
        this.topKOperations = redisModulesOperations.opsForTopK();
    }

    // get key memory size
    public Long getKeyMemorySize(String key) {
        try {
            return jedisPooled.memoryUsage(key);
        } catch (JedisDataException e) {
            logger.error("Error getting memory size for key: {}", key, e);
            return 0L;
        }
    }

    public Set<String> getKeysByPrefix(String prefix) {
        Set<String> keys = new HashSet<>();
        String cursor = "0";
        ScanParams scanParams = new ScanParams().match(prefix + "*").count(100);

        do {
            ScanResult<String> scanResult = jedisPooled.scan(cursor, scanParams);
            cursor = scanResult.getCursor();
            keys.addAll(scanResult.getResult());
        } while (!cursor.equals("0"));

        return keys;
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

    // list all members with their scores
    public List<Tuple> zRangeWithScores(String key) {
        return jedisPooled.zrangeWithScores(key, 0, -1);
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

    public Map<String, Long> cmsQuery(String key, List<String> items) {
        try {
            Map<String, Long> result = new LinkedHashMap<>();
            int batchSize = 10000;

            for (int i = 0; i < items.size(); i += batchSize) {
                int end = Math.min(i + batchSize, items.size());
                logger.info("Processing items from index {} to {}", i, end - 1);
                List<String> batch = items.subList(i, end);
                List<Long> counts = countMinSketchOperations.cmsQuery(key, batch.toArray(new String[0]));

                for (int j = 0; j < batch.size(); j++) {
                    result.put(batch.get(j), counts.get(j));
                }
            }

            return result.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));
        } catch (JedisDataException e) {
            return Collections.emptyMap();
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

    public Map<String, Long> topkList(String key) {
        return topKOperations.listWithCount(key);
    }
}
