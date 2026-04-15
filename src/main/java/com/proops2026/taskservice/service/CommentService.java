package com.proops2026.taskservice.service;

import com.proops2026.taskservice.dto.request.AddCommentRequest;
import com.proops2026.taskservice.dto.response.CommentResponse;

public interface CommentService {

    CommentResponse addComment(String userId, String role, String taskId, AddCommentRequest request);
}

