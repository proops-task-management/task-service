package com.proops2026.taskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proops2026.taskservice.config.SecurityConfig;
import com.proops2026.taskservice.dto.request.AssignTaskRequest;
import com.proops2026.taskservice.dto.request.CreateTaskRequest;
import com.proops2026.taskservice.dto.request.UpdateStatusRequest;
import com.proops2026.taskservice.dto.response.TaskDetailResponse;
import com.proops2026.taskservice.dto.response.TaskListResponse;
import com.proops2026.taskservice.dto.response.TaskResponse;
import com.proops2026.taskservice.exception.BadRequestException;
import com.proops2026.taskservice.exception.UnauthorizedException;
import com.proops2026.taskservice.service.TaskFilters;
import com.proops2026.taskservice.service.TaskService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @Test
    void createTask_missingHeaders_returns401() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "t"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("unauthorized"));
    }

    @Test
    void createTask_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/tasks")
                        .header("X-User-Id", "u1")
                        .header("X-User-Role", "member")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title is required"));
    }

    @Test
    void createTask_valid_returns201() throws Exception {
        TaskResponse response = TaskResponse.builder()
                .id("t1")
                .title("title")
                .status("todo")
                .assigneeId(null)
                .createdBy("u1")
                .createdAt(LocalDateTime.parse("2026-04-15T10:00:00"))
                .updatedAt(LocalDateTime.parse("2026-04-15T10:00:00"))
                .build();

        when(taskService.createTask(eq("u1"), eq("member"), any(CreateTaskRequest.class))).thenReturn(response);

        mockMvc.perform(post("/tasks")
                        .header("X-User-Id", "u1")
                        .header("X-User-Role", "member")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "title"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("t1"))
                .andExpect(jsonPath("$.title").value("title"))
                .andExpect(jsonPath("$.status").value("todo"))
                .andExpect(jsonPath("$.assignee_id").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.created_by").value("u1"));
    }

    @Test
    void listTasks_returns200() throws Exception {
        TaskListResponse response = TaskListResponse.builder()
                .tasks(List.of(TaskResponse.builder().id("t1").title("t").status("todo").build()))
                .total(1)
                .build();

        when(taskService.listTasks(eq("u1"), eq("member"), any(TaskFilters.class))).thenReturn(response);

        mockMvc.perform(get("/tasks?assignee=me&overdue=true")
                        .header("X-User-Id", "u1")
                        .header("X-User-Role", "member"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.tasks[0].id").value("t1"));
    }

    @Test
    void assignTask_asMember_returns403() throws Exception {
        when(taskService.assignTask(eq("u1"), eq("member"), eq("t1"), any(AssignTaskRequest.class)))
                .thenThrow(new UnauthorizedException("only team leads can assign tasks"));

        mockMvc.perform(patch("/tasks/t1/assign")
                        .header("X-User-Id", "u1")
                        .header("X-User-Role", "member")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("assignee_id", "u2"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("only team leads can assign tasks"));
    }

    @Test
    void updateStatus_invalid_returns400WithExactMessage() throws Exception {
        when(taskService.updateStatus(eq("u1"), eq("member"), eq("t1"), any(UpdateStatusRequest.class)))
                .thenThrow(new BadRequestException("status must be one of: todo, in_progress, done"));

        mockMvc.perform(patch("/tasks/t1/status")
                        .header("X-User-Id", "u1")
                        .header("X-User-Role", "member")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "invalid"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status must be one of: todo, in_progress, done"));
    }

    @Test
    void getTask_returns200() throws Exception {
        TaskDetailResponse response = TaskDetailResponse.builder()
                .task(TaskResponse.builder().id("t1").title("t").status("todo").build())
                .comments(List.of())
                .build();

        when(taskService.getTask(eq("t1"))).thenReturn(response);

        mockMvc.perform(get("/tasks/t1")
                        .header("X-User-Id", "u1")
                        .header("X-User-Role", "member"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.task.id").value("t1"));
    }

    @Test
    void deleteTask_asMember_returns403() throws Exception {
        doThrow(new UnauthorizedException("only team leads can delete tasks"))
                .when(taskService).deleteTask(eq("u1"), eq("member"), eq("t1"));

        mockMvc.perform(delete("/tasks/t1")
                        .header("X-User-Id", "u1")
                        .header("X-User-Role", "member"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("only team leads can delete tasks"));
    }
}

