package com.redis.dashboard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.resps.Tuple;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final static Logger logger = LoggerFactory.getLogger(ApiController.class);
    private final RedisService redisService;

    public ApiController(RedisService redisService) {
        this.redisService = redisService;
    }

    @GetMapping("/keys")
    public Map<String, String> getKeysByPrefix(@RequestParam String prefix) {
        Set<String> keys = redisService.getKeysByPrefix(prefix);
        return keys.stream()
                .map(key -> Map.entry(key, redisService.getKeyMemorySize(key)))
                .sorted(Map.Entry.<String, Long>comparingByKey().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> formatSize(entry.getValue()),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private String formatSize(long bytes) {
        if (bytes >= 1_048_576) {
            return String.format("%.2f MB", bytes / 1_048_576.0);
        } else if (bytes >= 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return bytes + " B";
        }
    }

    @GetMapping("/data")
    public Object getDataForKey(@RequestParam String key, @RequestParam String type) {
        switch (type) {
            case "cms":
                logger.info("Requested key: {}, type: {}", key, type);
                List<String> words = redisService.sMembers("words-set").stream().toList();
                return redisService.cmsQuery(key, words).entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .limit(100)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                LinkedHashMap::new
                        ));

            case "bf":
                return null;

            case "topk":
                return redisService.topkList(key);

            case "set":
                return redisService.sMembers(key).stream().limit(1000).collect(Collectors.toList());

            case "zset":
                return redisService.zRangeWithScores(key).stream()
                        .sorted(Comparator.comparingDouble(Tuple::getScore).reversed()) // sort by score descending
                        .limit(100)
                        .collect(Collectors.toMap(
                                Tuple::getElement,
                                Tuple::getScore,
                                (e1, e2) -> e1,
                                LinkedHashMap::new
                        ));

            default:
                return Collections.singletonMap("error", "Unsupported data type");
        }
    }

    @GetMapping("/bloom/check")
    public Map<String, Boolean> checkBloomFilter(@RequestParam String key, @RequestParam String item) {
        logger.info("Checking if item '{}' exists in Bloom Filter '{}'", item, key);
        boolean exists = redisService.isInBloomFilter(key, item);
        logger.info("Item '{}' {} in Bloom Filter '{}'", item, exists ? "might exist" : "definitely does not exist", key);
        return Collections.singletonMap("exists", exists);
    }
}
