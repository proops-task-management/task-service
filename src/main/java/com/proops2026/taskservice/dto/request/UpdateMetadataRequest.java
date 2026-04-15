package com.proops2026.taskservice.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateMetadataRequest {

    private String title;

    private String description;

    private LocalDate dueDate;
}

