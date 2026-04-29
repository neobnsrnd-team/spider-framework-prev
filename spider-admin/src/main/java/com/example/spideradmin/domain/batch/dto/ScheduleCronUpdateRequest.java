package com.example.spideradmin.domain.batch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 스케줄 Cron 표현식 변경 요청 DTO.
 * Admin에서 배치의 Cron 스케줄을 변경할 때 사용한다.
 * DB(FWK_BATCH_APP.CRON_TEXT) 업데이트 후 모든 배정 WAS 인스턴스에 TCP 커맨드가 전송된다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleCronUpdateRequest {

    /**
     * Quartz Cron 표현식 (예: {@code 0 0 2 * * ?} — 매일 02:00 실행).
     * 공백을 포함한 6필드 또는 7필드 형식을 지원한다.
     */
    @NotBlank(message = "Cron 표현식은 필수입니다")
    private String cronText;
}
