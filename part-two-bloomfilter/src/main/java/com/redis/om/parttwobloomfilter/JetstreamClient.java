package com.redis.om.parttwobloomfilter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.websocket.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
@ClientEndpoint
public class JetstreamClient {

    private static final Logger logger = LoggerFactory.getLogger(JetstreamClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final DeduplicationService deduplicationService;
    private final JetstreamProcessor processor;

    private Session session;
    private URI endpointURI;
    private boolean manuallyClosed = false;

    public JetstreamClient(DeduplicationService deduplicationService, JetstreamProcessor processor) {
        this.deduplicationService = deduplicationService;
        this.processor = processor;
    }

    @OnOpen
    public void onOpen(Session session) {
        logger.info("Connected to Bluesky stream");
    }

    @OnMessage
    public void onMessage(String message) throws JsonProcessingException {
        for (String line : message.split("\n")) {
            Event event = objectMapper.readValue(line, Event.class);

            if (event.commit == null || event.commit.record == null || event.commit.record.text == null) {
                continue;
            }

            if (event.commit.record.langs != null && !event.commit.record.langs.contains("en")) {
                continue; // Skip non-English
            }

            String uri = "at://" + event.did + "/app.bsky.feed.post/" + event.commit.rkey;
            if (!deduplicationService.isNewMessage(uri)) {
                logger.debug("Skipping duplicate message");
                return;
            }

            processor.process(event.commit.record.text);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.info("Disconnected: {}", closeReason);
        if (!manuallyClosed) tryReconnect();
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.error("WebSocket error: {}", throwable.getMessage());
        if (!manuallyClosed) tryReconnect();
    }

    public void start(String uri) throws Exception {
        this.endpointURI = new URI(uri);
        connect();
    }

    @PreDestroy
    public void stop() throws IOException {
        manuallyClosed = true;
        if (session != null && session.isOpen()) {
            session.close();
        }
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
                    Thread.sleep(backoffDelay(++attempts));
                    logger.info("Trying to reconnect... attempt {}", attempts);
                    connect();
                    logger.info("Reconnected!");
                    break;
                } catch (Exception e) {
                    logger.warn("Reconnect failed: {}", e.getMessage());
                }
            }
        }).start();
    }

    private long backoffDelay(int attempts) {
        return Math.min(30_000, 2000L * attempts); // max 30s
    }
}