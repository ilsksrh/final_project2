package kz.project.producer.controller;

import kz.project.producer.dto.AppealEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EventProducerController {

    private final KafkaTemplate<String, AppealEvent> kafkaTemplate;

    @PostMapping("/send")
    public String sendEvent(@RequestBody AppealEvent request) {
        if (request.getEventId() == null) {
            request.setEventId(UUID.randomUUID());
        }

        if (request.getCreatedAt() == null) {
            request.setCreatedAt(OffsetDateTime.now());
        }

        kafkaTemplate.send("topic-1", request.getEventId().toString(), request);
        log.info("Sent event to topic-1: {}", request);

        return "Event sent successfully: " + request.getEventId();
    }

    @PostMapping("/send-test")
    public String sendTestEvent() {
        AppealEvent event = new AppealEvent();
        event.setEventId(UUID.randomUUID());
        event.setClientId(123L);
        event.setCategory("urgent");
        event.setTheme("Тестовое обращение");
        event.setCreatedAt(OffsetDateTime.now());

        kafkaTemplate.send("topic-1", event.getEventId().toString(), event);
        log.info("Sent test event: {}", event);

        return "Test event sent: " + event.getEventId();
    }
}