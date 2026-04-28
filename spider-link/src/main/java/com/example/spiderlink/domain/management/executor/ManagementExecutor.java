package com.example.spiderlink.domain.management.executor;

import java.util.Map;

/**
 * 관리 명령 실행기 인터페이스 (전략 패턴).
 *
 * <p>DGB {@code ManagementAgent.doProcess(gubun)} 구조에 대응한다.
 * {@link com.example.spiderlink.domain.management.ManagementReloadCommandHandler}가
 * {@code gubun} 값으로 적합한 실행기를 선택하여 위임한다.</p>
 *
 * <p>spider-link는 {@link LogLevelExecutor}, {@link LogAdditivityExecutor}를 기본 제공한다.
 * 각 WAS는 자신에게 필요한 실행기를 {@code @Component}로 등록하여 확장한다.</p>
 */
public interface ManagementExecutor {

    /**
     * 이 실행기가 주어진 gubun을 처리할 수 있는지 판단한다.
     *
     * @param gubun 관리 명령 구분자 (예: "log_config_level", "batch_reload")
     * @return 처리 가능 여부
     */
    boolean supports(String gubun);

    /**
     * 관리 명령을 실행하고 결과를 반환한다.
     *
     * @param params 명령 파라미터 ({@code gubun} 포함)
     * @return 실행 결과 맵
     */
    Map<String, Object> execute(Map<String, Object> params);
}
