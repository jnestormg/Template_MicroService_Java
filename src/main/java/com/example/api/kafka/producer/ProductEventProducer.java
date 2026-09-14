package com.example.api.kafka.producer;

import com.example.api.kafka.config.KafkaTopicProperties;
import com.example.api.product.event.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties properties;

    public void publish(ProductEvent event) {
        kafkaTemplate.send(properties.getProductTopic(), event.id().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Evento publicado en {} con offset {}", properties.getProductTopic(),
                                result != null ? result.getRecordMetadata().offset() : null);
                    } else {
                        log.error("Fallo al publicar evento de producto {}", event.id(), ex);
                    }
                });
    }
}