package com.example.spider_admin.domain.transaction.mapper;

import com.example.spider_admin.domain.transaction.dto.TrxCreateRequest;
import com.example.spider_admin.domain.transaction.dto.TrxDetailResponse;
import com.example.spider_admin.domain.transaction.dto.TrxListResponse;
import com.example.spider_admin.domain.transaction.dto.TrxResponse;
import com.example.spider_admin.domain.transaction.dto.TrxSimpleResponse;
import com.example.spider_admin.domain.transaction.dto.TrxStopListResponse;
import com.example.spider_admin.domain.transaction.dto.TrxUpdateRequest;
import com.example.spider_admin.domain.trxmessage.dto.TrxMessageResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 트랜잭션 Command Mapper (CRUD)
 * - insert / update / delete / 단건 조회 (Command 중심)
 * - 목록 조회, 검색 등 Query 전용 SQL은 TrxMapper에서 처리
 */
public interface TrxMapper {

    // 단건 조회 (Response DTO 직접 반환)
    TrxResponse selectResponseById(String trxId);

    // 복수 조회 (배치 처리용 - trxId, trxStopYn만 필요)
    List<TrxSimpleResponse> selectSimpleByIds(@Param("trxIds") List<String> trxIds);

    // 생성
    void insertTrx(@Param("dto") TrxCreateRequest dto);

    // 수정
    int updateTrx(@Param("trxId") String trxId, @Param("dto") TrxUpdateRequest dto);

    // 거래중지 상태 일괄 변경 (배치)
    void batchUpdateTrxStop(
            @Param("trxIds") List<String> trxIds,
            @Param("trxStopYn") String trxStopYn,
            @Param("trxStopReason") String trxStopReason);

    // 운영모드 변경 전용 (단건)
    void updateOperMode(@Param("trxId") String trxId, @Param("operModeType") String operModeType);

    // 운영모드 일괄 변경 (전체)
    void updateAllOperMode(@Param("operModeType") String operModeType);

    // 삭제
    void deleteTrxById(String trxId);

    // 존재 확인 (중복 체크용)
    int countByTrxId(String trxId);

    // ==================== 목록 조회 ====================

    /**
     * 전체 트랜잭션 목록 조회 (select box용)
     */
    List<TrxSimpleResponse> findAllSimple();

    /**
     * 검색 조건으로 트랜잭션 목록 조회 (전문정보 포함)
     * FWKI0060 거래관리 화면용
     */
    List<TrxListResponse> findAllWithSearchAndMessages(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("trxStopYnFilter") String trxStopYnFilter,
            @Param("orgIdFilter") String orgIdFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 검색 조건에 해당하는 거래 수 (전문정보 포함 조건)
     */
    long countAllWithSearchAndMessages(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("trxStopYnFilter") String trxStopYnFilter,
            @Param("orgIdFilter") String orgIdFilter);

    // ==================== 집계 ====================

    /**
     * 전체 트랜잭션 수
     */
    long countAll();

    // ==================== 상세 조회 ====================

    /**
     * 거래 상세 조회 (기본 정보)
     * @param trxId 거래 ID
     * @return 거래 상세 정보
     */
    TrxDetailResponse findTrxDetailById(@Param("trxId") String trxId);

    /**
     * 거래에 연결된 전문 목록 조회
     * @param trxId 거래 ID
     * @return 전문 목록
     */
    List<TrxMessageResponse> findMessagesByTrxId(@Param("trxId") String trxId);

    // ==================== 거래중지 화면용 조회 ====================

    /**
     * FWK_TRX에서 사용 중인 BIZ_GROUP_ID 목록 (중복 제거, 정렬)
     */
    List<String> findDistinctBizGroups();

    /**
     * 거래중지 페이지용 목록 조회 (접근허용자 수 포함)
     */
    List<TrxStopListResponse> findAllForTrxStop(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("operModeTypeFilter") String operModeTypeFilter,
            @Param("trxStopYnFilter") String trxStopYnFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 거래관리 엑셀 내보내기용 전체 조회 (페이징 없음)
     */
    List<TrxListResponse> findAllWithSearchAndMessagesForExport(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("trxStopYnFilter") String trxStopYnFilter,
            @Param("orgIdFilter") String orgIdFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    /**
     * 거래중지 엑셀 내보내기용 전체 조회 (페이징 없음)
     */
    List<TrxStopListResponse> findAllForTrxStopExport(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("operModeTypeFilter") String operModeTypeFilter,
            @Param("trxStopYnFilter") String trxStopYnFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    /**
     * 거래중지 페이지용 건수 조회
     */
    long countAllForTrxStop(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("operModeTypeFilter") String operModeTypeFilter,
            @Param("trxStopYnFilter") String trxStopYnFilter);

    /**
     * 거래 목록 전체 조회 (엑셀 내보내기용, 페이징 없음)
     */
    List<TrxListResponse> findAllForExport();

    /**
     * 거래중지 목록 전체 조회 (엑셀 내보내기용, 페이징 없음)
     */
    List<TrxStopListResponse> findAllTrxStopForExport();
}
