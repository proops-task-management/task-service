package com.proops2026.taskservice.mapper;

import com.proops2026.taskservice.dto.response.CommentResponse;
import com.proops2026.taskservice.model.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "taskId", source = "task.id")
    CommentResponse toResponse(Comment comment);
}

