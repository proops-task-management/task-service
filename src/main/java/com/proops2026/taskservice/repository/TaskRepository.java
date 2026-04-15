package com.proops2026.taskservice.repository;

import com.proops2026.taskservice.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, String>, TaskRepositoryCustom {

    @Query("select t from Task t where t.dueDate is not null and t.dueDate < :today and t.status <> 'done'")
    List<Task> findOverdueTasks(@Param("today") LocalDate today);
}

