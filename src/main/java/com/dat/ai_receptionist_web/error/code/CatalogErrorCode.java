package com.dat.ai_receptionist_web.error.code;

import com.dat.ai_receptionist_web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum CatalogErrorCode implements ErrorCode {
    CLASS_SCHEDULE_NOT_FOUND("CLASS_SCHEDULE_NOT_FOUND", HttpStatus.NOT_FOUND, "Class schedule not found",
            "Class schedule not found"),
    COURSE_NOT_FOUND(
            "COURSE_NOT_FOUND",
            HttpStatus.NOT_FOUND,
            "Course not found",
            "No course exists for the provided courseId"),
    COURSE_PRICE_NOT_FOUND("COURSE_PRICE_NOT_FOUND", HttpStatus.NOT_FOUND, "Course price not found",
            "Course price not found");

    private final String code;
    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String title() { return title; }
    @Override public String defaultDetail() { return defaultDetail; }
}
