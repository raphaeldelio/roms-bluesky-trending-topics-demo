package dev.raphaeldelio.countminsketchprobabilisticdatastructure;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CountMinSketchProbabilisticDataStructureApplication {

    public static void main(String[] args) {
        SpringApplication.run(CountMinSketchProbabilisticDataStructureApplication.class, args);
    }


    @Bean
    public CommandLineRunner run(
            JetstreamClient client) {
        return args -> {
            client.start("wss://jetstream2.us-east.bsky.network/subscribe?wantedCollections=app.bsky.feed.post"); // this is the firehose, replace with Jetstream URL if needed
        };}
}
