package com.example.spiderlink.infra.tcp.handler;

import com.example.spiderlink.domain.meta.context.MessageEngineContext;
import com.example.spiderlink.domain.meta.dto.ComponentInfo;
import com.example.spiderlink.domain.meta.dto.RelationParam;
import com.example.spiderlink.domain.meta.dto.ServiceStep;
import com.example.spiderlink.domain.meta.mapper.MetaRoutingMapper;
import com.example.spiderlink.infra.tcp.biz.Biz;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.parser.JsonPayloadValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * FWK 메타데이터 기반 범용 서비스 체인 오케스트레이터.
 *
 * <p>FWK_LISTENER_TRX_MESSAGE에 등록된 커맨드를 수신하면
 * FWK_SERVICE → FWK_SERVICE_RELATION → FWK_COMPONENT 체인을 따라
 * SqlSession으로 SQL을 동적 실행한다. 새 커맨드를 Java 코드 수정 없이
 * DB 데이터 등록만으로 추가할 수 있다.</p>
 *
 * <p>다단계 실행 규칙:
 * <ul>
 *   <li>COMPONENT_TYPE='S' (SELECT) 결과가 null 이면 이후 스텝을 중단하고 실패 응답 반환</li>
 *   <li>SELECT 결과는 컨텍스트에 병합되어 다음 스텝 파라미터로 활용 가능</li>
 *   <li>COMPONENT_TYPE='U' (UPDATE/INSERT/DELETE) 는 auto-commit으로 즉시 반영</li>
 *   <li>COMPONENT_TYPE='B' (Biz 클래스) 는 COMPONENT_CLASS_NAME 스프링 빈을 리플렉션으로 호출,
 *       응답 맵을 컨텍스트에 병합. TCP·REST 등 외부 연동은 이 방식으로 확장한다.</li>
 *   <li>마지막 SELECT 또는 Biz 결과를 응답 payload로 반환</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetaDrivenServiceOrchestrator implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    /**
     * 이 bizApp이 담당하는 게이트웨이 ID.
     * GatewayLoader 활성 시 spider.gateway.id 값이 주입되며,
     * 정적 설정 유지 시 기본값 DEMO_GW를 사용한다.
     */
    @Value("${spider.gateway.id:DEMO_GW}")
    private String GW_ID;

    private static final String IO_TYPE = "I";

    private final MetaRoutingMapper metaRoutingMapper;
    private final TrxRecorder trxRecorder;
    private final SqlSessionFactory sqlSessionFactory;
    private final ObjectMapper objectMapper;
    /** FWK_MESSAGE_FIELD 기반 민감 필드 마스킹 — 거래 이력 INSERT 전 적용 */
    private final JsonPayloadValidator jsonPayloadValidator;
    /** Biz 타입 컴포넌트 리플렉션 호출 시 스프링 빈 조회용 */
    private final ApplicationContext applicationContext;
    /**
     * 기동 시 FWK_LISTENER_TRX_MESSAGE를 일괄 메모리 캐싱 — 거래 수신마다 DB 재조회 제거.
     * 참고소스의 MessageEngineContext.initialize()에 해당한다.
     */
    private final MessageEngineContext messageEngineContext;

    /**
     * 지원 커맨드 셋 — MessageEngineContext에서 파생.
     * volatile: refreshCommands()가 TCP 워커 스레드와 다른 스레드에서 참조를 교체하므로 가시성 보장 필요.
     */
    private volatile Set<String> supportedCommands = new HashSet<>();

    @PostConstruct
    void init() {
        // MessageEngineContext가 먼저 @PostConstruct로 초기화되므로 즉시 사용 가능
        supportedCommands = messageEngineContext.getRegisteredCommands(GW_ID);
        log.info("[MetaDrivenServiceOrchestrator] 등록된 커맨드 {}개 로드: {}", supportedCommands.size(), supportedCommands);
    }

    /**
     * 지원 커맨드 캐시와 MessageEngineContext 라우팅 맵을 DB에서 다시 로드한다.
     *
     * <p>Admin에서 FWK_LISTENER_TRX_MESSAGE 변경 후 재기동 없이 반영할 때
     * {@code POST /api/management/reload} (gubun=request_app_mapping) 호출 시 실행된다.</p>
     */
    public void refreshCommands() {
        messageEngineContext.reload();
        supportedCommands = messageEngineContext.getRegisteredCommands(GW_ID);
        log.info("[MetaDrivenServiceOrchestrator] 커맨드 캐시 갱신 완료: {}개 — {}", supportedCommands.size(), supportedCommands);
    }

    @Override
    public boolean supports(String command) {
        return supportedCommands.contains(command);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        Map<String, Object> payload = request.getPayload() != null ? request.getPayload() : Map.of();
        String userId = String.valueOf(payload.getOrDefault("userId", ""));
        // supports()를 통과한 커맨드이므로 getTrxId/getOrgId는 반드시 non-null
        String trxId = messageEngineContext.getTrxId(GW_ID, command);
        String orgId = messageEngineContext.getOrgId(GW_ID, command);

        // FWK_MESSAGE_FIELD 기준 마스킹 후 거래 이력 저장 — password 등 민감 필드 보호
        // maskForLog 키: trxId + "_REQ" (FWK_MESSAGE_FIELD는 TRX_ID 기준으로 필드 정의)
        trxRecorder.recordRequest(trxId, request.getRequestId(), userId,
                jsonPayloadValidator.maskForLog(trxId + "_REQ", payload));

        try {
            // FWK_MESSAGE_FIELD REQUIRED_YN='Y' 기준 필수 필드 검증 — 미등록 전문은 통과
            jsonPayloadValidator.validate(trxId + "_REQ", payload);

            String serviceId = metaRoutingMapper.selectServiceId(trxId, orgId, IO_TYPE);
            if (serviceId == null) {
                throw new IllegalStateException("서비스 미등록: trxId=" + trxId);
            }

            List<ServiceStep> steps = metaRoutingMapper.selectServiceSteps(serviceId);
            // payload를 초기 컨텍스트로 설정 — SELECT 결과 병합 후 다음 스텝 파라미터로 활용
            Map<String, Object> context = new HashMap<>(payload);
            Map<String, Object> lastSelectResult = null;

            try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
                for (ServiceStep step : steps) {
                    ComponentInfo comp = metaRoutingMapper.selectComponent(step.getComponentId());
                    List<RelationParam> params = metaRoutingMapper.selectRelationParams(
                            serviceId, step.getServiceSeqNo(), step.getComponentId());

                    // PARAM_KEY → context에서 PARAM_VALUE 키로 꺼낸 값으로 SQL 파라미터 맵 구성
                    Map<String, Object> paramMap = new HashMap<>();
                    for (RelationParam rp : params) {
                        paramMap.put(rp.getParamKey(), context.getOrDefault(rp.getParamValue(), ""));
                    }

                    log.debug("[MetaDrivenServiceOrchestrator] step={} type={} params={}",
                            step.getServiceSeqNo(), comp.getComponentType(), paramMap.keySet());

                    if ("S".equals(comp.getComponentType())) {
                        String statementId = comp.getComponentClassName() + "." + comp.getComponentMethodName();
                        Object result = sqlSession.selectOne(statementId, paramMap);
                        if (result == null) {
                            // SELECT 결과 없음 → 중단, 실패 응답
                            JsonCommandResponse failResp = JsonCommandResponse.builder()
                                    .command(command).success(false).error("조회 결과가 없습니다.").build();
                            trxRecorder.recordResponse(trxId, request.getRequestId(), userId, "no result", "N");
                            return failResp;
                        }
                        // DTO → Map 변환 후 컨텍스트 병합 (다음 스텝에서 참조 가능)
                        lastSelectResult = objectMapper.convertValue(result, Map.class);
                        context.putAll(lastSelectResult);
                    } else if ("U".equals(comp.getComponentType())) {
                        // UPDATE / INSERT / DELETE (auto-commit)
                        String statementId = comp.getComponentClassName() + "." + comp.getComponentMethodName();
                        sqlSession.update(statementId, paramMap);
                    } else {
                        // Biz 클래스 리플렉션 호출 — TCP·REST 등 외부 연동
                        // COMPONENT_CLASS_NAME = 스프링 빈 클래스명, COMPONENT_METHOD_NAME = 메서드/커맨드명
                        @SuppressWarnings("unchecked")
                        Class<? extends Biz> bizClass =
                                (Class<? extends Biz>) Class.forName(comp.getComponentClassName());
                        Biz biz = applicationContext.getBean(bizClass);
                        Map<String, Object> bizResult = biz.execute(comp.getComponentMethodName(), paramMap);
                        if (!bizResult.isEmpty()) {
                            lastSelectResult = new HashMap<>(bizResult);
                            context.putAll(lastSelectResult);
                        }
                    }
                }
            }

            JsonCommandResponse response = JsonCommandResponse.builder()
                    .command(command).success(true).payload(toCamelCaseKeys(lastSelectResult)).build();
            // FWK_MESSAGE_FIELD 기준 마스킹 후 응답 거래 이력 저장
            trxRecorder.recordResponse(trxId, request.getRequestId(), userId,
                    jsonPayloadValidator.maskForLog(trxId + "_RES", lastSelectResult), "Y");
            return response;

        } catch (Exception e) {
            log.error("[MetaDrivenServiceOrchestrator] 처리 오류: command={}, error={}", command, e.getMessage(), e);
            JsonCommandResponse errResp = JsonCommandResponse.builder()
                    .command(command).success(false).error("처리 중 오류가 발생했습니다.").build();
            trxRecorder.recordResponse(trxId, request.getRequestId(), userId, e.getMessage(), "N");
            return errResp;
        }
    }

    /** Oracle 대문자 컬럼명(USER_ID)을 camelCase(userId)로 변환 — biz-channel API가 camelCase 키를 기대함 */
    private Map<String, Object> toCamelCaseKeys(Map<String, Object> map) {
        if (map == null) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(toCamelCase(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private String toCamelCase(String columnName) {
        if (columnName == null) return null;
        // "_" 없으면 이미 camelCase 또는 단일 소문자 — 원본 유지
        if (!columnName.contains("_")) return columnName;
        String lower = columnName.toLowerCase();
        String[] parts = lower.split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            }
        }
        return sb.toString();
    }
}
