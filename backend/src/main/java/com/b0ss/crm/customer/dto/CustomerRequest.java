package com.b0ss.crm.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,

        @Size(max = 30, message = "phone must be at most 30 characters")
        String phone,

        @Email(message = "email must be a valid address")
        @Size(max = 120, message = "email must be at most 120 characters")
        String email,

        @Size(max = 120, message = "company must be at most 120 characters")
        String company,

        @Size(max = 1000, message = "note must be at most 1000 characters")
        String note) {
}
