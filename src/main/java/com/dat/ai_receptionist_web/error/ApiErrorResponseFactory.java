package com.dat.ai_receptionist_web.error;

import com.dat.ai_receptionist_web.error.code.GeneralErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApiErrorResponseFactory {
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private final ObjectMapper objectMapper;

    public ResponseEntity<ProblemDetail> response(ApiException exception, HttpServletRequest request) {
        return response(exception.getErrorCode(), exception.responseDetail(), request);
    }

    public ResponseEntity<ProblemDetail> response(ErrorCode errorCode, HttpServletRequest request) {
        return response(errorCode, errorCode.defaultDetail(), request);
    }

    public ResponseEntity<ProblemDetail> response(ErrorCode errorCode, String detail, HttpServletRequest request) {
        return buildResponse(problem(errorCode, detail, request));
    }

    public ResponseEntity<ProblemDetail> validationResponse(List<ValidationError> errors, HttpServletRequest request) {
        ProblemDetail problemDetail = problem(GeneralErrorCode.VALIDATION_ERROR,
                GeneralErrorCode.VALIDATION_ERROR.defaultDetail(), request);
        problemDetail.setProperty("errors", errors);
        return buildResponse(problemDetail);
    }

    public ResponseEntity<ProblemDetail> unexpectedResponse(HttpServletRequest request) {
        return response(GeneralErrorCode.INTERNAL_SERVER_ERROR, request);
    }

    public void write(HttpServletResponse response, ErrorCode errorCode, HttpServletRequest request) throws IOException {
        write(response, problem(errorCode, errorCode.defaultDetail(), request));
    }

    public void write(HttpServletResponse response, ProblemDetail problemDetail) throws IOException {
        response.setStatus(problemDetail.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body(problemDetail));
    }

    public ProblemDetail problem(ErrorCode errorCode, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(errorCode.status(), detail);
        problemDetail.setType(URI.create(errorCode.type()));
        problemDetail.setTitle(errorCode.title());
        problemDetail.setProperty("code", errorCode.code());
        problemDetail.setProperty("correlationId", correlationId());
        if (request != null) {
            problemDetail.setInstance(URI.create(request.getRequestURI()));
        }
        return problemDetail;
    }

    public ErrorCode legacy(String code, HttpStatus status, String title, String defaultDetail) {
        return new LegacyErrorCode(code, status, title, defaultDetail);
    }

    private ResponseEntity<ProblemDetail> buildResponse(ProblemDetail problemDetail) {
        return ResponseEntity.status(problemDetail.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(problemDetail);
    }

    private Map<String, Object> body(ProblemDetail problemDetail) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", problemDetail.getType().toString());
        values.put("title", problemDetail.getTitle());
        values.put("status", problemDetail.getStatus());
        values.put("detail", problemDetail.getDetail());
        if (problemDetail.getInstance() != null) {
            values.put("instance", problemDetail.getInstance().toString());
        }
        if (problemDetail.getProperties() != null) {
            values.putAll(problemDetail.getProperties());
        }
        return values;
    }

    private String correlationId() {
        return MDC.get(CORRELATION_ID_MDC_KEY);
    }

    private record LegacyErrorCode(String code, HttpStatus status, String title, String defaultDetail)
            implements ErrorCode {
    }
}
