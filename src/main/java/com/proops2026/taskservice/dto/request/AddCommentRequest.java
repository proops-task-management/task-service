package com.proops2026.taskservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCommentRequest {

    @NotBlank(message = "text is required")
    @Size(max = 1000, message = "text must be at most 1000 characters")
    private String text;
}

