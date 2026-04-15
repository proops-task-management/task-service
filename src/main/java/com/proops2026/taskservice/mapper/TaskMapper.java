package com.proops2026.taskservice.mapper;

import com.proops2026.taskservice.dto.response.TaskResponse;
import com.proops2026.taskservice.model.Task;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    TaskResponse toResponse(Task task);
}

