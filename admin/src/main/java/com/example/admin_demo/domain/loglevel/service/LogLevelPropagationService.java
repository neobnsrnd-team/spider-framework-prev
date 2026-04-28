package com.example.admin_demo.domain.loglevel.service;

import com.example.admin_demo.domain.loglevel.dto.LogLevelPropagateRequest;
import com.example.admin_demo.domain.reload.dto.ReloadExecuteRequest;
import com.example.admin_demo.domain.reload.dto.ReloadResultResponse;
import com.example.admin_demo.domain.reload.enums.ReloadType;
import com.example.admin_demo.domain.reload.service.ReloadService;
import com.example.admin_demo.global.exception.InvalidInputException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 로그 레벨·Additivity Reload 서비스.
 *
 * <p>Admin에서 변경된 로그 설정을 WAS 인스턴스에 실시간 Reload한다.
 * 통신 방식(HTTP/TCP)은 {@link ReloadService}가 FWK_PROPERTY의 COMM_TYPE에 따라 자동 선택한다.</p>
 *
 * <p>저장 시 자동 Reload 대상은 {@code reload.default-was-group} 프로퍼티로 지정된 그룹이다.
 * ASIS의 {@code ALL_WAS_CONFIG} 역할을 DB 그룹({@code FWK_WAS_GROUP})으로 대체한 구조다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogLevelPropagationService {

    private final ReloadService reloadService;

    /** 저장 시 자동 Reload 대상 WAS 그룹 — application.yml reload.default-was-group */
    @Value("${reload.default-was-group}")
    private String defaultGroup;

    /**
     * 로그 레벨 또는 Additivity 변경을 지정된 WAS 인스턴스에 Reload한다.
     *
     * @param request Reload 요청 ({@code gubun}, {@code logName}, {@code level}/{@code additivity}, {@code instanceIds})
     * @return 각 WAS 인스턴스별 Reload 결과
     * @throws InvalidInputException gubun이 로그 관련 타입이 아닐 때
     */
    public ReloadResultResponse propagate(LogLevelPropagateRequest request) {
        validateGubun(request.getGubun());

        Map<String, String> additionalParams = new HashMap<>();
        additionalParams.put("logName", request.getLogName());

        if (ReloadType.LOG_LEVEL.getCode().equals(request.getGubun())) {
            // null/blank level → 부모 상속으로 처리 (파라미터 미포함)
            if (request.getLevel() != null && !request.getLevel().isBlank()) {
                additionalParams.put("level", request.getLevel());
            }
        } else {
            // log_config_additivity
            if (!"Y".equals(request.getAdditivity()) && !"N".equals(request.getAdditivity())) {
                throw new InvalidInputException("additivity는 Y 또는 N이어야 합니다.");
            }
            additionalParams.put("additivity", request.getAdditivity());
        }

        log.info(
                "[LogLevelPropagationService] Reload 요청: gubun={}, logName={}, instanceIds={}",
                request.getGubun(),
                request.getLogName(),
                request.getInstanceIds());

        return reloadService.executeReload(ReloadExecuteRequest.builder()
                .reloadType(request.getGubun())
                .instanceIds(request.getInstanceIds())
                .additionalParams(additionalParams)
                .build());
    }

    /**
     * 로그 레벨 변경을 default 그룹 전체 WAS에 자동 Reload한다.
     *
     * <p>저장 버튼 클릭 시 Admin 자신의 레벨 변경 직후 호출된다.</p>
     *
     * @param logName 대상 로거 이름
     * @param level   변경할 레벨 (null이면 상속 — 파라미터 미포함으로 처리)
     * @return 각 WAS별 Reload 결과
     */
    public ReloadResultResponse propagateLevelToDefaultGroup(String logName, String level) {
        Map<String, String> params = new HashMap<>();
        params.put("logName", logName);
        if (level != null && !level.isBlank()) {
            params.put("level", level);
        }
        log.info("[Reload] 레벨 자동 Reload: group={}, logName={}, level={}", defaultGroup, logName, level);
        return reloadService.executeReloadForGroup(defaultGroup, ReloadType.LOG_LEVEL.getCode(), params);
    }

    /**
     * Additivity 변경을 default 그룹 전체 WAS에 자동 Reload한다.
     *
     * @param logName    대상 로거 이름
     * @param additivity Y 또는 N
     * @return 각 WAS별 Reload 결과
     */
    public ReloadResultResponse propagateAdditivityToDefaultGroup(String logName, String additivity) {
        if (!"Y".equals(additivity) && !"N".equals(additivity)) {
            throw new InvalidInputException("additivity는 Y 또는 N이어야 합니다.");
        }
        Map<String, String> params = new HashMap<>();
        params.put("logName", logName);
        params.put("additivity", additivity);
        log.info("[Reload] Additivity 자동 Reload: group={}, logName={}, additivity={}", defaultGroup, logName, additivity);
        return reloadService.executeReloadForGroup(defaultGroup, ReloadType.LOG_ADDITIVITY.getCode(), params);
    }

    private void validateGubun(String gubun) {
        if (!ReloadType.LOG_LEVEL.getCode().equals(gubun)
                && !ReloadType.LOG_ADDITIVITY.getCode().equals(gubun)) {
            throw new InvalidInputException("로그 Reload에 사용할 수 없는 gubun입니다: " + gubun);
        }
    }
}
