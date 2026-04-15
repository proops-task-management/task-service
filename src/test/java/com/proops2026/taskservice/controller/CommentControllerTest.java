package com.proops2026.taskservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proops2026.taskservice.config.SecurityConfig;
import com.proops2026.taskservice.dto.request.AddCommentRequest;
import com.proops2026.taskservice.dto.response.CommentResponse;
import com.proops2026.taskservice.exception.TaskNotFoundException;
import com.proops2026.taskservice.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @Test
    void addComment_missingHeaders_returns401() throws Exception {
        mockMvc.perform(post("/tasks/123/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("text", "hi"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("unauthorized"));
    }

    @Test
    void addComment_missingText_returns400() throws Exception {
        mockMvc.perform(post("/tasks/123/comments")
                        .header("X-User-Id", "u1")
                        .header("X-User-Role", "member")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("text is required"));
    }

    @Test
    void addComment_taskNotFound_returns404() throws Exception {
        when(commentService.addComment(eq("u1"), eq("member"), eq("missing"), any(AddCommentRequest.class)))
                .thenThrow(new TaskNotFoundException("missing"));

        mockMvc.perform(post("/tasks/missing/comments")
                        .header("X-User-Id", "u1")
                        .header("X-User-Role", "member")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("text", "hi"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("task not found"));
    }

    @Test
    void addComment_valid_returns201() throws Exception {
        CommentResponse response = CommentResponse.builder()
                .id("c1")
                .taskId("t1")
                .authorId("u1")
                .text("hi")
                .createdAt(LocalDateTime.parse("2026-04-15T10:00:00"))
                .build();

        when(commentService.addComment(eq("u1"), eq("member"), eq("t1"), any(AddCommentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/tasks/t1/comments")
                        .header("X-User-Id", "u1")
                        .header("X-User-Role", "member")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("text", "hi"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("c1"))
                .andExpect(jsonPath("$.task_id").value("t1"))
                .andExpect(jsonPath("$.author_id").value("u1"))
                .andExpect(jsonPath("$.text").value("hi"))
                .andExpect(jsonPath("$.created_at").exists());
    }
}

