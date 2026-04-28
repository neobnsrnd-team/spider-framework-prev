package com.example.mockcore.infra;

/**
 * 고정길이 바이너리 프로토콜 기반 계정계 Mock 커맨드 핸들러 인터페이스.
 *
 * <p>프레이밍: 4바이트 길이 프리픽스(int, big-endian) + 고정길이 바이너리 바이트열.
 * spider-link TcpClient.send() 메서드와 동일한 프레이밍을 사용한다.</p>
 *
 * <p>REQ 바이트 레이아웃: COMMAND(C,20) + REQUEST_ID(C,36) + 커맨드별 필드
 * RES 바이트 레이아웃: SUCCESS(C,1) + ERROR_MSG(K,200) + 커맨드별 필드</p>
 */
public interface LegacyCoreHandler {

    /** 처리할 커맨드 이름 (예: CORE_USER_AUTH) */
    String getCommand();

    /**
     * 요청 바이트를 처리하여 응답 바이트를 반환한다.
     *
     * <p>구현체는 {@link FixedMessageReader} / {@link FixedMessageWriter} 를 활용한다.
     * 예외 발생 시에도 SUCCESS=N + ERROR_MSG를 포함한 완전한 RES 바이트를 반환해야 한다.</p>
     *
     * @param requestBytes 수신된 전체 요청 바이트 (헤더 포함)
     * @return 전송할 응답 바이트 (헤더 포함)
     */
    byte[] handle(byte[] requestBytes);
}
