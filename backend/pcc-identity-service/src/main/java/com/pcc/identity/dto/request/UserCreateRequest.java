package com.pcc.identity.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UserCreateRequest(
        @NotBlank(message = "Employee code is required")
        @Size(max = 32)
        String employeeCode,

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 64)
        String username,

        @NotBlank(message = "First name is required")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100)
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 191)
        String email,

        @Size(max = 32)
        String phone,

        @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
        String password,

        Set<String> roleCodes
) {
}
