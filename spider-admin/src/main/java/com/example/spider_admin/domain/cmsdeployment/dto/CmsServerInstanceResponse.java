package com.example.spider_admin.domain.cmsdeployment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * CMS 배포 서버 인스턴스 응답 DTO (FWK_CMS_SERVER_INSTANCE, ALIVE_YN='Y' 조회 결과)
 *
 * <p>만료수동처리 배포 시 순차 전송 대상 서버 목록을 담는다.
 * URL 조합: {@code http://{instanceIp}:{instancePort}/cms/api/deploy/receive}</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsServerInstanceResponse {

    /** 서버 인스턴스 ID */
    private String instanceId;

    /** 서버 IP */
    private String instanceIp;

    /** 서버 포트 */
    private String instancePort;
}
