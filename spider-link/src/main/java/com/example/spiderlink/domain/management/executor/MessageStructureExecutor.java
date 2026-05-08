package com.example.spiderlink.domain.management.executor;

import com.example.spidercommon.domain.management.executor.ManagementExecutor;
import com.example.spiderlink.infra.tcp.parser.MessageStructureCache;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

/**
 * FWK_MESSAGE 전문 구조 캐시 초기화 관리 실행기.
 *
 * <p>{@code gubun = "message"} 명령을 처리한다.
 * Admin에서 전문 구조(FWK_MESSAGE / FWK_MESSAGE_FIELD) 변경 후 이 실행기가 호출되면
 * {@link MessageStructureCache}의 인메모리 캐시를 초기화한다.</p>
 *
 * <p>spider-link AutoConfiguration으로 자동 등록된다.
 * {@link MessageStructureCache} 빈이 없으면(고정길이 전문 미사용) 등록되지 않는다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class MessageStructureExecutor implements ManagementExecutor {

    static final String GUBUN = "message";

    /** 고정길이 전문 미사용 시 null — supports()가 false를 반환하므로 execute()는 호출되지 않는다 */
    @Nullable
    private final MessageStructureCache messageStructureCache;

    @Override
    public boolean supports(String gubun) {
        return GUBUN.equals(gubun) && messageStructureCache != null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> params) {
        messageStructureCache.clear();
        log.info("[MessageStructureExecutor] 전문 구조 캐시 초기화 완료");
        return Map.of("gubun", GUBUN);
    }
}
