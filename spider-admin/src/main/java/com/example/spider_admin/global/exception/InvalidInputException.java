package com.example.spider_admin.global.exception;

import com.example.spider_admin.global.exception.base.BaseException;

/**
 * 입력값이 유효하지 않을 때 발생하는 예외 (HTTP 400).
 */
public class InvalidInputException extends BaseException {

    public InvalidInputException(String detail) {
        super(ErrorType.INVALID_INPUT, detail);
    }
}
