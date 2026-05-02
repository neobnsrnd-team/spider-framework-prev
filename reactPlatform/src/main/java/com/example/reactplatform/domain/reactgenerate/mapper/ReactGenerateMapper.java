package com.example.reactplatform.domain.reactgenerate.mapper;

import com.example.reactplatform.domain.reactgenerate.dto.ReactApprovalResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateEntity;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateHistoryResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateSearchRequest;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * FWK_REACT_CODE_HIS 테이블 Mapper.
 * React 코드 생성 이력의 저장·조회·상태 변경을 담당한다.
 *
 * <p>Oracle {@code @MapperScan}("domain.**.mapper") 스캔 대상으로 등록되어
 * Oracle datasource를 통해 FWK_REACT_CODE_HIS 테이블에 접근한다.
 */
public interface ReactGenerateMapper {

    /**
     * 생성된 React 코드 이력을 신규 저장한다.
     *
     * @param entity 삽입할 이력 데이터 (codeId, figmaUrl, domain, brand, componentName 등 포함)
     */
    void insert(@Param("e") ReactGenerateEntity entity);

    /** CODE_ID로 생성 이력을 단건 조회한다. 존재하지 않으면 null 반환. */
    ReactGenerateResponse selectById(@Param("codeId") String codeId);

    /** 승인 상태를 변경한다. 승인·반려 시 approvalUserId/approvalDtime 함께 기록. 반려 사유는 failReason에 저장. */
    void updateStatus(
            @Param("codeId") String codeId,
            @Param("status") String status,
            @Param("approvalUserId") String approvalUserId,
            @Param("approvalDtime") String approvalDtime,
            @Param("failReason") String failReason);

    /**
     * 현재 STATUS가 requiredStatus인 경우에만 상태를 변경하고 변경된 행 수를 반환한다.
     * Race Condition 방지를 위해 approve 처리 시 사용한다.
     * 반환값이 0이면 다른 요청이 이미 상태를 변경했음을 의미한다.
     */
    int updateStatusConditional(
            @Param("codeId") String codeId,
            @Param("status") String status,
            @Param("approvalUserId") String approvalUserId,
            @Param("approvalDtime") String approvalDtime,
            @Param("failReason") String failReason,
            @Param("requiredStatus") String requiredStatus);

    /** 렌더링 실패 또는 코드 생성 실패 시 STATUS를 FAILED로, FAIL_REASON을 기록한다. */
    void updateToFailed(@Param("codeId") String codeId, @Param("failReason") String failReason);

    /**
     * 검색 조건에 맞는 이력 목록을 생성일시 내림차순으로 조회한다.
     * CLOB 컬럼(REACT_CODE 등)은 제외하고 목록 표시에 필요한 컬럼만 반환한다.
     */
    List<ReactGenerateHistoryResponse> selectList(@Param("req") ReactGenerateSearchRequest req);

    /** 검색 조건에 맞는 전체 건수를 반환한다 (페이지네이션용). */
    int selectCount(@Param("req") ReactGenerateSearchRequest req);

    /** 검색 조건에 맞는 전체 목록을 페이지네이션 없이 조회한다 (엑셀 내보내기용). */
    List<ReactGenerateHistoryResponse> selectAllForExport(@Param("req") ReactGenerateSearchRequest req);

    /**
     * 승인 대기(PENDING_APPROVAL) 목록을 생성일시 오름차순으로 조회한다 (승인 관리 메뉴 전용).
     *
     * @param offset 시작 오프셋 (0-based)
     * @param endRow 마지막 행 번호 (inclusive)
     * @return 승인 대기 목록
     */
    List<ReactApprovalResponse> selectPendingList(@Param("offset") int offset, @Param("endRow") int endRow);

    /** 승인 대기(PENDING_APPROVAL) 전체 건수를 반환한다 (페이지네이션용). */
    int selectPendingCount();

    /**
     * 승인 이력(APPROVED / REJECTED) 목록을 처리일시 내림차순으로 조회한다.
     *
     * @param offset         시작 오프셋 (0-based)
     * @param endRow         마지막 행 번호 (inclusive)
     * @param status         상태 필터 (null/빈 문자열이면 APPROVED/REJECTED 전체)
     * @param title          화면 제목 부분 일치 (null/빈 문자열이면 미적용)
     * @param componentName  컴포넌트명 부분 일치 (null/빈 문자열이면 미적용)
     * @param approvalUserId 처리자 ID 부분 일치 (null/빈 문자열이면 미적용)
     * @param createUserId   요청자 ID 부분 일치 (null/빈 문자열이면 미적용)
     * @param fromDate       처리일시 시작 (yyyyMMdd, null/빈 문자열이면 미적용)
     * @param toDate         처리일시 종료 (yyyyMMdd, null/빈 문자열이면 미적용)
     * @return 승인 이력 목록
     */
    List<ReactGenerateHistoryResponse> selectApprovalHistory(
            @Param("offset") int offset,
            @Param("endRow") int endRow,
            @Param("status") String status,
            @Param("title") String title,
            @Param("componentName") String componentName,
            @Param("approvalUserId") String approvalUserId,
            @Param("createUserId") String createUserId,
            @Param("fromDate") String fromDate,
            @Param("toDate") String toDate);

    /** 승인 이력 전체 건수를 반환한다 (페이지네이션용). */
    int selectApprovalHistoryCount(
            @Param("status") String status,
            @Param("title") String title,
            @Param("componentName") String componentName,
            @Param("approvalUserId") String approvalUserId,
            @Param("createUserId") String createUserId,
            @Param("fromDate") String fromDate,
            @Param("toDate") String toDate);
}
