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
 * 카드 목록 조회 커맨드 핸들러 ({@code CORE_CARD_LIST}).
 *
 * <p>REQ: COMMAND(C,20) + REQUEST_ID(C,36) + userId(C,20)
 * RES: SUCCESS(C,1) + ERROR_MSG(K,200) + cardCnt(N,4)
 *      + [반복: id(C,20)+name(K,60)+maskedNumber(C,20)+brand(C,15)+balance(N,15)
 *              +expiry(C,10)+paymentBank(K,40)+paymentAccount(C,20)+paymentDay(C,2)
 *              +limitAmount(N,15)+usedAmount(N,15)]</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CoreCardListHandler implements LegacyCoreHandler {

    private final AccountRepository accountRepository;

    @Override
    public String getCommand() {
        return BizCommands.CORE_CARD_LIST;
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] handle(byte[] requestBytes) {
        FixedMessageReader reader = new FixedMessageReader(requestBytes);
        reader.skip(20); // COMMAND
        reader.skip(36); // REQUEST_ID
        String userId = reader.readC(20);

        log.debug("[CORE_CARD_LIST] 카드 목록 조회 — userId={}", userId);

        FixedMessageWriter writer = new FixedMessageWriter();
        try {
            List<Map<String, Object>> cards = accountRepository.findCardsByUserId(userId);
            log.debug("[CORE_CARD_LIST] 조회 성공 — userId={}, 카드수={}", userId, cards.size());
            writer.writeC("Y", 1);
            writer.writeK("", 200);
            writer.writeN(cards.size(), 4);
            for (Map<String, Object> card : cards) {
                writer.writeC(str(card, "id"), 20);
                writer.writeK(str(card, "name"), 60);
                writer.writeC(str(card, "maskedNumber"), 20);
                writer.writeC(str(card, "brand"), 15);
                writer.writeN(toLong(card.get("balance")), 15);
                writer.writeC(str(card, "expiry"), 10);
                writer.writeK(str(card, "paymentBank"), 40);
                writer.writeC(str(card, "paymentAccount"), 20);
                writer.writeC(str(card, "paymentDay"), 2);
                writer.writeN(toLong(card.get("limitAmount")), 15);
                writer.writeN(toLong(card.get("usedAmount")), 15);
            }
        } catch (Exception e) {
            log.warn("[CORE_CARD_LIST] 조회 실패 — {}", e.getMessage());
            writer = new FixedMessageWriter();
            writer.writeC("N", 1);
            writer.writeK(e.getMessage(), 200);
            writer.writeN(0, 4);
        }
        return writer.toBytes();
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
