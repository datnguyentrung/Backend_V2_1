package com.dat.backend_v2_1.util.error;

import com.dat.backend_v2_0.dto.RestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalException {
    // Nhóm exception liên quan đến authentication, credential
    @ExceptionHandler({
            UsernameNotFoundException.class,
            BadCredentialsException.class,
            IdInvalidException.class,
            InvalidPasswordException.class
    })
    public ResponseEntity<RestResponse<Object>> handleAuthException(Exception ex) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.BAD_REQUEST.value());
        res.setError(ex.getClass().getSimpleName()); // Ví dụ: InvalidPasswordException
        res.setMessage(ex.getMessage());             // Ví dụ: "Mật khẩu cũ không đúng"
        return ResponseEntity.badRequest().body(res);
    }

    // Validation error cho DTO (có @Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Object>> validationError(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        List<FieldError> fieldErrors = result.getFieldErrors();

        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.BAD_REQUEST.value());
        res.setError("ValidationError");

        List<String> errors = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        res.setMessage(errors.size() > 1 ? errors : errors.get(0));

        return ResponseEntity.badRequest().body(res);
    }

    // Fallback handler cho các lỗi chưa định nghĩa
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResponse<Object>> handleGeneric(Exception ex) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        res.setError("InternalServerError");
        res.setMessage(ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<RestResponse<Object>> handleUserNotFound(UserNotFoundException ex) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(HttpStatus.NOT_FOUND.value());
        res.setError(ex.getClass().getSimpleName());
        res.setMessage(ex.getMessage());
        res.setData(null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(res);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RestResponse<Object>> handleResponseStatus(ResponseStatusException ex) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatusCode(ex.getStatusCode().value());
        res.setError("ResponseStatusException");
        res.setMessage(ex.getReason());
        res.setData(null);

        return ResponseEntity.status(ex.getStatusCode()).body(res);
    }
}
