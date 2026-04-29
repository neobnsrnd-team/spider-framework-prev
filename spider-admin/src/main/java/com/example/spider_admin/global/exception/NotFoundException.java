package com.example.spider_admin.global.exception;

import com.example.spider_admin.global.exception.base.BaseException;

/**
 * 요청한 리소스를 찾을 수 없을 때 발생하는 예외 (HTTP 404).
 */
public class NotFoundException extends BaseException {

    public NotFoundException(String detail) {
        super(ErrorType.RESOURCE_NOT_FOUND, detail);
    }
}
