package com.example.spiderlink.domain.meta.context;

import com.example.spiderlink.domain.meta.dto.TrxMappingEntry;
import com.example.spiderlink.domain.meta.mapper.MetaRoutingMapper;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 기동 시 FWK_LISTENER_TRX_MESSAGE 전체를 메모리에 캐싱하는 라우팅 컨텍스트.
 *
 * <p>참고소스(spiderLink_Admin)의 {@code MessageEngineContext.initialize()} 역할에 해당한다.
 * 거래 수신마다 DB를 재조회하는 대신, 이 컨텍스트에서 TRX_ID · ORG_ID를 반환한다.</p>
 *
 * <p>캐시 구조:
 * <pre>
 *   Map key : gwId + ":" + reqIdCode  (ex. "DEMO_GW:LOGIN")
 *   Map value: TrxMappingEntry (gwId, reqIdCode, orgId, trxId)
 * </pre>
 * </p>
 *
 * <p>Admin에서 FWK_LISTENER_TRX_MESSAGE 변경 후 재기동 없이 반영하려면 {@link #reload()}를 호출한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageEngineContext {

    private final MetaRoutingMapper metaRoutingMapper;

    /**
     * gwId + ":" + reqIdCode → TrxMappingEntry.
     * volatile: reload()는 관리 스레드에서, 조회는 TCP 워커 스레드에서 실행될 수 있으므로 가시성 보장 필요.
     */
    private volatile Map<String, TrxMappingEntry> trxMappings = new HashMap<>();

    @PostConstruct
    void init() {
        reload();
    }

    /**
     * FWK_LISTENER_TRX_MESSAGE 전체를 DB에서 다시 읽어 캐시를 교체한다.
     *
     * <p>Admin에서 커맨드·기관 매핑을 변경한 뒤 재기동 없이 반영할 때 호출한다.</p>
     */
    public void reload() {
        List<TrxMappingEntry> all = metaRoutingMapper.selectAllTrxMappings();
        Map<String, TrxMappingEntry> map = new HashMap<>(all.size() * 2);
        for (TrxMappingEntry e : all) {
            map.put(buildKey(e.getGwId(), e.getReqIdCode()), e);
        }
        // 참조를 atomic하게 교체 — 진행 중인 거래는 이전 맵을 계속 사용하므로 안전
        this.trxMappings = map;
        log.info("[MessageEngineContext] FWK_LISTENER_TRX_MESSAGE {}건 로드 완료", map.size());
    }

    /**
     * GW_ID + 커맨드(REQ_ID_CODE)에 해당하는 TRX_ID를 반환한다.
     *
     * @return 미등록 커맨드이면 {@code null}
     */
    public String getTrxId(String gwId, String command) {
        TrxMappingEntry entry = trxMappings.get(buildKey(gwId, command));
        return entry != null ? entry.getTrxId() : null;
    }

    /**
     * GW_ID + 커맨드(REQ_ID_CODE)에 해당하는 ORG_ID를 반환한다.
     *
     * @return 미등록 커맨드이면 {@code null}
     */
    public String getOrgId(String gwId, String command) {
        TrxMappingEntry entry = trxMappings.get(buildKey(gwId, command));
        return entry != null ? entry.getOrgId() : null;
    }

    /**
     * GW_ID에 등록된 커맨드(REQ_ID_CODE) 전체를 반환한다.
     *
     * <p>{@code MetaDrivenCommandHandler.supports()} 판별용 커맨드 셋 초기화에 사용한다.</p>
     */
    public Set<String> getRegisteredCommands(String gwId) {
        String prefix = gwId + ":";
        return trxMappings.entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .map(e -> e.getValue().getReqIdCode())
                .collect(Collectors.toSet());
    }

    private static String buildKey(String gwId, String reqIdCode) {
        return gwId + ":" + reqIdCode;
    }
}
