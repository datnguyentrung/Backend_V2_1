package com.dat.ai_receptionist_web.error;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode errorCode;
    private final String safeDetail;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, null);
    }

    public ApiException(ErrorCode errorCode, String safeDetail) {
        super(errorCode.code());
        this.errorCode = errorCode;
        this.safeDetail = safeDetail;
    }

    public String responseDetail() {
        return safeDetail == null || safeDetail.isBlank() ? errorCode.defaultDetail() : safeDetail;
    }
}
