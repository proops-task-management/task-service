package com.proops2026.taskservice.service.impl;

import com.proops2026.taskservice.dto.request.AddCommentRequest;
import com.proops2026.taskservice.dto.response.CommentResponse;
import com.proops2026.taskservice.exception.TaskNotFoundException;
import com.proops2026.taskservice.mapper.CommentMapper;
import com.proops2026.taskservice.model.Comment;
import com.proops2026.taskservice.model.Task;
import com.proops2026.taskservice.repository.CommentRepository;
import com.proops2026.taskservice.repository.TaskRepository;
import com.proops2026.taskservice.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    @CacheEvict(value = "task", key = "#taskId")
    public CommentResponse addComment(String userId, String role, String taskId, AddCommentRequest request) {
        Task task = findTaskOrThrow(taskId);
        Comment saved = commentRepository.save(buildComment(task, userId, request));
        log.info("Comment created: {} on task {}", saved.getId(), taskId);
        return commentMapper.toResponse(saved);
    }

    private Task findTaskOrThrow(String taskId) {
        return taskRepository.findById(taskId).orElseThrow(() -> new TaskNotFoundException(taskId));
    }

    private Comment buildComment(Task task, String userId, AddCommentRequest request) {
        return Comment.builder()
                .id(UUID.randomUUID().toString())
                .task(task)
                .authorId(userId)
                .text(request.getText())
                .build();
    }
}

