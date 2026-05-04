package com.example.spideradmin.global.exception.base;

import com.example.spideradmin.global.exception.ErrorType;
import lombok.Getter;

/**
 * 비즈니스 예외 최상위 클래스.
 *
 * @see ErrorType
 * @see com.example.spideradmin.global.exception.GlobalExceptionHandler
 */
@Getter
public class BaseException extends RuntimeException {

    private final transient ErrorType errorType;
    private final String detailMessage;

    public BaseException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.detailMessage = null;
    }

    public BaseException(ErrorType errorType, String detailMessage) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.detailMessage = detailMessage;
    }

    public BaseException(ErrorType errorType, String detailMessage, Throwable cause) {
        super(errorType.getMessage(), cause);
        this.errorType = errorType;
        this.detailMessage = detailMessage;
    }
}
