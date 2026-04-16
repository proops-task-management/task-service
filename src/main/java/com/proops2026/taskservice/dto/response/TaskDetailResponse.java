package com.proops2026.taskservice.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Builder
@Jacksonized
public class TaskDetailResponse {

    private TaskResponse task;
    private List<CommentResponse> comments;
}

