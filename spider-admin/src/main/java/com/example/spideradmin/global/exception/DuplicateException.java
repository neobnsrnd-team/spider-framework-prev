package com.example.spideradmin.global.exception;

import com.example.spideradmin.global.exception.base.BaseException;

/**
 * 이미 존재하는 리소스와 충돌할 때 발생하는 예외 (HTTP 409).
 */
public class DuplicateException extends BaseException {

    public DuplicateException(String detail) {
        super(ErrorType.DUPLICATE_RESOURCE, detail);
    }
}
