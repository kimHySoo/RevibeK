package com.ssafy.revibek.common.dto;

public record ApiResponseDto<T>(
    boolean success,
    String message,
    T data
) {
    public static <T> ApiResponseDto<T> success(String message, T data) {
        return new ApiResponseDto<>(true, message, data);
    }

    public static <T> ApiResponseDto<T> failure(String message, T data) {
        return new ApiResponseDto<>(false, message, data);
    }
}
