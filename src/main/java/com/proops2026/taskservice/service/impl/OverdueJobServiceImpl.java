package com.proops2026.taskservice.service.impl;

import com.proops2026.taskservice.model.Task;
import com.proops2026.taskservice.repository.TaskRepository;
import com.proops2026.taskservice.service.OverdueJobService;
import com.proops2026.taskservice.util.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OverdueJobServiceImpl implements OverdueJobService {

    private final TaskRepository taskRepository;
    private final EventPublisher eventPublisher;

    @Scheduled(cron = "${overdue.job.cron}")
    @Override
    @Transactional(readOnly = true)
    public void detectAndPublishOverdue() {
        List<Task> overdue = taskRepository.findOverdueTasks(LocalDate.now());
        overdue.forEach(this::publishOverdueIfAssigned);
    }

    private void publishOverdueIfAssigned(Task task) {
        if (task.getAssigneeId() == null || task.getAssigneeId().isBlank()) {
            return;
        }
        eventPublisher.publish("task.overdue", task.getId(), task.getAssigneeId());
        log.info("Overdue task published: {}", task.getId());
    }
}

