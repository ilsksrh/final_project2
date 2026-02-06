package kz.project.moderation.consumer;

import kz.project.moderation.dto.AppealEvent;
import kz.project.moderation.service.ModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppealKafkaConsumer {

    private final ModerationService moderationService;

    @KafkaListener(
            topics = "topic-1",
            groupId = "moderation-group"
    )
    public void consume(AppealEvent event) {
        log.debug("Received event from Kafka: {}", event);
        moderationService.process(event);
    }
}
