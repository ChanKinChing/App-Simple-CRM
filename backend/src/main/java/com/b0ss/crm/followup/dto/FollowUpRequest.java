package com.b0ss.crm.followup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FollowUpRequest(
        @NotBlank(message = "content is required")
        @Size(max = 2000, message = "content must be at most 2000 characters")
        String content) {
}
