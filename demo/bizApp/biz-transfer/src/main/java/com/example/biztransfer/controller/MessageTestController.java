package com.example.biztransfer.controller;

import com.example.spidercommon.infra.tcp.handler.CommandDispatcher;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin 전문 테스트 화면과의 HTTP 연동 컨트롤러.
 *
 * <p>Admin은 HTTP POST + form-data로 요청을 전송한다.
 * 이 컨트롤러는 form 파라미터를 JsonCommandRequest로 변환 후 CommandDispatcher에 위임하고,
 * 응답을 key=value 텍스트 포맷으로 반환한다.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ibsmgr")
public class MessageTestController {

    private final CommandDispatcher<JsonCommandRequest, JsonCommandResponse> commandDispatcher;

    /** Admin 전문 테스트에서 제외할 시스템 파라미터 키 목록 */
    private static final java.util.Set<String> SYSTEM_KEYS = java.util.Set.of(
            "orgId", "_$TRX_ID", "wasInstanceList", "URI", "targetMethod",
            "xmlYn", "strLoop", "LOG_FILE_CATEGORY", "testName", "testDesc", "fieldNameArray"
    );

    @PostMapping(
            value = "/spider.admin.ap.message.test.ConnectorEmulatorA.web",
            produces = MediaType.TEXT_PLAIN_VALUE)
    public String simulate(@RequestParam Map<String, String> params) {
        String command = params.get("_$TRX_ID");
        String requestId = params.getOrDefault("REQUEST_ID", UUID.randomUUID().toString());

        log.info("[MessageTestController] 수신: command={}, requestId={}", command, requestId);

        // 시스템 파라미터 제거 후 payload 구성
        Map<String, Object> payload = new HashMap<>();
        params.forEach((k, v) -> {
            if (!SYSTEM_KEYS.contains(k)) {
                payload.put(k, v);
            }
        });

        JsonCommandRequest request = JsonCommandRequest.builder()
                .command(command)
                .requestId(requestId)
                .payload(payload)
                .build();

        try {
            JsonCommandResponse response = commandDispatcher.dispatch(request);
            return buildKeyValueResponse(response);
        } catch (Exception e) {
            log.error("[MessageTestController] 처리 실패: command={}, error={}", command, e.getMessage());
            return "success=false\nerror=" + e.getMessage() + "\n";
        }
    }

    /** JsonCommandResponse를 key=value 텍스트 포맷으로 변환 */
    private String buildKeyValueResponse(JsonCommandResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("success=").append(response.isSuccess()).append("\n");
        if (response.getMessage() != null) {
            sb.append("message=").append(response.getMessage()).append("\n");
        }
        if (response.getError() != null) {
            sb.append("error=").append(response.getError()).append("\n");
        }
        if (response.getPayload() != null) {
            response.getPayload().forEach((k, v) ->
                    sb.append(k).append("=").append(v).append("\n"));
        }
        return sb.toString();
    }
}
