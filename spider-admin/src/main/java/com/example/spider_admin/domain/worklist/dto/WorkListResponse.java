package com.example.spider_admin.domain.worklist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 작업함 목록 단건 응답 DTO. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkListResponse {
    private Integer workSeq;
    /** 항목 한글 레이블 (전문, 거래, SQL쿼리관리 등). */
    private String workId;
    /** 원본 WORK_ID 코드 (Message, Trx, SQL_QUERY 등). */
    private String workOriId;
    /** 식별자 — 해당 관리 항목의 PK. */
    private String workDataPk;
    /** 작업 내용 설명. */
    private String workName;
    /** CRUD 유형 한글 (생성/수정/삭제). */
    private String crudType;
    /** 결재일련번호 (결재 미신청이면 '결재미신청'). */
    private String approvalSeq;

    private String lastUpdateUserId;
    /** 운영반영여부 (Y/N). */
    private String distYn;
    /** 운영반영일시. */
    private String distDtime;

    private String groupId;
    /** 이행스크립트 파일명. */
    private String fileName;
    /** 삭제 가능 여부 — 결재신청 전(N)에만 삭제 가능. */
    private String deleteYn;

    private String lastUpdateDtime;
}
