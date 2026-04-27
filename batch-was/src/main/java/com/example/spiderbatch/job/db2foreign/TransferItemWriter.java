package com.example.spiderbatch.job.db2foreign;

import com.example.spiderbatch.job.common.CardUsage;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * 외부 시스템 HTTP 연계 ItemWriter.
 *
 * <p>POC_카드사용내역에서 읽은 CardUsage를 외부 시스템(Mock) HTTP 엔드포인트로 전송한다.
 * Chunk 단위로 처리되므로 한 Chunk 내 오류 발생 시 해당 Chunk 전체가 롤백된다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class TransferItemWriter implements ItemWriter<CardUsage> {

    private final RestTemplate restTemplate;

    /** 외부 시스템 전문 연계 URL (Mock 엔드포인트) */
    private final String transferUrl;

    /**
     * Chunk 내 각 CardUsage를 외부 URL로 POST 전송.
     * 응답이 2xx가 아니면 예외를 던져 해당 Chunk를 롤백시킨다.
     */
    @Override
    public void write(Chunk<? extends CardUsage> chunk) {
        for (CardUsage usage : chunk) {
            Map<String, Object> body = new HashMap<>();
            body.put("userId", usage.getUserId());
            body.put("cardNo", usage.getCardNo());
            body.put("usageDt", usage.getUsageDt());
            body.put("merchant", usage.getMerchant());
            body.put("amount", usage.getAmount());
            body.put("approvalYn", usage.getApprovalYn());
            body.put("paymentStatusCode", usage.getPaymentStatusCode());

            log.debug("외부 전문 전송: userId={}, cardNo={}, url={}",
                    usage.getUserId(), usage.getCardNo(), transferUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(transferUrl, entity, Map.class);
                // RestTemplate은 비-2xx 응답에서 HttpStatusCodeException을 throw하므로
                // 이 분기는 안전망 역할(커스텀 ErrorHandler 사용 시 도달 가능)
                if (!response.getStatusCode().is2xxSuccessful()) {
                    throw new ExternalTransferException(
                            "외부 전문 연계 비정상 응답: userId=" + usage.getUserId()
                            + ", cardNo=" + usage.getCardNo()
                            + ", status=" + response.getStatusCode());
                }
            } catch (ExternalTransferException e) {
                throw e;
            } catch (Exception e) {
                // 네트워크 오류·비-2xx HttpStatusCodeException 모두 ExternalTransferException으로 통일
                // → Db2ForeignJob Step의 skip 대상으로만 처리
                throw new ExternalTransferException(
                        "외부 전문 연계 실패: userId=" + usage.getUserId()
                        + ", cardNo=" + usage.getCardNo()
                        + " — " + e.getMessage(), e);
            }
        }
        log.info("외부 전문 전송 완료: {}건", chunk.size());
    }
}
