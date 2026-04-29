package com.example.spider_admin.infra.tcp.adapter;

/**
 * TCP 통신 어댑터 인터페이스.
 *
 * <p>레퍼런스(spiderlink_Admin SocketManagementAdapter) 구조를 참고한다.
 * 로컬/원격 분기(isLocal)와 실행(doProcess)을 추상화한다.</p>
 *
 * @param <REQ> 요청 페이로드 타입
 * @param <RES> 응답 타입
 */
public interface ManagementAdapter<REQ, RES> {

    /**
     * 대상 서버에 커맨드를 전송하고 결과를 반환한다.
     * isLocal() 결과에 따라 직접 실행 또는 TCP 전송으로 분기한다.
     *
     * @param command 실행할 커맨드
     * @param payload 커맨드 페이로드
     * @return 실행 결과
     */
    RES doProcess(String command, REQ payload);

    /**
     * 대상이 현재 프로세스와 동일한 JVM 내 로컬 인스턴스인지 판단한다.
     * 로컬이면 TCP 통신 없이 직접 실행한다.
     *
     * @return 로컬 여부
     */
    boolean isLocal();
}
