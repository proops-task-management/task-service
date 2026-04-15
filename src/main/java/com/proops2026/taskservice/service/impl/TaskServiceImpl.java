package com.proops2026.taskservice.service.impl;

import com.proops2026.taskservice.dto.request.AssignTaskRequest;
import com.proops2026.taskservice.dto.request.CreateTaskRequest;
import com.proops2026.taskservice.dto.request.UpdateMetadataRequest;
import com.proops2026.taskservice.dto.request.UpdateStatusRequest;
import com.proops2026.taskservice.dto.response.CommentResponse;
import com.proops2026.taskservice.dto.response.TaskDetailResponse;
import com.proops2026.taskservice.dto.response.TaskListResponse;
import com.proops2026.taskservice.dto.response.TaskResponse;
import com.proops2026.taskservice.exception.BadRequestException;
import com.proops2026.taskservice.exception.TaskNotFoundException;
import com.proops2026.taskservice.exception.UnauthorizedException;
import com.proops2026.taskservice.mapper.CommentMapper;
import com.proops2026.taskservice.mapper.TaskMapper;
import com.proops2026.taskservice.model.Task;
import com.proops2026.taskservice.repository.CommentRepository;
import com.proops2026.taskservice.repository.TaskRepository;
import com.proops2026.taskservice.service.TaskFilters;
import com.proops2026.taskservice.service.TaskService;
import com.proops2026.taskservice.util.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("todo", "in_progress", "done");

    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final TaskMapper taskMapper;
    private final CommentMapper commentMapper;
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

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "tasks", key = "#userId + ':' + #role + ':' + #filters.cacheKey()")
    public TaskListResponse listTasks(String userId, String role, TaskFilters filters) {
        validateStatusIfPresent(filters.getStatus());
        List<TaskResponse> tasks = taskRepository.findTasks(userId, role, filters).stream()
                .map(taskMapper::toResponse)
                .toList();
        return TaskListResponse.builder()
                .tasks(tasks)
                .total(tasks.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "task", key = "#taskId")
    public TaskDetailResponse getTask(String taskId) {
        Task task = findTaskOrThrow(taskId);
        List<CommentResponse> comments = commentRepository.findByTask_IdOrderByCreatedAtAsc(taskId).stream()
                .map(commentMapper::toResponse)
                .toList();
        return TaskDetailResponse.builder()
                .task(taskMapper.toResponse(task))
                .comments(comments)
                .build();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", allEntries = true),
            @CacheEvict(value = "task", key = "#taskId")
    })
    public TaskResponse assignTask(String userId, String role, String taskId, AssignTaskRequest request) {
        requireLead(role, "only team leads can assign tasks");
        Task task = findTaskOrThrow(taskId);
        task.setAssigneeId(request.getAssigneeId());
        Task saved = taskRepository.save(task);
        eventPublisher.publish("task.assigned", saved.getId(), request.getAssigneeId());
        log.info("Task assigned: {} to {}", saved.getId(), request.getAssigneeId());
        return taskMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", allEntries = true),
            @CacheEvict(value = "task", key = "#taskId")
    })
    public TaskResponse updateStatus(String userId, String role, String taskId, UpdateStatusRequest request) {
        Task task = findTaskOrThrow(taskId);
        requireAssigneeOrCreator(task, userId);
        String status = validateStatus(request.getStatus());
        task.setStatus(status);
        Task saved = taskRepository.save(task);
        eventPublisher.publish("task.status_changed", saved.getId(), saved.getCreatedBy());
        log.info("Task status updated: {} -> {}", saved.getId(), status);
        return taskMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", allEntries = true),
            @CacheEvict(value = "task", key = "#taskId")
    })
    public TaskResponse updateMetadata(String userId, String role, String taskId, UpdateMetadataRequest request) {
        requireLead(role, "only team leads can edit task metadata");
        Task task = findTaskOrThrow(taskId);
        applyMetadataUpdates(task, request);
        Task saved = taskRepository.save(task);
        log.info("Task metadata updated: {}", saved.getId());
        return taskMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", allEntries = true),
            @CacheEvict(value = "task", key = "#taskId")
    })
    public void deleteTask(String userId, String role, String taskId) {
        requireLead(role, "only team leads can delete tasks");
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException(taskId);
        }
        taskRepository.deleteById(taskId);
        log.info("Task deleted: {}", taskId);
    }

    private Task buildTask(CreateTaskRequest request, String userId) {
        return Task.builder()
                .id(UUID.randomUUID().toString())
                .title(request.getTitle())
                .description(request.getDescription())
                .status("todo")
                .createdBy(userId)
                .dueDate(request.getDueDate())
                .build();
    }

    private Task findTaskOrThrow(String taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private void requireLead(String role, String message) {
        if (!"lead".equals(role)) {
            throw new UnauthorizedException(message);
        }
    }

    private void requireAssigneeOrCreator(Task task, String userId) {
        boolean isAllowed = userId.equals(task.getAssigneeId()) || userId.equals(task.getCreatedBy());
        if (!isAllowed) {
            throw new UnauthorizedException("you do not have permission to update this task");
        }
    }

    private void applyMetadataUpdates(Task task, UpdateMetadataRequest request) {
        if (request.getTitle() == null && request.getDescription() == null && request.getDueDate() == null) {
            throw new BadRequestException("at least one field is required");
        }
        if (request.getTitle() != null) {
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }
    }

    private void validateStatusIfPresent(String status) {
        if (status == null) {
            return;
        }
        validateStatus(status);
    }

    private String validateStatus(String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            throw new BadRequestException("status must be one of: todo, in_progress, done");
        }
        return status;
    }
}

