package com.proops2026.taskservice.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private static final String TASK_EVENTS_QUEUE = "task-events";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String eventType, String taskId, String userId) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventType", eventType,
                    "taskId", taskId,
                    "userId", userId,
                    "timestamp", Instant.now().toString()
            ));
            redisTemplate.opsForList().leftPush(TASK_EVENTS_QUEUE, payload);
        } catch (JsonProcessingException ex) {
            log.error("Failed to publish event {} for task {}", eventType, taskId);
        }
    }
}

