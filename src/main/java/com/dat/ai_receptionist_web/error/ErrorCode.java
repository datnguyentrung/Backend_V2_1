package com.dat.ai_receptionist_web.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String code();

    HttpStatus status();

    String title();

    String defaultDetail();

    default String type() {
        return "/errors/" + code().toLowerCase().replace('_', '-');
    }
}
