package com.example.spidercommon.infra.tcp.model;

/**
 * TCP 커맨드를 식별하는 타입에 부여하는 마커 인터페이스.
 *
 * <p>{@link com.example.spidercommon.infra.tcp.handler.CommandDispatcher}가
 * 커맨드 이름을 추출하여 적합한 핸들러를 선택하는 데 사용한다.</p>
 */
public interface HasCommand {
    String getCommand();
}
