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
import com.proops2026.taskservice.util.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final CommentMapper commentMapper;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    @CacheEvict(value = "task", key = "#taskId")
    public CommentResponse addComment(String userId, String role, String taskId, AddCommentRequest request) {
        Task task = findTaskOrThrow(taskId);
        Comment saved = commentRepository.saveAndFlush(buildComment(task, userId, request));
        if (saved.getCreatedAt() == null) {
            saved = commentRepository.findById(saved.getId()).orElse(saved);
        }
        publishCommentNotifications(task, userId);
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

    private void publishCommentNotifications(Task task, String authorId) {
        Set<String> recipientIds = new LinkedHashSet<>();

        if (task.getCreatedBy() != null && !task.getCreatedBy().equals(authorId)) {
            recipientIds.add(task.getCreatedBy());
        }

        if (task.getAssigneeId() != null && !task.getAssigneeId().equals(authorId)) {
            recipientIds.add(task.getAssigneeId());
        }

        recipientIds.forEach(recipientId -> eventPublisher.publish("task.commented", task.getId(), recipientId));
    }
}

