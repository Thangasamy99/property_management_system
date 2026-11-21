package com.pcc.workflow.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record DefinitionRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9_]{2,40}$") String code,
        @NotBlank String name,
        String description,
        @NotBlank @Pattern(regexp = "PROPERTY|LAND|FINANCE|DOCUMENT") String module,
        @NotBlank String workflowType,
        @NotBlank String entityType,
        @Pattern(regexp = "POST|PATCH|PUT") String integrationMethod,
        String integrationPath,
        Integer escalationDays,
        Boolean active,
        @NotEmpty List<@Valid LevelRequest> levels) {}
