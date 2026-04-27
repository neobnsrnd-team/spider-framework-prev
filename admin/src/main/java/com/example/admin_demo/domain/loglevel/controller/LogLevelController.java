package com.example.admin_demo.domain.loglevel.controller;

import com.example.admin_demo.domain.loglevel.dto.AdditivityUpdateRequest;
import com.example.admin_demo.domain.loglevel.dto.LogLevelResponse;
import com.example.admin_demo.domain.loglevel.dto.LogLevelUpdateRequest;
import com.example.admin_demo.domain.loglevel.service.LogLevelService;
import com.example.admin_demo.global.client.SpiderLogLevelClient;
import com.example.admin_demo.global.dto.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <h3>LogLevel REST Controller</h3>
 * <p>Logback 로거의 레벨 및 Additivity를 런타임에 조회·변경하는 엔드포인트를 제공합니다.</p>
 *
 * <h4>API 엔드포인트:</h4>
 * <ul>
 *     <li>GET /api/log-level — 전체 로거 목록 조회</li>
 *     <li>PATCH /api/log-level/level — 로그 레벨 변경</li>
 *     <li>PATCH /api/log-level/additivity — Additivity 변경</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/log-level")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('LOG_LEVEL:R')")
public class LogLevelController {

    private final LogLevelService logLevelService;
    private final SpiderLogLevelClient spiderLogLevelClient;

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
     * 특정 로거의 로그 레벨을 변경합니다.
     *
     * @param request 로거 이름 + 변경할 레벨 (ERROR/WARN/INFO/DEBUG/TRACE/OFF)
     */
    @PatchMapping("/level")
    @PreAuthorize("hasAuthority('LOG_LEVEL:W')")
    public ResponseEntity<ApiResponse<Void>> updateLevel(@Valid @RequestBody LogLevelUpdateRequest request) {
        log.info("PATCH /api/log-level/level - logName={}, level={}", request.getLogName(), request.getLevel());
        logLevelService.updateLevel(request);
        // Admin 자신의 Logback 변경 후 spider-link에도 동일하게 전파 (실패해도 200 반환)
        spiderLogLevelClient.syncLevel(request.getLogName(), request.getLevel());
        return ResponseEntity.ok(ApiResponse.success("로그 레벨이 변경되었습니다", null));
    }

    /**
     * 특정 로거의 Additivity를 변경합니다.
     *
     * @param request 로거 이름 + Additivity (Y/N)
     */
    @PatchMapping("/additivity")
    @PreAuthorize("hasAuthority('LOG_LEVEL:W')")
    public ResponseEntity<ApiResponse<Void>> updateAdditivity(@Valid @RequestBody AdditivityUpdateRequest request) {
        log.info(
                "PATCH /api/log-level/additivity - logName={}, additivity={}",
                request.getLogName(),
                request.getAdditivity());
        logLevelService.updateAdditivity(request);
        // Admin 자신의 Logback 변경 후 spider-link에도 동일하게 전파 (실패해도 200 반환)
        spiderLogLevelClient.syncAdditivity(request.getLogName(), request.getAdditivity());
        return ResponseEntity.ok(ApiResponse.success("Additivity가 변경되었습니다", null));
    }
}
