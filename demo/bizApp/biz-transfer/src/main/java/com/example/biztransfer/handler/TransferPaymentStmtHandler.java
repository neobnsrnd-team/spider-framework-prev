package com.example.biztransfer.handler;

import com.example.bizcommon.BizCommands;
import com.example.biztransfer.service.TransferService;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * TRANSFER_PAYMENT_STMT 커맨드 핸들러.
 *
 * <p>카드 결제예정 명세서 조회 요청을 수신하여 mock-core의 CORE_PAYMENT_STMT로 중계한다.</p>
 *
 * <ul>
 *   <li>인바운드 페이로드: {@code {userId, yearMonth?, paymentDay?}}</li>
 *   <li>응답 페이로드:    {@code {dueDate, totalAmount, items:[...], cardInfo, billingPeriod}}</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransferPaymentStmtHandler implements CommandHandler<JsonCommandRequest, JsonCommandResponse> {

    private final TransferService transferService;

    @Override
    public boolean supports(String command) {
        return BizCommands.TRANSFER_PAYMENT_STMT.equals(command);
    }

    @Override
    public JsonCommandResponse handle(String command, JsonCommandRequest request) {
        log.debug("[TransferPaymentStmtHandler] 요청 수신 — requestId={}", request.getRequestId());
        return transferService.forward(BizCommands.TRANSFER_PAYMENT_STMT, BizCommands.CORE_PAYMENT_STMT, request);
    }
}
