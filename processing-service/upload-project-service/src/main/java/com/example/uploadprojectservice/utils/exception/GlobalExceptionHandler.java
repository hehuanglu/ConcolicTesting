package com.example.uploadprojectservice.utils.exception;

import com.github.javaparser.ParseProblemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ParseProblemException.class)
    public ResponseEntity<String> handleParseProblemException(ParseProblemException ex) {
        // Xử lý ngoại lệ ParseProblemException và trả về thông báo lỗi cho người dùng
        String errorMessage = ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
    }

}
