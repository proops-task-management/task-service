package com.proops2026.taskservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TaskListResponse {

    private List<TaskResponse> tasks;
    private long total;
}

