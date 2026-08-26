package com.dat.ai_receptionist_web.error.code;

import com.dat.ai_receptionist_web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum GeneralErrorCode implements ErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Validation error", "Request validation failed"),
    INVALID_REQUEST_PARAMETER("INVALID_REQUEST_PARAMETER", HttpStatus.BAD_REQUEST, "Invalid request parameter",
            "Request parameter is invalid"),
    INVALID_REQUEST_BODY("INVALID_REQUEST_BODY", HttpStatus.BAD_REQUEST, "Invalid request body",
            "Request body contains an invalid value"),
    DATA_INTEGRITY_CONFLICT("DATA_INTEGRITY_CONFLICT", HttpStatus.CONFLICT, "Data integrity conflict",
            "The request conflicts with existing data"),
    MEDIA_TYPE_NOT_ACCEPTABLE("MEDIA_TYPE_NOT_ACCEPTABLE", HttpStatus.NOT_ACCEPTABLE,
            "Media type not acceptable", "Requested media type is not acceptable"),
    FILE_TOO_LARGE("FILE_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE, "File too large",
            "File exceeds the allowed size"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
            "Internal server error");

    private final String code;
    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    @Override
    public String code() {
        return code;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String title() {
        return title;
    }

    @Override
    public String defaultDetail() {
        return defaultDetail;
    }
}
