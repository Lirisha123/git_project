package com.insight.dashboard.controller;

import com.insight.dashboard.dto.ApiError;
import com.insight.dashboard.exception.AppException;
import com.insight.dashboard.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiError(exception.getMessage(), HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiError> handleAppException(AppException exception) {
        return ResponseEntity.badRequest()
            .body(new ApiError(exception.getMessage(), HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiError(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
}
