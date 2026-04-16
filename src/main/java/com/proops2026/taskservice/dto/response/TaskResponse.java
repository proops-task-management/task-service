package com.proops2026.taskservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@Jacksonized
public class TaskResponse {

    private String id;
    private String title;
    private String description;
    private String status;
    private String assigneeId;
    private String createdBy;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

