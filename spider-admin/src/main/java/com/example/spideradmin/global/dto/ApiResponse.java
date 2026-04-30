package com.example.spideradmin.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic API Response wrapper
 * Used for standardized REST API responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

    private Boolean success;
    private String message;
    private T data;
    private Integer code;

    /**
     * Success response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("성공")
                .data(data)
                .code(200)
                .build();
    }

    /**
     * Success response with custom message
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .code(200)
                .build();
    }

    /**
     * Error response with message
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .code(400)
                .build();
    }

    /**
     * Error response with custom code
     */
    public static <T> ApiResponse<T> error(String message, Integer code) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .code(code)
                .build();
    }
}
