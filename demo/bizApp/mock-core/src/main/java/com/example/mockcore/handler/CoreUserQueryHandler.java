package com.example.mockcore.handler;

import com.example.bizcommon.BizCommands;
import com.example.mockcore.infra.FixedMessageReader;
import com.example.mockcore.infra.FixedMessageWriter;
import com.example.mockcore.infra.LegacyCoreHandler;
import com.example.mockcore.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 사용자 조회 커맨드 핸들러 ({@code CORE_USER_QUERY}).
 *
 * <p>REQ: COMMAND(C,20) + REQUEST_ID(C,36) + userId(C,20)
 * RES: SUCCESS(C,1) + ERROR_MSG(K,200) + userId(C,20) + userName(K,60)
 *      + userGrade(K,6) + lastLoginDtime(C,14)</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoreUserQueryHandler implements LegacyCoreHandler {

    private final AccountRepository accountRepository;

    @Override
    public String getCommand() {
        return BizCommands.CORE_USER_QUERY;
    }

    @Override
    public byte[] handle(byte[] requestBytes) {
        FixedMessageReader reader = new FixedMessageReader(requestBytes);
        reader.skip(20); // COMMAND
        reader.skip(36); // REQUEST_ID
        String userId = reader.readC(20);

        log.debug("[CORE_USER_QUERY] 사용자 조회 요청 — userId={}", userId);

        FixedMessageWriter writer = new FixedMessageWriter();
        try {
            Map<String, Object> info = accountRepository.findUserById(userId);
            log.debug("[CORE_USER_QUERY] 조회 성공 — userId={}", userId);
            writer.writeC("Y", 1);
            writer.writeK("", 200);
            writer.writeC(str(info, "userId"), 20);
            writer.writeK(str(info, "userName"), 60);
            writer.writeK(str(info, "userGrade"), 6);
            writer.writeC(str(info, "lastLoginDtime"), 14);
        } catch (Exception e) {
            log.warn("[CORE_USER_QUERY] 조회 실패 — {}", e.getMessage());
            writer = new FixedMessageWriter();
            writer.writeC("N", 1);
            writer.writeK(e.getMessage(), 200);
            writer.writeC("", 20);
            writer.writeK("", 60);
            writer.writeK("", 6);
            writer.writeC("", 14);
        }
        return writer.toBytes();
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}
