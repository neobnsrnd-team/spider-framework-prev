package com.example.spiderbatch.job.db2foreign;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DB2Foreign 패턴 시연을 위한 Mock 외부 시스템 엔드포인트.
 *
 * <p>실제 운영에서는 외부 금융기관 API(예: 카드 결제 전문 연계)에 해당한다.
 * POC에서는 동일 WAS 내 Mock 컨트롤러로 대체하여 외부 서버 없이 시연한다.</p>
 *
 * <p>Db2ForeignJobConfig의 TransferItemWriter가 이 엔드포인트를 호출한다.</p>
 */
@Slf4j
@RestController
@RequestMapping("/mock/external")
public class MockExternalController {

    /**
     * 외부 전문 연계 Mock 엔드포인트.
     *
     * @param body userId, cardNo, usageDt, merchant, amount, approvalYn, paymentStatusCode 포함 Map
     * @return 처리 결과 (success: true, message: ...)
     */
    @PostMapping("/transfer")
    public Map<String, Object> transfer(@RequestBody Map<String, Object> body) {
        log.info("[MOCK] 카드사용내역 전문 수신: userId={}, cardNo={}, amount={}",
                body.get("userId"), body.get("cardNo"), body.get("amount"));

        // 실제 외부 시스템 연동 시뮬레이션: 항상 성공 응답
        return Map.of(
                "success", true,
                "message", "전문 처리 완료",
                "userId", body.getOrDefault("userId", "unknown"),
                "cardNo", body.getOrDefault("cardNo", "unknown")
        );
    }
}
