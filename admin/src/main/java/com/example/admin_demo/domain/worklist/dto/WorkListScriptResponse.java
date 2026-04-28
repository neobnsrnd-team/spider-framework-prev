package com.example.admin_demo.domain.worklist.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 이행스크립트 생성·조회 응답 DTO. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkListScriptResponse {
    /** 저장된 파일명 (.sql). */
    private String fileName;
    /** 스크립트 파일 내용 (SQL 텍스트). */
    private String content;
}
