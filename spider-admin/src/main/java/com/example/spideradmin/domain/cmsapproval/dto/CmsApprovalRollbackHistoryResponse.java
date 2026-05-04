package com.example.spideradmin.domain.cmsapproval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 롤백용 이력 스냅샷 조회 결과
 *
 * <p>resultType=map 으로 받으면 PAGE_HTML(CLOB)이 java.sql.Clob 으로 반환되어
 * String 캐스트 시 ClassCastException 이 발생한다.
 * 전용 DTO 를 사용하면 MyBatis 가 CLOB → String 자동 변환을 수행한다.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CmsApprovalRollbackHistoryResponse {

    /** 복원할 페이지 HTML (CLOB) */
    private String pageHtml;

    /** 복원할 파일 경로 */
    private String filePath;
}
