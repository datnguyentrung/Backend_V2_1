package com.dat.ai_receptionist_web.error.code;

import com.dat.ai_receptionist_web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum CoreErrorCode implements ErrorCode {
    BRANCH_NOT_FOUND(
            "BRANCH_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            "Branch not found",
            "Branch not found"),
    PERSON_NOT_FOUND("PERSON_NOT_FOUND", HttpStatus.NOT_FOUND, "Person not found", "Person not found"),
    USER_PERSON_NOT_FOUND("USER_PERSON_NOT_FOUND", HttpStatus.NOT_FOUND, "User person not found",
            "User person not found"),
    NATIONAL_CODE_ALREADY_EXISTS("NATIONAL_CODE_ALREADY_EXISTS", HttpStatus.CONFLICT,
            "National code already exists", "National code already exists"),
    PERSON_CODE_ALREADY_EXISTS("PERSON_CODE_ALREADY_EXISTS", HttpStatus.CONFLICT,
            "Person code already exists", "Person code already exists");

    private final String code;
    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String title() { return title; }
    @Override public String defaultDetail() { return defaultDetail; }
}
