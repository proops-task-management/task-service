package com.proops2026.taskservice.exception;

public class TaskNotFoundException extends RuntimeException {

    public TaskNotFoundException(String taskId) {
        super("task not found");
    }
}

