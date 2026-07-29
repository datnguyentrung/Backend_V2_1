package com.dat.ai_receptionist_web.util.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
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

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ProblemDetail> handleAppException(AppException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getErrorCode().getStatusCode());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, ex.getErrorCode().getMessage());
        problemDetail.setTitle(ex.getErrorCode().name());
        return buildResponse(problemDetail, status);
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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                com.dat.ai_receptionist_web.enums.ErrorCode.FILE_TOO_LARGE.getMessage()
        );
        problemDetail.setTitle(com.dat.ai_receptionist_web.enums.ErrorCode.FILE_TOO_LARGE.name());
        return buildResponse(problemDetail, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    // Xử lý lỗi NoSuchElementException ở đây, hoặc gom chung vào Generic
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        log.error("Lỗi gốc hệ thống: ", ex);

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

    // Spring Framework 7's default JSON converter only advertises application/json.
    // Pinning application/problem+json caused exception rendering itself to fail.
    private ResponseEntity<ProblemDetail> buildResponse(ProblemDetail problemDetail, HttpStatus status) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(problemDetail);
    }
}
