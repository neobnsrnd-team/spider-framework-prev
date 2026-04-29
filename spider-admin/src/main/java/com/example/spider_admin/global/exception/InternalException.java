package com.example.spider_admin.global.exception;

import com.example.spider_admin.global.exception.base.BaseException;

/**
 * 서버 내부 오류가 발생했을 때 사용하는 예외 (HTTP 500).
 */
public class InternalException extends BaseException {

    public InternalException(String detail) {
        super(ErrorType.INTERNAL_ERROR, detail);
    }

    public InternalException(String detail, Throwable cause) {
        super(ErrorType.INTERNAL_ERROR, detail, cause);
    }
}
