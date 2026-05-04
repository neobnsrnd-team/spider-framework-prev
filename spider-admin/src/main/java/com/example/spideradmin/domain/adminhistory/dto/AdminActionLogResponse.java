package com.example.spideradmin.domain.adminhistory.dto;

import lombok.*;

/**
 * 관리자 작업이력 로그 응답 DTO (FWK_USER_ACCESS_HIS 기반)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminActionLogResponse {

    /** 사용자ID (USER_ID) */
    private String userId;

    /** 접근 일시 (ACCESS_DTIME, yyyyMMddHHmmss) */
    private String accessDtime;

    /** 접근 IP (ACCESS_IP) */
    private String accessIp;

    /** 접근 URL (ACCESS_URL) */
    private String accessUrl;

    /** 입력 데이터 (INPUT_DATA, JSON) */
    private String inputData;

    /** 결과 메세지 (RESULT_MESSAGE) */
    private String resultMessage;
}
