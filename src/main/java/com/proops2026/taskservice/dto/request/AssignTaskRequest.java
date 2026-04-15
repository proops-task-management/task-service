package com.proops2026.taskservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignTaskRequest {

    @NotBlank(message = "assignee_id is required")
    private String assigneeId;
}

