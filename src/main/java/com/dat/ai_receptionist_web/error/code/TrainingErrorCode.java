package com.dat.ai_receptionist_web.error.code;

import com.dat.ai_receptionist_web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum TrainingErrorCode implements ErrorCode {
    CLASS_SESSION_NOT_FOUND("CLASS_SESSION_NOT_FOUND", HttpStatus.NOT_FOUND, "Class session not found",
            "Class session not found"),
    COACH_ASSIGNMENT_NOT_FOUND("COACH_ASSIGNMENT_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Coach assignment not found", "Coach assignment not found"),
    COACH_TIMESHEET_NOT_FOUND("COACH_TIMESHEET_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Coach timesheet not found", "Coach timesheet not found"),
    STUDENT_ATTENDANCE_NOT_FOUND("STUDENT_ATTENDANCE_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Student attendance not found", "Student attendance not found"),
    STUDENT_ENROLLMENT_NOT_FOUND("STUDENT_ENROLLMENT_NOT_FOUND", HttpStatus.NOT_FOUND,
            "Student enrollment not found", "Student enrollment not found");

    private final String code;
    private final HttpStatus status;
    private final String title;
    private final String defaultDetail;

    @Override public String code() { return code; }
    @Override public HttpStatus status() { return status; }
    @Override public String title() { return title; }
    @Override public String defaultDetail() { return defaultDetail; }
}
