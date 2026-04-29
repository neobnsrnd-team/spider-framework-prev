package com.example.spider_admin.global.exception;

import com.example.spider_admin.global.exception.base.BaseException;

/**
 * 현재 리소스 상태에서 허용되지 않는 전이를 시도할 때 발생하는 예외 (HTTP 409).
 *
 * <p>예) {@code APPROVED} 상태의 이미지를 다시 승인 요청하거나,
 * 동시 승인 레이스로 이미 처리된 요청을 다시 처리하려는 경우에 사용한다.
 */
public class InvalidStateException extends BaseException {

    public InvalidStateException(String detail) {
        super(ErrorType.INVALID_STATE, detail);
    }
}
