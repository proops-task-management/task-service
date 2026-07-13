# task-service — Claude Agent

## Read these first (via Notion MCP)
- DOP-001: https://www.notion.so/3412ba48f4878140a5ebf3df60896b9b
- IRD-002: https://www.notion.so/3412ba48f48781c89938c3078a3d09ec

---

## Scope
This repo contains task-service only.
Only implement what is defined in IRD-002. Nothing more.

---

## Events & Observability (Production Program — D8, IRD-002 amended + ADR-004)

**Event schema is v2 — every event carries `eventId`.**
- `EventPublisher` generates `eventId = UUID.randomUUID().toString()` per event; the consumer
  (notification-service) uses it for idempotent processing via a `processed_events` ledger
  (IRD-004/ADR-004). v2 is **additive** — a consumer that ignores `eventId` still works.
- Publisher stays best-effort/fire-and-forget (at-most-once end-to-end, ADR-004); still
  `LPUSH task-events`.

**Actuator endpoints exposed** (allowlist `health,prometheus`):
- `/actuator/health/liveness` + `/actuator/health/readiness` — wired to k8s probes.
- `/actuator/prometheus` — Micrometer; histogram buckets capped to the SLO set
  `100ms,300ms,1s,3s` (IRD-020 cardinality budget). No auth (gateway enforces upstream).

**Production hardening:** `server.shutdown=graceful` (20s drain), HikariCP `maximum-pool-size=10`
(IRD-018 budget), JVM `-XX:MaxRAMPercentage=75.0` set in the **Dockerfile** (`JAVA_TOOL_OPTIONS`),
never in `application.yml`.

**Integration tests** run against **real MySQL + Redis via Testcontainers** — Docker must be
running locally. See `integration/TaskEventPublishIntegrationTest`.

---

