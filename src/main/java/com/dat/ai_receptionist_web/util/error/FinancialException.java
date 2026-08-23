package com.dat.ai_receptionist_web.util.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class FinancialException extends RuntimeException {
    private final String code;
    private final HttpStatus status;

    public FinancialException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
