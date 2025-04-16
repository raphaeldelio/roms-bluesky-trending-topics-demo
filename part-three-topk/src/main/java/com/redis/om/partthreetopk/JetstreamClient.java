package com.redis.om.partthreetopk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@ClientEndpoint
public class JetstreamClient {

    private static final Logger logger = LoggerFactory.getLogger(JetstreamClient.class);

    private final RedisService redisService;
    private final ResourceLoader resourceLoader;

    private Session session;
    private URI endpointURI;
    private boolean manuallyClosed = false;

    public JetstreamClient(RedisService redisService, ResourceLoader resourceLoader) {
        this.redisService = redisService;
        this.resourceLoader = resourceLoader;
    }

    @OnOpen
    public void onOpen(Session session) throws IOException {
        logger.info("Connected to Bluesky stream");
        var resource = resourceLoader.getResource("classpath:stopwords-en.json");
        // read source and store it in the list
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> stopwords = objectMapper.readValue(resource.getInputStream(), List.class);
        logger.info("Stopwords loaded: " + stopwords.size());

        if (!redisService.exists("stopwords-bf")) {
            redisService.createBloomFilter("stopwords-bf", 1300, 0.01);
            redisService.addMultiToBloomFilter("stopwords-bf", stopwords.toArray(new String[0]));
            redisService.sAdd("stopwords-set", stopwords.toArray(new String[0]));
        }
    }

    @OnMessage
    public void onMessage(String message) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        for (String line : message.split("\n")) {
            Event event = mapper.readValue(line, Event.class);
            if (event.commit != null && event.commit.record != null && event.commit.record.text != null) {
                if (event.commit.record.langs != null && !event.commit.record.langs.contains("en")) {
                    return; // Skip non-English messages
                }

                String rawText = event.commit.record.text;

                // Clean and split text
                String[] rawWords = rawText.split("\\s+");
                List<String> words = new ArrayList<>();

                for (String word : rawWords) {
                    String cleaned = word
                            .replaceAll("(?<!^)#|[\\p{Punct}\\p{S}&&[^#]]", "") // remove punctuation & symbols
                            .replaceAll("^\\d+$", "")                           // replace full numeric terms
                            .toLowerCase()
                            .trim();

                    if (!cleaned.isEmpty()) {
                        if (!redisService.isInBloomFilter("stopwords-bf", cleaned)) {
                            words.add(cleaned);
                        }
                    }
                }

                String timeBucket = LocalDateTime.now().withSecond(0).withNano(0).toString();
                ensureCms(timeBucket);

                for (int i = 0; i < words.size(); i++) {
                    String word = words.get(i);

                    // Track the single word
                    redisService.sAdd("words-set", word);
                    redisService.zIncrBy("words-bucket-zset:" + timeBucket, word, 1);
                    redisService.cmsIncrBy("words-bucket-cms:" + timeBucket, word, 1);

                    // Word with previous
                    if (i > 0) {
                        String combo = words.get(i - 1) + " " + word;
                        redisService.sAdd("words-set", combo);
                        redisService.zIncrBy("words-bucket-zset:" + timeBucket, combo, 1);
                        redisService.cmsIncrBy("words-bucket-cms:" + timeBucket, combo, 1);
                    }

                    // Word with next
                    if (i < words.size() - 1) {
                        String combo = word + " " + words.get(i + 1);
                        redisService.sAdd("words-set", combo);
                        redisService.zIncrBy("words-bucket-zset:" + timeBucket, combo, 1);
                        redisService.cmsIncrBy("words-bucket-cms:" + timeBucket, combo, 1);
                    }
                }
            }
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info("Disconnected: " + closeReason);
        if (!manuallyClosed) {
            tryReconnect();
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket error: " + throwable.getMessage());
        if (!manuallyClosed) {
            tryReconnect();
        }
    }

    public void start(String uri) throws Exception {
        this.endpointURI = new URI(uri);
        connect();
    }

    private void connect() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        this.session = container.connectToServer(this, endpointURI);
    }

    private void tryReconnect() {
        new Thread(() -> {
            int attempts = 0;
            while (!manuallyClosed) {
                try {
                    Thread.sleep(Math.min(30000, 2000 * ++attempts)); // exponential up to 30s
                    logger.info("Trying to reconnect... attempt " + attempts);
                    connect();
                    logger.info("Reconnected!");
                    break;
                } catch (Exception e) {
                    System.err.println("Reconnect failed: " + e.getMessage());
                }
            }
        }).start();
    }

    public void stop() throws IOException {
        manuallyClosed = true;
        if (session != null && session.isOpen()) {
            session.close();
        }
    }

    private void ensureCms(String timeBucket) {
        String key = "words-bucket-cms:" + timeBucket;
        if (!redisService.exists(key)) {
            redisService.createCms(key, 1000, 7);
        }
    }
}