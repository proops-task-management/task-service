package com.proops2026.taskservice.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private static final String TASK_EVENTS_QUEUE = "task-events";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String eventType, String taskId, String userId) {
        try {
            // Event schema v2 (IRD-002 amended): every event carries a unique eventId so the
            // consumer can dedupe (notification-service processed_events ledger, IRD-004/ADR-004).
            // Additive over v1 — a consumer that ignores eventId still works during transition.
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "eventType", eventType,
                    "taskId", taskId,
                    "userId", userId,
                    "timestamp", Instant.now().toString()
            ));
            redisTemplate.opsForList().leftPush(TASK_EVENTS_QUEUE, payload);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize event {} for task {}", eventType, taskId, ex);
        } catch (RuntimeException ex) {
            log.warn("Failed to publish event {} for task {}", eventType, taskId, ex);
        }
    }
}

