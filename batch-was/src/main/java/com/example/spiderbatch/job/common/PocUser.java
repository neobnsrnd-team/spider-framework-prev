package com.example.spiderbatch.job.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * POC_USER 데이터 모델.
 * file2db Job(CSV → POC_USER 일괄 적재)에서 사용.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PocUser {

    /** 사용자 ID (PK) */
    private String userId;

    /** 사용자명 */
    private String userName;

    /** 비밀번호 */
    private String password;

    /** 사용자 등급 */
    private String userGrade;

    /** 로그 기록 여부 (Y/N) */
    private String logYn;

    /** 최종 로그인 일시 (YYYYMMDDHH24MISS) */
    private String lastLoginDtime;
}
