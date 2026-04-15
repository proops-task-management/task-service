package com.proops2026.taskservice.service;

import com.proops2026.taskservice.dto.request.AssignTaskRequest;
import com.proops2026.taskservice.dto.request.CreateTaskRequest;
import com.proops2026.taskservice.dto.request.UpdateMetadataRequest;
import com.proops2026.taskservice.dto.request.UpdateStatusRequest;
import com.proops2026.taskservice.dto.response.TaskDetailResponse;
import com.proops2026.taskservice.dto.response.TaskListResponse;
import com.proops2026.taskservice.dto.response.TaskResponse;

public interface TaskService {

    TaskResponse createTask(String userId, String role, CreateTaskRequest request);

    TaskListResponse listTasks(String userId, String role, TaskFilters filters);

    TaskDetailResponse getTask(String taskId);

    TaskResponse assignTask(String userId, String role, String taskId, AssignTaskRequest request);

    TaskResponse updateStatus(String userId, String role, String taskId, UpdateStatusRequest request);

    TaskResponse updateMetadata(String userId, String role, String taskId, UpdateMetadataRequest request);

    void deleteTask(String userId, String role, String taskId);
}

