package com.b0ss.crm.customer.dto;

import java.time.Instant;

public record CustomerResponse(
        Long id,
        String name,
        String phone,
        String email,
        String company,
        String note,
        String createdBy,
        Instant createdAt,
        Instant updatedAt) {
}
