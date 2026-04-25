package com.family.schedule.web.dto;

import java.util.Map;

public record ErrorResponse(String error, String message, Map<String, String> fields) {
    public static ErrorResponse validation(String message, Map<String, String> fields) {
        return new ErrorResponse("VALIDATION", message, fields);
    }
    public static ErrorResponse notFound(String message) {
        return new ErrorResponse("NOT_FOUND", message, null);
    }
    public static ErrorResponse internal(String message) {
        return new ErrorResponse("INTERNAL", message, null);
    }
}
