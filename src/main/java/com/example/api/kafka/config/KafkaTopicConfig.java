package com.example.api.kafka.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfig {

    private final KafkaTopicProperties properties;

    @Bean
    public NewTopic productTopic() {
        return TopicBuilder.name(properties.getProductTopic())
                .partitions(1)
                .replicas(1)
                .build();
    }
}