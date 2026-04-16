package com.proops2026.taskservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;

@Getter
@Builder
@Jacksonized
public class CommentResponse {

    private String id;
    private String taskId;
    private String authorId;
    private String text;
    private LocalDateTime createdAt;
}