## NEVER
- Generate code for user-service, api-gateway, notification-service, or frontend-service
- Add endpoints not defined in IRD-002
- Validate JWT — trust X-User-Id and X-User-Role headers from api-gateway only
- Call user-service at runtime — userId and role come from headers, never from a service call
- Join tasks_db to users_db — store assignee_id and created_by as plain UUIDs only
- Hardcode secrets — all config via env vars
- Use `ddl-auto=create` or `update` — Flyway manages schema
- Call repository directly from controller — always go through service
- Return `@Entity` from controller — always map to DTO via MapStruct
- Use try/catch in controller or serviceImpl — throw exceptions, let GlobalExceptionHandler catch
- Write methods longer than 20 lines — split into private helpers
- Use `@Data` on JPA entities — use `@Getter` + `@Setter` + `@Builder` separately
- Use `@Autowired` for dependency injection — always use `@RequiredArgsConstructor` + `private final`
- Use `System.out.println` — always use `@Slf4j` + `log.info()`
- Allow a member to see another member's tasks — member scope is always own tasks only
- Skip cache eviction on any mutation — evict on POST /tasks, PATCH /tasks/:id/*, DELETE /tasks/:id

---

## Spring Boot Code Conventions

---

### Directory Structure

```
src/main/java/com/proops2026/taskservice/
├── controller/        HTTP layer only — receive request, return response, no logic
│   ├── TaskController.java
│   └── CommentController.java
├── service/           Interfaces + implementations
│   ├── TaskService.java
│   ├── CommentService.java
│   ├── OverdueJobService.java
│   └── impl/
│       ├── TaskServiceImpl.java
│       ├── CommentServiceImpl.java
│       └── OverdueJobServiceImpl.java
├── repository/        JpaRepository interfaces + custom queries
│   ├── TaskRepository.java
│   ├── CommentRepository.java
│   └── impl/
│       └── TaskRepositoryImpl.java   ← complex filter queries
├── model/             JPA entities — maps to database tables
│   ├── Task.java
│   └── Comment.java
├── dto/
│   ├── request/       Input objects — what the client sends
│   │   ├── CreateTaskRequest.java
│   │   ├── AssignTaskRequest.java
│   │   ├── UpdateStatusRequest.java
│   │   ├── UpdateMetadataRequest.java
│   │   └── AddCommentRequest.java
│   └── response/      Output objects — what the client receives
│       ├── TaskResponse.java
│       ├── TaskListResponse.java
│       └── CommentResponse.java
├── mapper/            MapStruct interfaces — entity ↔ DTO conversion
├── util/
│   └── EventPublisher.java   ← LPUSH task-events to Redis
├── exception/
│   ├── TaskNotFoundException.java
│   ├── UnauthorizedException.java
│   └── GlobalExceptionHandler.java
└── TaskServiceApplication.java
```

---

### Lombok — Required Annotations

**Entity (`model/`):**
```java
@Entity
@Table(name = "tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    private String id;
    private String title;
    private String description;
    private String status;       // "todo" | "in_progress" | "done"
    private String assigneeId;
    private String createdBy;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```
> Do NOT use `@Data` on entities — causes issues with JPA lazy loading and equals/hashCode.

**Request DTO (`dto/request/`):**
```java
@Getter
@Setter
public class CreateTaskRequest {
    @NotBlank(message = "title is required")
    @Size(max = 200, message = "title must be at most 200 characters")
    private String title;

    private String description;

    private LocalDate dueDate;
}
```

**Response DTO (`dto/response/`):**
```java
@Getter
@Builder
public class TaskResponse {
    private String id;
    private String title;
    private String description;
    private String status;
    private String assigneeId;
    private String createdBy;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**ServiceImpl:**
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final EventPublisher eventPublisher;
}
```
> Use `@RequiredArgsConstructor` + `private final` — never use `@Autowired`.

---

### Layer Rules

**Controller** — HTTP only, no business logic, no auth checks
```java
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Validated
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(201).body(taskService.createTask(userId, role, request));
    }
}
```
> Always extract `X-User-Id` and `X-User-Role` via `@RequestHeader`. Missing headers → 401 (handled by filter).

**Service (interface)** — one method per use case
```java
public interface TaskService {
    TaskResponse createTask(String userId, String role, CreateTaskRequest request);
    TaskResponse assignTask(String userId, String role, String taskId, AssignTaskRequest request);
    TaskResponse updateStatus(String userId, String role, String taskId, UpdateStatusRequest request);
    TaskResponse updateMetadata(String userId, String role, String taskId, UpdateMetadataRequest request);
    void deleteTask(String userId, String role, String taskId);
    TaskListResponse listTasks(String userId, String role, TaskFilters filters);
    TaskResponse getTask(String taskId);
}
```

**ServiceImpl** — all business logic, role checks, cache, event publishing
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    @CacheEvict(value = "tasks", allEntries = true)
    public TaskResponse createTask(String userId, String role, CreateTaskRequest request) {
        Task saved = taskRepository.save(buildTask(request, userId));
        eventPublisher.publish("task.created", saved.getId(), userId);
        log.info("Task created: {} by {}", saved.getId(), userId);
        return taskMapper.toResponse(saved);
    }

    private Task buildTask(CreateTaskRequest request, String userId) {
        return Task.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .status("todo")
            .createdBy(userId)
            .dueDate(request.getDueDate())
            .build();
    }
}
```

---

### Authorization Rules (enforce in ServiceImpl, not Controller)

| Action          | Who                                |
|-----------------|------------------------------------|
| Create task     | Any (X-User-Role present)          |
| List tasks      | member → own tasks · lead → all    |
| Get one task    | Any                                |
| Assign task     | lead only                          |
| Update status   | assignee_id or created_by only     |
| Edit metadata   | lead only                          |
| Delete task     | lead only                          |
| Add comment     | Any                                |

```java
private void requireLead(String role) {
    if (!"lead".equals(role)) {
        throw new UnauthorizedException("only team leads can assign tasks");
    }
}

private void requireOwnership(Task task, String userId, String role) {
    boolean isOwner = userId.equals(task.getAssigneeId()) || userId.equals(task.getCreatedBy());
    if (!isOwner && !"lead".equals(role)) {
        throw new UnauthorizedException("you do not have permission to update this task");
    }
}
```

---

### Cache Strategy

```java
// Cache task list — user-scoped
@Cacheable(value = "tasks", key = "#userId + ':' + #filters")
public TaskListResponse listTasks(String userId, String role, TaskFilters filters) { ... }

// Cache single task
@Cacheable(value = "task", key = "#taskId")
public TaskResponse getTask(String taskId) { ... }

// Evict all task list cache on any mutation
@CacheEvict(value = "tasks", allEntries = true)
public TaskResponse createTask(...) { ... }
```

- Cache key for lists: `tasks:{userId}:list:{filters}` — TTL 30s
- Cache key for single task: `tasks:{taskId}` — TTL 60s
- On any mutation: evict all keys for affected userId (creator + assignee)
- Member's cache never contains other users' tasks — key is always user-scoped

---

### Event Publishing (Redis)

```java
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String eventType, String taskId, String userId) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "eventId", UUID.randomUUID().toString(),   // schema v2 — consumer idempotency key
                "eventType", eventType,
                "taskId", taskId,
                "userId", userId,
                "timestamp", Instant.now().toString()
            ));
            redisTemplate.opsForList().leftPush("task-events", payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to publish event {}: {}", eventType, e.getMessage());
        }
    }
}
```

| Event                | Trigger                              |
|----------------------|--------------------------------------|
| `task.created`       | POST /tasks success                  |
| `task.assigned`      | PATCH /tasks/:id/assign success      |
| `task.status_changed`| PATCH /tasks/:id/status success      |
| `task.overdue`       | Background job detects overdue task  |

---

### Background Job — Overdue Detection

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class OverdueJobServiceImpl implements OverdueJobService {

    private final TaskRepository taskRepository;
    private final EventPublisher eventPublisher;

    @Scheduled(cron = "${OVERDUE_JOB_CRON:0 */5 * * * *}")
    @Override
    public void detectAndPublishOverdue() {
        List<Task> overdue = taskRepository.findOverdueTasks(LocalDate.now());
        overdue.forEach(task -> {
            eventPublisher.publish("task.overdue", task.getId(), task.getCreatedBy());
            log.info("Overdue task published: {}", task.getId());
        });
    }
}
```
- Does NOT modify task records — read-only scan + publish
- Cron controlled via `OVERDUE_JOB_CRON` env var
- Query: `WHERE due_date < NOW() AND status != 'done'`

---

### Helper Rules
- If a block appears more than once — extract to private method
- If a method exceeds 20 lines — split it
- Name helpers after what they do: `requireLead`, `requireOwnership`, `buildTask`, `findTaskOrThrow`
- Helpers are `private` in ServiceImpl — never expose through the interface

---

### MapStruct — Entity ↔ DTO
```java
@Mapper(componentModel = "spring")
public interface TaskMapper {
    TaskResponse toResponse(Task task);
    CommentResponse toResponse(Comment comment);
}
```
- One mapper per entity, lives in `mapper/`
- Never map manually — always use MapStruct
- Never call mapper from Controller — only from ServiceImpl

---

### Exception Handling

```java
// Custom exceptions
public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String taskId) {
        super("task not found");
    }
}

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

