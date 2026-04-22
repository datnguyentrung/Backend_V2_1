package com.dat.backend_v2_1.util.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler({
            UsernameNotFoundException.class,
            BadCredentialsException.class,
            IdInvalidException.class,
            InvalidPasswordException.class
    })
    public ResponseEntity<ProblemDetail> handleAuthException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        return buildResponse(problemDetail, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validationError(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation Error");
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        problemDetail.setProperty("errors", errors.size() > 1 ? errors : errors.get(0));
        return buildResponse(problemDetail, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        return buildResponse(problemDetail, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        return buildResponse(problemDetail, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
        return buildResponse(problemDetail, HttpStatus.valueOf(ex.getStatusCode().value()));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ProblemDetail> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_ACCEPTABLE, ex.getMessage());
        return buildResponse(problemDetail, HttpStatus.NOT_ACCEPTABLE);
    }

    // Xử lý lỗi NoSuchElementException ở đây, hoặc gom chung vào Generic
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problemDetail.setTitle("Lỗi hệ thống hoặc lỗi chưa được định nghĩa");

        // Nếu lỗi là do NoSuchElementException (như trường hợp điểm danh)
        if (ex instanceof java.util.NoSuchElementException) {
            problemDetail.setStatus(HttpStatus.BAD_REQUEST.value());
            problemDetail.setTitle("Dữ liệu không hợp lệ");
            return buildResponse(problemDetail, HttpStatus.BAD_REQUEST);
        }

        return buildResponse(problemDetail, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Hàm helper để code gọn hơn, luôn ép về application/json
    private ResponseEntity<ProblemDetail> buildResponse(ProblemDetail problemDetail, HttpStatus status) {
        return ResponseEntity.status(status)
//                .contentType(MediaType.APPLICATION_JSON)
                .body(problemDetail);
    }
}