package com.example.mockcore.handler;

import com.example.bizcommon.BizCommands;
import com.example.mockcore.infra.FixedMessageReader;
import com.example.mockcore.infra.FixedMessageWriter;
import com.example.mockcore.infra.LegacyCoreHandler;
import com.example.mockcore.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 이용대금명세서 조회 커맨드 핸들러 ({@code CORE_PAYMENT_STMT}).
 *
 * <p>REQ: COMMAND(C,20) + REQUEST_ID(C,36) + userId(C,20) + yearMonth(C,6) + paymentDay(C,2)
 * RES: SUCCESS(C,1) + ERROR_MSG(K,200) + dueDate(C,8) + totalAmount(N,15)
 *      + paymentBank(K,40) + paymentAccount(C,20) + paymentDay(C,2) + itemCnt(N,4)
 *      + [반복: cardNo(C,20)+cardName(C,20)+amount(N,15)+itemDueDate(C,8)]</p>
 *
 * <p>루프 내 itemDueDate 는 헤더의 dueDate 와 동일한 값이지만, FWK_MESSAGE_FIELD PK 충돌
 * 방지를 위해 별도 필드명을 사용한다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorePaymentStmtHandler implements LegacyCoreHandler {

    private final AccountRepository accountRepository;

    @Override
    public String getCommand() {
        return BizCommands.CORE_PAYMENT_STMT;
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] handle(byte[] requestBytes) {
        FixedMessageReader reader = new FixedMessageReader(requestBytes);
        reader.skip(20); // COMMAND
        reader.skip(36); // REQUEST_ID
        String userId     = reader.readC(20);
        String yearMonth  = emptyToNull(reader.readC(6));
        String paymentDay = emptyToNull(reader.readC(2));

        log.debug("[CORE_PAYMENT_STMT] 명세서 조회 — userId={}, yearMonth={}, paymentDay={}",
                userId, yearMonth, paymentDay);

        FixedMessageWriter writer = new FixedMessageWriter();
        try {
            Map<String, Object> result = accountRepository.findPaymentStatement(userId, yearMonth, paymentDay);

            String dueDate     = str(result, "dueDate");
            long totalAmount   = toLong(result.get("totalAmount"));
            Map<String, Object> cardInfo = (Map<String, Object>) result.getOrDefault("cardInfo", Map.of());
            List<Map<String, Object>> items = (List<Map<String, Object>>) result.getOrDefault("items", List.of());

            log.debug("[CORE_PAYMENT_STMT] 조회 성공 — userId={}, totalAmount={}, itemCnt={}",
                    userId, totalAmount, items.size());
            writer.writeC("Y", 1);
            writer.writeK("", 200);
            writer.writeC(dueDate, 8);
            writer.writeN(totalAmount, 15);
            writer.writeK(str(cardInfo, "paymentBank"), 40);
            writer.writeC(str(cardInfo, "paymentAccount"), 20);
            writer.writeC(str(cardInfo, "paymentDay"), 2);
            writer.writeN(items.size(), 4);
            for (Map<String, Object> item : items) {
                writer.writeC(str(item, "cardNo"), 20);
                writer.writeC(str(item, "cardName"), 20);
                writer.writeN(toLong(item.get("amount")), 15);
                writer.writeC(str(item, "dueDate"), 8); // itemDueDate
            }
        } catch (Exception e) {
            log.warn("[CORE_PAYMENT_STMT] 조회 실패 — {}", e.getMessage());
            writer = new FixedMessageWriter();
            writer.writeC("N", 1);
            writer.writeK(e.getMessage(), 200);
            writer.writeC("", 8);
            writer.writeN(0L, 15);
            writer.writeK("", 40);
            writer.writeC("", 20);
            writer.writeC("", 2);
            writer.writeN(0, 4);
        }
        return writer.toBytes();
    }

    private String emptyToNull(String val) {
        return (val == null || val.isBlank()) ? null : val;
    }

    private long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        if (val != null) {
            try { return Long.parseLong(val.toString()); } catch (NumberFormatException ignored) {}
        }
        return 0L;
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : "";
    }
}
