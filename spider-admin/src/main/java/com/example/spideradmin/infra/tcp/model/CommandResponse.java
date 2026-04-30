package com.example.spideradmin.infra.tcp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * TCP 커맨드 응답 공통 추상 모델.
 *
 * @param <T> 응답 데이터 타입
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class CommandResponse<T> {

    /** 요청과 대응되는 커맨드 이름 */
    private String command;

    /** 성공 여부 */
    private boolean success;

    /** 응답 데이터 */
    private T data;

    /** 실패 시 에러 메시지 */
    private String errorMessage;
}
