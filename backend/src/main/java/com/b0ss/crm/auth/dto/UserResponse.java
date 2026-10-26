package com.b0ss.crm.auth.dto;

import com.b0ss.crm.user.User;

public record UserResponse(Long id, String username, String displayName, String role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole().name());
    }
}
