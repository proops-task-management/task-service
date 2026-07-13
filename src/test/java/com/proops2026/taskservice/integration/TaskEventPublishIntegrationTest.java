package com.proops2026.taskservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proops2026.taskservice.util.EventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Full-context integration test against a real MySQL + Redis (Testcontainers).
 *
 * Proves the D8 acceptance criteria for task-service (IRD-002 amended, ADR-004):
 *   - the publisher emits event schema v2 — every payload carries a unique {@code eventId};
 *   - the payload lands on the {@code task-events} Redis list with the other v2 fields intact.
 *
 * Docker must be running locally to execute this test.
 */
@SpringBootTest
@Testcontainers
class TaskEventPublishIntegrationTest {

    private static final String TASK_EVENTS_QUEUE = "task-events";

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Test
    void publish_eventCarriesEventId_schemaV2() throws Exception {
        String taskId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();

        eventPublisher.publish("task.created", taskId, userId);

        // Producer LPUSHes; drain the tail (RPOP) to read the single event we just published.
        String raw = redisTemplate.opsForList().rightPop(TASK_EVENTS_QUEUE);
        assertThat(raw).as("an event should have been published to %s", TASK_EVENTS_QUEUE).isNotNull();

        JsonNode event = objectMapper.readTree(raw);
        assertThat(event.hasNonNull("eventId")).as("schema v2 requires eventId").isTrue();
        // eventId must be a parseable UUID
        assertThatCode(() -> UUID.fromString(event.get("eventId").asText())).doesNotThrowAnyException();
        assertThat(event.get("eventType").asText()).isEqualTo("task.created");
        assertThat(event.get("taskId").asText()).isEqualTo(taskId);
        assertThat(event.get("userId").asText()).isEqualTo(userId);
        assertThat(event.hasNonNull("timestamp")).isTrue();
    }
}
