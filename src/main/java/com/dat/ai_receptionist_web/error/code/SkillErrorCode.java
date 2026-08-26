package com.dat.ai_receptionist_web.error.code;

import com.dat.ai_receptionist_web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum SkillErrorCode implements ErrorCode {
    FITNESS_NOT_FOUND("FITNESS_NOT_FOUND", HttpStatus.NOT_FOUND, "Fitness not found", "Fitness not found"),
    FITNESS_RECORD_NOT_FOUND("FITNESS_RECORD_NOT_FOUND", HttpStatus.NOT_FOUND, "Fitness record not found",
            "Fitness record not found");

    private final String code;
    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String title() { return title; }
    @Override public String defaultDetail() { return defaultDetail; }
}
