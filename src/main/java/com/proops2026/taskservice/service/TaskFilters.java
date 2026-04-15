package com.proops2026.taskservice.service;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskFilters {

    private String assigneeId;
    private String status;
    private boolean overdue;

    public static TaskFilters fromQuery(String userId, String assignee, String status, Boolean overdue) {
        return TaskFilters.builder()
                .assigneeId(resolveAssigneeId(userId, assignee))
                .status(normalize(status))
                .overdue(Boolean.TRUE.equals(overdue))
                .build();
    }

    public String cacheKey() {
        return "assignee=" + nullSafe(assigneeId)
                + "|status=" + nullSafe(status)
                + "|overdue=" + overdue;
    }

    private static String resolveAssigneeId(String userId, String assignee) {
        if (assignee == null || assignee.isBlank()) {
            return null;
        }
        if ("me".equalsIgnoreCase(assignee)) {
            return userId;
        }
        return assignee;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String nullSafe(String value) {
        return value == null ? "null" : value;
    }
}

