package com.proops2026.taskservice.controller;

import com.proops2026.taskservice.dto.request.AssignTaskRequest;
import com.proops2026.taskservice.dto.request.CreateTaskRequest;
import com.proops2026.taskservice.dto.request.UpdateMetadataRequest;
import com.proops2026.taskservice.dto.request.UpdateStatusRequest;
import com.proops2026.taskservice.dto.response.TaskDetailResponse;
import com.proops2026.taskservice.dto.response.TaskListResponse;
import com.proops2026.taskservice.dto.response.TaskResponse;
import com.proops2026.taskservice.service.TaskFilters;
import com.proops2026.taskservice.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping
    public ResponseEntity<TaskListResponse> list(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(name = "assignee", required = false) String assignee,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "overdue", required = false) Boolean overdue) {
        TaskFilters filters = TaskFilters.fromQuery(userId, assignee, status, overdue);
        return ResponseEntity.ok(taskService.listTasks(userId, role, filters));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDetailResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(taskService.getTask(id));
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<TaskResponse> assign(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String id,
            @Valid @RequestBody AssignTaskRequest request) {
        return ResponseEntity.ok(taskService.assignTask(userId, role, id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateStatus(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String id,
            @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(taskService.updateStatus(userId, role, id, request));
    }

    @PatchMapping("/{id}/metadata")
    public ResponseEntity<TaskResponse> updateMetadata(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String id,
            @RequestBody UpdateMetadataRequest request) {
        return ResponseEntity.ok(taskService.updateMetadata(userId, role, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String id) {
        taskService.deleteTask(userId, role, id);
        return ResponseEntity.noContent().build();
    }
}

