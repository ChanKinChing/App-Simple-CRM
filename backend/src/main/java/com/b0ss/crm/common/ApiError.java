package com.b0ss.crm.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, null);
    }

    public static ApiError withFields(int status, String code, String message, String path,
                                      Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, code, message, path, fieldErrors);
    }
}
