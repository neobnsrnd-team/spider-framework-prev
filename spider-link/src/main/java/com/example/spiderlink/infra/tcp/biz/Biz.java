package com.example.spiderlink.infra.tcp.biz;

import java.util.Map;

/**
 * MetaDrivenServiceOrchestrator 에서 리플렉션으로 호출되는 비즈니스 로직 인터페이스.
 *
 * <p>원본 {@code spider.common.biz.Biz} 의 POC 경량화 버전.
 * {@code COMPONENT_TYPE != 'S'/'U'} 인 컴포넌트는 {@code COMPONENT_CLASS_NAME} 에
 * 이 인터페이스를 구현한 스프링 빈의 클래스명을 등록한다.</p>
 *
 * <pre>{@code
 *   // FWK_COMPONENT 등록 예시
 *   COMPONENT_TYPE      = 'B'
 *   COMPONENT_CLASS_NAME = 'com.example.spiderlink.infra.tcp.biz.TcpCallBiz'
 *   COMPONENT_METHOD_NAME = 'AUTH_LOGIN'   ← methodName 으로 전달됨
 * }</pre>
 */
public interface Biz {

    /**
     * 비즈니스 로직을 수행하고 다음 스텝 컨텍스트에 병합할 결과를 반환한다.
     *
     * @param methodName {@code COMPONENT_METHOD_NAME} 값 — 구현체별 의미가 다름
     *                   (TcpCallBiz 는 TCP 커맨드명, 향후 RestCallBiz 는 경로 등)
     * @param params     FWK_RELATION_PARAM 기반으로 컨텍스트에서 바인딩된 파라미터 맵
     * @return 다음 스텝 컨텍스트에 병합할 결과 맵 (없으면 빈 맵)
     * @throws Exception 외부 호출 실패 등 처리 불가 오류
     */
    Map<String, Object> execute(String methodName, Map<String, Object> params) throws Exception;
}
