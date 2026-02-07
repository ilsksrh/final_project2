package kz.project.moderation.consumer;

import kz.project.moderation.dto.AppealEvent;
import kz.project.moderation.service.ModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
        log.debug("Received event from Kafka: eventId={}", event.getEventId());
        moderationService.process(event)
                .doOnError(error -> log.error(
                        "Error processing event {}: {}",
                        event.getEventId(),
                        error.getMessage()
                ))
                .onErrorResume(e -> Mono.empty())
                .subscribe();

    }
}
