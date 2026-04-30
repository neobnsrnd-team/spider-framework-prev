package com.example.spideradmin.domain.batch.mapper;

import com.example.spideradmin.domain.batch.dto.BatchAppCreateRequest;
import com.example.spideradmin.domain.batch.dto.BatchAppResponse;
import com.example.spideradmin.domain.batch.dto.BatchAppUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 배치앱 Mapper (CRUD + Query)
 */
public interface BatchAppMapper {

    // 단건 조회 (ResponseDTO 직접 반환)
    BatchAppResponse selectResponseById(String batchAppId);

    // 생성
    void insertBatchApp(
            @Param("dto") BatchAppCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    // 수정
    void updateBatchApp(
            @Param("batchAppId") String batchAppId,
            @Param("dto") BatchAppUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    // 삭제
    void deleteBatchAppById(String batchAppId);

    // 존재 확인 (중복 체크용)
    int countByBatchAppId(String batchAppId);

    // ==================== 목록 조회 ====================

    /**
     * 전체 배치앱 목록 조회 (비페이징)
     */
    List<BatchAppResponse> findAll();

    /**
     * 전체 배치앱 목록 조회 (페이징)
     */
    List<BatchAppResponse> findAllPaged(@Param("offset") int offset, @Param("endRow") int endRow);

    /**
     * 전체 배치앱 건수 조회
     */
    long countAll();

    /**
     * 검색 조건으로 배치앱 목록 조회
     */
    List<BatchAppResponse> findAllWithSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("instanceIdFilter") String instanceIdFilter,
            @Param("batchCycleFilter") String batchCycleFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 검색 조건으로 배치앱 건수 조회
     */
    long countAllWithSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("instanceIdFilter") String instanceIdFilter,
            @Param("batchCycleFilter") String batchCycleFilter);

    /**
     * 엑셀 내보내기용 전체 목록 조회 (페이징 없음)
     */
    List<BatchAppResponse> findAllForExport(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("instanceIdFilter") String instanceIdFilter,
            @Param("batchCycleFilter") String batchCycleFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);

    /**
     * FWK_BATCH_APP.CRON_TEXT 단독 업데이트.
     * 스케줄 변경 API에서 전체 배치앱 정보를 갱신하지 않고 Cron 표현식만 변경할 때 사용한다.
     */
    void updateCronText(
            @Param("batchAppId") String batchAppId,
            @Param("cronText") String cronText,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);
}
