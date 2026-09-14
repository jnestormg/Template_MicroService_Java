package com.example.api.kafka.consumer;

import com.example.api.product.event.ProductEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductEventConsumer {

    @KafkaListener(topics = "${app.kafka.product-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onProductEvent(ProductEvent event) {
        log.info("Evento de producto recibido: id={}, nombre={}, precio={}, disponible={}",
                event.id(), event.name(), event.price(), event.available());
    }
}