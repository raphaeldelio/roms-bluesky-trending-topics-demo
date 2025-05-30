package com.redis.om.parttwobloomfilter;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PartTwoBloomfilterApplication {
    public static void main(String[] args) {
        SpringApplication.run(PartTwoBloomfilterApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(
            JetstreamClient client) {
        return args -> client.start("wss://jetstream2.us-west.bsky.network/subscribe?wantedCollections=app.bsky.feed.post");
    }
}
