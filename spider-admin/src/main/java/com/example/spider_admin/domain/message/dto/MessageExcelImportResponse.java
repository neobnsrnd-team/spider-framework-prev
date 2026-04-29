package com.example.spider_admin.domain.message.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 전문 엑셀 업로드 결과 DTO
 * POST /api/messages/import 응답에 사용
 */
@Getter
@Builder
public class MessageExcelImportResponse {

    /** 전체 처리 행 수 */
    private int totalRows;

    /** 신규 생성 건수 */
    private int created;

    /** 기존 데이터 수정 건수 */
    private int updated;

    /** 건너뛴 건수 (필수값 누락 등) */
    private int skipped;

    /** 행별 오류 메시지 목록 */
    private List<String> errors;
}
