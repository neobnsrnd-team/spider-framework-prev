package com.example.spideradmin.domain.reactcmsadmindeployment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * React CMS Admin 배포 이력 응답 DTO
 *
 * <p>FWK_CMS_FILE_SEND_HIS JOIN FWK_CMS_SERVER_INSTANCE 기반
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactCmsAdminDeployHistoryResponse {

    /** 서버 인스턴스 ID */
    private String instanceId;

    /** 서버 인스턴스명 */
    private String instanceName;

    /** 서버 IP */
    private String instanceIp;

    /** 서버 포트 */
    private String instancePort;

    /** 파일 ID ({pageId}_v{version}.html) */
    private String fileId;

    /** 파일 크기 (bytes) */
    private Long fileSize;

    /** CRC 값 (SHA-256 앞 16자리) */
    private String fileCrcValue;

    /** 배포 실행자 ID */
    private String lastModifierId;

    /** 배포 일시 (YYYYMMDDHH24MISS) */
    private String lastModifiedDtime;
}
