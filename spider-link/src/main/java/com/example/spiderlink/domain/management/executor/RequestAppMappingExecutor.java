package com.example.spiderlink.domain.management.executor;

import com.example.spidercommon.domain.management.executor.ManagementExecutor;
import com.example.spiderlink.infra.tcp.handler.MetaDrivenServiceOrchestrator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FWK_LISTENER_TRX_MESSAGE 커맨드 캐시 갱신 관리 실행기.
 *
 * <p>{@code gubun = "request_app_mapping"} 명령을 처리한다.
 * Admin에서 커맨드-핸들러 매핑 변경 후 이 실행기가 호출되면
 * {@link MetaDrivenServiceOrchestrator}의 인메모리 커맨드 캐시를 갱신한다.</p>
 *
 * <p>spider-link AutoConfiguration으로 자동 등록된다.
 * {@link MetaDrivenServiceOrchestrator} 빈이 없으면 등록되지 않는다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RequestAppMappingExecutor implements ManagementExecutor {

    static final String GUBUN = "request_app_mapping";

    private final MetaDrivenServiceOrchestrator metaDrivenServiceOrchestrator;

    @Override
    public boolean supports(String gubun) {
        return GUBUN.equals(gubun);
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> params) {
        metaDrivenServiceOrchestrator.refreshCommands();
        log.info("[RequestAppMappingExecutor] 커맨드 캐시 갱신 완료");
        return Map.of("gubun", GUBUN);
    }
}