// Global handler
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TaskNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(UnauthorizedException ex) {
        return ResponseEntity.status(403).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .findFirst()
            .orElse("validation error");
        return ResponseEntity.status(400).body(new ErrorResponse(message));
    }
}
```
- No try/catch in Controller or ServiceImpl — throw, let handler catch
- All errors return `{ "message": "string" }` — no stack traces
- 401 = missing X-User-Id or X-User-Role header
- 403 = role/ownership violation
- 404 = task not found

---

### Validation — on Request DTOs only

Error messages must match IRD-002 exactly:

| Field       | Rule                        | Message                                      |
|-------------|-----------------------------|----------------------------------------------|
| title       | @NotBlank                   | `"title is required"`                        |
| title       | @Size(max=200)              | `"title must be at most 200 characters"`     |
| text        | @NotBlank                   | `"text is required"`                         |
| text        | @Size(max=1000)             | `"text must be at most 1000 characters"`     |
| status      | enum validation             | `"status must be one of: todo, in_progress, done"` |
| assignee_id | @NotBlank                   | `"assignee_id is required"`                  |

---

### Naming Conventions

| Type | Pattern | Example |
|---|---|---|
| Class | PascalCase | `TaskServiceImpl` |
| Method | camelCase, verb-first | `findTaskOrThrow`, `requireLead` |
| Variable | camelCase | `savedTask`, `overdueList` |
| Constant | UPPER_SNAKE_CASE | `TASK_EVENTS_QUEUE` |
| DTO request | Action + Request | `CreateTaskRequest`, `AssignTaskRequest` |
| DTO response | Entity + Response | `TaskResponse`, `CommentResponse` |
| Exception | Noun + Exception | `TaskNotFoundException`, `UnauthorizedException` |
| Mapper | Entity + Mapper | `TaskMapper` |

---

### Logging
```java
@Slf4j
public class TaskServiceImpl implements TaskService {
    log.info("Task created: {} by {}", taskId, userId);
    log.warn("Unauthorized assign attempt by user: {}", userId);
    log.error("Failed to publish event: {}", ex.getMessage());
}
```
- Every inbound request logged via `LoggingInterceptor`: `[timestamp] METHOD /path → STATUS (Xms)`
- Never log task content or user IDs at error level — info/warn only for business events
- Never use `System.out.println`

---

### Testing
- Integration tests only — real test database, no mocks
- Use `@SpringBootTest` + real test MySQL container
- One test class per controller: `TaskControllerTest`, `CommentControllerTest`
- Method name: `methodName_condition_expectedResult`

```java
@Test
void createTask_missingTitle_returns400() { ... }

@Test
void assignTask_asMember_returns403() { ... }

@Test
void listTasks_asMember_returnsOwnTasksOnly() { ... }

@Test
void deleteTask_asLead_returns204() { ... }
```

Unit tests for cache behavior:
```java
@Test
void listTasks_cacheHit_doesNotQueryDb() { ... }

@Test
void createTask_evictsCache() { ... }
```

---

### Database Rules (MySQL 8, database: `tasks_db`)

```sql
id    CHAR(36) PRIMARY KEY DEFAULT (UUID())
dates DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
```
- Schema managed via Flyway: `db/migrations/V{n}__description.sql`
- `spring.jpa.hibernate.ddl-auto=validate`
- Never use `ddl-auto=create` or `update`
- Overdue filter applied in application layer (MySQL does not support partial indexes)

---

### Environment Variables

```
PORT=8082
SPRING_DATASOURCE_URL=jdbc:mysql://db-task:3306/tasks_db
SPRING_DATASOURCE_USERNAME=app_user
SPRING_DATASOURCE_PASSWORD=app_pass
OVERDUE_JOB_CRON=0 */5 * * * *
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379
TASK_CACHE_TTL_SECONDS=30
```
