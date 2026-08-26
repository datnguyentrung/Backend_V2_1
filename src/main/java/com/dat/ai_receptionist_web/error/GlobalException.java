package com.dat.ai_receptionist_web.error;

import com.dat.ai_receptionist_web.error.code.GeneralErrorCode;
import com.dat.ai_receptionist_web.error.code.SecurityErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalException {
    private final ApiErrorResponseFactory errorResponseFactory;

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException exception, HttpServletRequest request) {
        log.warn("Expected API error {} status={} correlationId={}",
                exception.getErrorCode().code(), exception.getErrorCode().status().value(), correlationId());
        return errorResponseFactory.response(exception, request);
    }

    @ExceptionHandler(FinancialException.class)
    public ResponseEntity<ProblemDetail> handleFinancialException(
            FinancialException exception, HttpServletRequest request) {
        log.warn("Legacy financial API error {} status={} correlationId={}",
                exception.getCode(), exception.getStatus().value(), correlationId());
        ErrorCode errorCode = errorResponseFactory.legacy(exception.getCode(), exception.getStatus(),
                exception.getCode(), exception.getMessage());
        return errorResponseFactory.response(errorCode, request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityConflict(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("Database constraint rejected a request correlationId={}: {}",
                correlationId(), exception.getClass().getSimpleName());
        return errorResponseFactory.response(GeneralErrorCode.DATA_INTEGRITY_CONFLICT, request);
    }

    @ExceptionHandler({
            UsernameNotFoundException.class,
            IdInvalidException.class,
            InvalidPasswordException.class
    })
    public ResponseEntity<ProblemDetail> handleAuthException(Exception exception, HttpServletRequest request) {
        log.warn("Expected auth error correlationId={}: {}", correlationId(), exception.getClass().getSimpleName());
        return errorResponseFactory.response(SecurityErrorCode.INVALID_CREDENTIALS, request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException exception,
                                                              HttpServletRequest request) {
        log.info("Bad credentials correlationId={}", correlationId());
        return errorResponseFactory.response(SecurityErrorCode.INVALID_CREDENTIALS, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception,
                                                            HttpServletRequest request) {
        log.warn("Access denied correlationId={}", correlationId());
        return errorResponseFactory.response(SecurityErrorCode.ACCESS_DENIED, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> validationError(MethodArgumentNotValidException exception,
                                                         HttpServletRequest request) {
        List<ValidationError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toValidationError)
                .toList();
        log.warn("Validation failed correlationId={} errorCount={}", correlationId(), errors.size());
        return errorResponseFactory.validationResponse(errors, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableRequest(HttpMessageNotReadableException exception,
                                                                 HttpServletRequest request) {
        log.warn("Unreadable request body correlationId={}: {}", correlationId(), exception.getClass().getSimpleName());
        return errorResponseFactory.response(GeneralErrorCode.INVALID_REQUEST_BODY, request);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException exception,
                                                            HttpServletRequest request) {
        log.warn("Legacy user not found correlationId={}", correlationId());
        ErrorCode errorCode = errorResponseFactory.legacy("USER_NOT_FOUND", HttpStatus.NOT_FOUND,
                "User not found", "User not found");
        return errorResponseFactory.response(errorCode, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException exception,
                                                                 HttpServletRequest request) {
        log.warn("Legacy business API error correlationId={}: {}", correlationId(), exception.getMessage());
        ErrorCode errorCode = errorResponseFactory.legacy("BUSINESS_ERROR", HttpStatus.BAD_REQUEST,
                "Business error", exception.getMessage());
        return errorResponseFactory.response(errorCode, request);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ProblemDetail> handleAppException(AppException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getErrorCode().getStatusCode());
        log.warn("Legacy app API error {} status={} correlationId={}",
                exception.getErrorCode().name(), status.value(), correlationId());
        ErrorCode errorCode = errorResponseFactory.legacy(exception.getErrorCode().name(), status,
                exception.getErrorCode().name(), exception.getErrorCode().getMessage());
        return errorResponseFactory.response(errorCode, request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(ResponseStatusException exception,
                                                              HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        log.warn("Response status exception status={} correlationId={}", status.value(), correlationId());
        ErrorCode errorCode = errorResponseFactory.legacy(status.name(), status, status.getReasonPhrase(),
                status.getReasonPhrase());
        return errorResponseFactory.response(errorCode, request);
    }

    @ExceptionHandler(LeaderboardUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleLeaderboardUnavailable(LeaderboardUnavailableException exception,
                                                                      HttpServletRequest request) {
        log.error("Leaderboard read model unavailable correlationId={}", correlationId(), exception);
        ErrorCode errorCode = errorResponseFactory.legacy("LEADERBOARD_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                "Leaderboard unavailable", "Leaderboard is temporarily unavailable");
        return errorResponseFactory.response(errorCode, request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ProblemDetail> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException exception, HttpServletRequest request) {
        log.warn("Media type not acceptable correlationId={}", correlationId());
        return errorResponseFactory.response(GeneralErrorCode.MEDIA_TYPE_NOT_ACCEPTABLE, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception,
                                                                     HttpServletRequest request) {
        log.warn("Max upload size exceeded correlationId={}", correlationId());
        return errorResponseFactory.response(GeneralErrorCode.FILE_TOO_LARGE, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception exception, HttpServletRequest request) {
        log.error("Unexpected system error correlationId={}", correlationId(), exception);
        return errorResponseFactory.unexpectedResponse(request);
    }

    private ValidationError toValidationError(FieldError fieldError) {
        return new ValidationError(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private String correlationId() {
        return org.slf4j.MDC.get(ApiErrorResponseFactory.CORRELATION_ID_MDC_KEY);
    }
}
