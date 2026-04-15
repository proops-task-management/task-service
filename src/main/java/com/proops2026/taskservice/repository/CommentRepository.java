package com.proops2026.taskservice.repository;

import com.proops2026.taskservice.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {

    List<Comment> findByTask_IdOrderByCreatedAtAsc(String taskId);
}

