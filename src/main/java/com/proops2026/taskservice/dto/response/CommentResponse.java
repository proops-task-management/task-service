package com.proops2026.taskservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponse {

    private String id;
    private String taskId;
    private String authorId;
    private String text;
    private LocalDateTime createdAt;
}

