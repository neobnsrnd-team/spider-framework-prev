package com.example.spider_admin.domain.transaction.mapper;

import com.example.spider_admin.domain.transaction.dto.TrxStopHistorySearchRequest;
import com.example.spider_admin.domain.transaction.dto.TrxStopHistoryWithTrxNameResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 거래중지이력 Command Mapper (INSERT 전용)
 * <p>거래중지/시작 시 이력을 FWK_TRX_STOP_HISTORY에 기록합니다.
 * <p>조회는 monitor 도메인의 TrxStopHistoryMapper가 담당합니다.
 */
@Mapper
public interface TrxStopHistoryMapper {

    /**
     * 거래중지이력 단건 삽입
     *
     * @param gubunType 구분유형 (T: 거래, S: 서비스)
     * @param trxId 거래ID
     * @param trxStopUpdateDtime 거래중지시간
     * @param trxStopReason 거래중지사유
     * @param trxStopYn 거래중지여부
     * @param lastUpdateUserId 최종수정자
     * @return 삽입된 행 수
     */
    int insert(
            @Param("gubunType") String gubunType,
            @Param("trxId") String trxId,
            @Param("trxStopUpdateDtime") String trxStopUpdateDtime,
            @Param("trxStopReason") String trxStopReason,
            @Param("trxStopYn") String trxStopYn,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 거래중지이력 배치 삽입
     *
     * @param list 거래중지이력 파라미터 목록
     * @return 삽입된 행 수
     */
    int insertBatch(@Param("list") List<java.util.Map<String, String>> list);

    /**
     * 거래중지이력 검색 (네이티브 ROWNUM 페이징)
     * FWK_TRX와 LEFT JOIN하여 거래명 포함
     *
     * @param searchDTO     검색 조건
     * @param sortBy        정렬 기준 필드
     * @param sortDirection 정렬 방향 (ASC, DESC)
     * @param offset        ROWNUM 시작 offset (0-based)
     * @param endRow        ROWNUM 끝 행 번호
     * @return 거래중지이력 목록 (거래명 포함)
     */
    List<TrxStopHistoryWithTrxNameResponse> searchHistories(
            @Param("searchDTO") TrxStopHistorySearchRequest searchDTO,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 거래중지이력 검색 건수
     *
     * @param searchDTO 검색 조건
     * @return 검색된 건수
     */
    long countSearchHistories(@Param("searchDTO") TrxStopHistorySearchRequest searchDTO);

    /**
     * 특정 거래ID의 거래중지이력 조회
     *
     * @param trxId 거래ID
     * @return 거래중지이력 목록 (거래명 포함)
     */
    List<TrxStopHistoryWithTrxNameResponse> findByTrxId(@Param("trxId") String trxId);

    /**
     * 엑셀 내보내기용 전체 거래중지이력 조회 (페이징 없음)
     *
     * @param searchDTO 검색 조건
     * @return 거래중지이력 목록 (거래명 포함)
     */
    List<TrxStopHistoryWithTrxNameResponse> findAllForExport(@Param("searchDTO") TrxStopHistorySearchRequest searchDTO);
}
