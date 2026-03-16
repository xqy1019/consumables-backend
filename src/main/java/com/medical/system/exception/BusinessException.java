package com.medical.system.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;
    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.code = 400;
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessException(String errorCode, String message, Integer code) {
        super(message);
        this.code = code;
        this.errorCode = errorCode;
    }
}
