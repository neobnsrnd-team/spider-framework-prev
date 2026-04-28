package com.example.spiderlink.infra.tcp.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin ↔ batch-was 간 TCP 통신 메시지 모델.
 *
 * <p>Java ObjectStream 바이너리 직렬화 시 양쪽이 동일한 클래스(패키지 포함)를 가져야 역직렬화가 성공한다.
 * spider-link 라이브러리로 이전하여 Admin, spider-batch 모두 이 공통 클래스를 참조한다.</p>
 *
 * <p>serialVersionUID를 명시하여 양쪽 클래스 호환성을 보장한다.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagementContext implements Serializable, HasCommand {

    private static final long serialVersionUID = 1L;

    /** 대상 WAS 인스턴스 ID (FWK_WAS_INSTANCE.INSTANCE_ID) */
    private String instanceId;

    /** 실행 커맨드 (BATCH_EXEC, PING 등) */
    private String command;

    /** 배치 APP ID */
    private String batchAppId;

    /** 배치 기준일 (YYYYMMDD) */
    private String batchDate;

    /** 실행 요청 사용자 ID */
    private String userId;

    /** 배치 파라미터 (key=value;key2=value2 형식, 선택) */
    private String parameters;

    /** 실행 결과 코드 (SUCCESS, ABNORMAL_TERMINATION 등) */
    private String resultCode;

    /** 배치 실행 시퀀스 (응답 시 채워짐) */
    private Integer executeSeq;

    /** 오류 메시지 (실패 시 채워짐, 예외 클래스명 + 메시지 포맷 권장) */
    private String errorMessage;

    /** Cron 표현식 (SCHEDULE_CRON_UPDATE 커맨드 시 새 스케줄 값) */
    private String cronText;
}
