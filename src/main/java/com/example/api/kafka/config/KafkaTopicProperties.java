package com.example.api.kafka.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
@Getter
@Setter
public class KafkaTopicProperties {

    private String productTopic;
}