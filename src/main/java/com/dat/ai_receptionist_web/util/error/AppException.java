package com.dat.ai_receptionist_web.util.error;

import com.dat.ai_receptionist_web.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AppException extends RuntimeException {
    private ErrorCode errorCode;
}
