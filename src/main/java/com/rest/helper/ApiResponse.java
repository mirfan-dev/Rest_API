package com.rest.helper;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;


import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private Integer statusCode;
    private HttpStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "hh:mm:ss a")
    private LocalDateTime timestamp;

    private T data;
    private List<String> errors;

    // SUCCESS without status
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(HttpStatus.OK)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }

    // SUCCESS with status + data
    public static <T> ApiResponse<T> success(HttpStatus status, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(status)
                .statusCode(status.value())
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }

    // SUCCESS with only status
    public static ApiResponse<Void> success(HttpStatus status) {
        return ApiResponse.<Void>builder()
                .success(true)
                .status(status)
                .statusCode(status.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ERROR with only status
    public static ApiResponse<Void> error(HttpStatus status) {
        return ApiResponse.<Void>builder()
                .success(false)
                .status(status)
                .statusCode(status.value())
                .timestamp(LocalDateTime.now())
                .build();
    }

    // ERROR with status + error list
    public static ApiResponse<Void> error(HttpStatus status, List<String> errors) {
        return ApiResponse.<Void>builder()
                .success(false)
                .status(status)
                .statusCode(status.value())
                .timestamp(LocalDateTime.now())
                .errors(errors)
                .build();
    }
}