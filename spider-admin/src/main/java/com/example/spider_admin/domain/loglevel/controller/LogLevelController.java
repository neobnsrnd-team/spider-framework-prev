package com.example.spider_admin.domain.loglevel.controller;

import com.example.spider_admin.domain.loglevel.dto.AdditivityUpdateRequest;
import com.example.spider_admin.domain.loglevel.dto.LogLevelPropagateRequest;
import com.example.spider_admin.domain.loglevel.dto.LogLevelResponse;
import com.example.spider_admin.domain.loglevel.dto.LogLevelUpdateRequest;
import com.example.spider_admin.domain.loglevel.service.LogLevelPropagationService;
import com.example.spider_admin.domain.loglevel.service.LogLevelService;
import com.example.spider_admin.domain.reload.dto.ReloadResultResponse;
import com.example.spider_admin.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h3>LogLevel REST Controller</h3>
 * <p>Logback 로거의 레벨 및 Additivity를 런타임에 조회·변경·Reload하는 엔드포인트를 제공합니다.</p>
 *
 * <h4>API 엔드포인트:</h4>
 * <ul>
 *     <li>GET  /api/log-level             — 전체 로거 목록 조회</li>
 *     <li>PATCH /api/log-level/level      — Admin 변경 + default 그룹 전체 WAS 자동 Reload</li>
 *     <li>PATCH /api/log-level/additivity — Admin 변경 + default 그룹 전체 WAS 자동 Reload</li>
 *     <li>POST /api/log-level/propagate   — 지정된 WAS 인스턴스에 Reload (API 직접 호출용)</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/log-level")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('LOG_LEVEL:R')")
public class LogLevelController {

    private final LogLevelService logLevelService;
    private final LogLevelPropagationService logLevelPropagationService;

    /**
     * 전체 로거 목록을 Logback LoggerContext에서 조회합니다.
     *
     * @return 로거 이름·레벨·Additivity·Appender 목록
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<LogLevelResponse>>> getAllLoggers() {
        log.info("GET /api/log-level - Fetching all loggers");
        List<LogLevelResponse> loggers = logLevelService.findAll();
        return ResponseEntity.ok(ApiResponse.success(loggers));
    }

    /**
     * Admin 자신의 로그 레벨을 변경하고, default 그룹 전체 WAS에 자동 Reload합니다.
     *
     * @param request 로거 이름 + 변경할 레벨 (ERROR/WARN/INFO/DEBUG/TRACE/OFF, null이면 상속)
     * @return 각 WAS별 Reload 결과
     */
    @PatchMapping("/level")
    @PreAuthorize("hasAuthority('LOG_LEVEL:W')")
    public ResponseEntity<ApiResponse<ReloadResultResponse>> updateLevel(
            @Valid @RequestBody LogLevelUpdateRequest request) {
        log.info("PATCH /api/log-level/level - logName={}, level={}", request.getLogName(), request.getLevel());
        logLevelService.updateLevel(request);
        ReloadResultResponse result =
                logLevelPropagationService.propagateLevelToDefaultGroup(request.getLogName(), request.getLevel());
        return ResponseEntity.ok(ApiResponse.success("Reload가 완료되었습니다", result));
    }

    /**
     * Admin 자신의 Additivity를 변경하고, default 그룹 전체 WAS에 자동 Reload합니다.
     *
     * @param request 로거 이름 + Additivity (Y/N)
     * @return 각 WAS별 Reload 결과
     */
    @PatchMapping("/additivity")
    @PreAuthorize("hasAuthority('LOG_LEVEL:W')")
    public ResponseEntity<ApiResponse<ReloadResultResponse>> updateAdditivity(
            @Valid @RequestBody AdditivityUpdateRequest request) {
        log.info(
                "PATCH /api/log-level/additivity - logName={}, additivity={}",
                request.getLogName(),
                request.getAdditivity());
        logLevelService.updateAdditivity(request);
        ReloadResultResponse result = logLevelPropagationService.propagateAdditivityToDefaultGroup(
                request.getLogName(), request.getAdditivity());
        return ResponseEntity.ok(ApiResponse.success("Reload가 완료되었습니다", result));
    }

    /**
     * 지정된 WAS 인스턴스에 로그 레벨 또는 Additivity를 Reload합니다.
     *
     * <p>통신 방식(HTTP/TCP)은 FWK_PROPERTY의 {@code {instanceId}.COMM_TYPE}에 따라 자동 결정됩니다.
     * {@code gubun}은 {@code log_config_level} 또는 {@code log_config_additivity}를 사용합니다.</p>
     *
     * @param request Reload 대상 WAS 목록, gubun, logName, level/additivity
     */
    @PostMapping("/propagate")
    @PreAuthorize("hasAuthority('LOG_LEVEL:W')")
    public ResponseEntity<ApiResponse<ReloadResultResponse>> propagate(
            @Valid @RequestBody LogLevelPropagateRequest request) {
        log.info(
                "POST /api/log-level/propagate - gubun={}, logName={}, instanceIds={}",
                request.getGubun(),
                request.getLogName(),
                request.getInstanceIds());
        ReloadResultResponse result = logLevelPropagationService.propagate(request);
        return ResponseEntity.ok(ApiResponse.success("Reload가 완료되었습니다", result));
    }
}
