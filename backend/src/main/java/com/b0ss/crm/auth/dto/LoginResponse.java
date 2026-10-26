package com.b0ss.crm.auth.dto;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt, UserResponse user) {
}
