package com.example.spidercommon.domain.management;

import com.example.spidercommon.domain.management.executor.ManagementExecutor;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TCP 관리 명령 핸들러.
 *
 * <p>DGB {@code ManagementAgent.doProcess(gubun)} 구조에 대응하는 TCP 수신 핸들러다.
 * {@code command = "MANAGEMENT_RELOAD"} 요청을 수신하고,
 * payload의 {@code gubun} 값으로 {@link ManagementExecutor} 목록에서 적합한 실행기를 선택한다.</p>
 *
 * <p>spider-common Auto-Configuration으로 자동 등록되므로
 * spider-common을 의존하는 모든 TCP WAS에 자동으로 적용된다.</p>
 *
 * <pre>{@code
 * // Admin → TCP WAS 전송 예시
 * {
 *   "command": "MANAGEMENT_RELOAD",
 *   "payload": {
 *     "gubun": "log_config_level",
 *     "logName": "com.example.service",
 *     "level": "DEBUG"
 *   }
 * }
 * }</pre>
 */
@Slf4j
@RequiredArgsConstructor
public class ManagementReloadCommandHandler
        implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    /** TCP 커맨드 이름 상수 */
    public static final String COMMAND = "MANAGEMENT_RELOAD";

    private final List<ManagementExecutor> executors;

    @Override
    public boolean supports(String command) {
        return COMMAND.equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        Map<String, Object> params = request.getPayload();
        if (params == null) {
            return errorResponse(command, "payload가 없습니다");
        }

        String gubun = (String) params.get("gubun");
        if (gubun == null || gubun.isBlank()) {
            return errorResponse(command, "gubun은 필수입니다");
        }

        log.info("[ManagementReloadCommandHandler] 관리 명령 수신: gubun={}", gubun);

        ManagementExecutor executor = executors.stream()
                .filter(e -> e.supports(gubun))
                .findFirst()
                .orElse(null);

        if (executor == null) {
            log.warn("[ManagementReloadCommandHandler] 지원하지 않는 gubun: {}", gubun);
            return errorResponse(command, "지원하지 않는 gubun: " + gubun);
        }

        try {
            Map<String, Object> result = executor.execute(params);
            log.info("[ManagementReloadCommandHandler] 관리 명령 완료: gubun={}", gubun);
            return JsonCommandResponse.builder()
                    .command(command)
                    .success(true)
                    .message(gubun + " 처리 완료")
                    .payload(result)
                    .build();
        } catch (IllegalArgumentException e) {
            return errorResponse(command, e.getMessage());
        } catch (Exception e) {
            log.error("[ManagementReloadCommandHandler] 관리 명령 오류: gubun={}, error={}", gubun, e.getMessage(), e);
            return errorResponse(command, e.getMessage());
        }
    }

    private JsonCommandResponse errorResponse(String command, String message) {
        return JsonCommandResponse.builder()
                .command(command)
                .success(false)
                .error(message)
                .build();
    }
}
