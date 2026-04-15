package com.proops2026.taskservice.repository;

import com.proops2026.taskservice.model.Task;
import com.proops2026.taskservice.service.TaskFilters;

import java.util.List;

public interface TaskRepositoryCustom {

    List<Task> findTasks(String userId, String role, TaskFilters filters);
}

