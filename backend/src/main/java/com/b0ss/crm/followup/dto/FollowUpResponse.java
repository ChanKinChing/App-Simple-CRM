package com.b0ss.crm.followup.dto;

import java.time.Instant;

public record FollowUpResponse(
        Long id,
        Long customerId,
        String content,
        String createdBy,
        Instant createdAt) {
}
