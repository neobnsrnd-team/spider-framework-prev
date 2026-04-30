package com.example.spideradmin.global.exception;

import static org.springframework.boot.logging.LogLevel.*;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

/**
 * HTTP 카테고리 기반 통합 에러 타입.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorType {
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND, WARN),
    DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", "이미 존재하는 리소스입니다.", HttpStatus.CONFLICT, WARN),
    INVALID_STATE("INVALID_STATE", "현재 상태에서 수행할 수 없는 작업입니다.", HttpStatus.CONFLICT, INFO),
    INSUFFICIENT_RESOURCE("INSUFFICIENT_RESOURCE", "리소스가 부족합니다.", HttpStatus.UNPROCESSABLE_ENTITY, INFO),
    INVALID_INPUT("INVALID_INPUT", "입력값이 유효하지 않습니다.", HttpStatus.BAD_REQUEST, DEBUG),
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED, WARN),
    FORBIDDEN("FORBIDDEN", "권한이 없습니다.", HttpStatus.FORBIDDEN, WARN),
    EXTERNAL_SERVICE_ERROR("EXTERNAL_SERVICE_ERROR", "외부 서비스에 일시적인 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY, ERROR),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR, ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    private final LogLevel logLevel;
}
