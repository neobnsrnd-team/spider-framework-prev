package com.example.spideradmin.domain.monitor.mapper;

import com.example.spideradmin.domain.monitor.dto.MonitorCreateRequest;
import com.example.spideradmin.domain.monitor.dto.MonitorResponse;
import com.example.spideradmin.domain.monitor.dto.MonitorUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <h3>모니터 Mapper (CRUD + Query)</h3>
 * <p>모니터 현황판의 생성, 수정, 삭제, 조회를 담당합니다.</p>
 */
@Mapper
public interface MonitorMapper {

    /**
     * 모니터 생성
     */
    void insertMonitor(
            @Param("dto") MonitorCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 모니터 수정
     */
    void updateMonitor(
            @Param("dto") MonitorUpdateRequest dto,
            @Param("oldMonitorId") String oldMonitorId,
            @Param("updateMonitorId") boolean updateMonitorId,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 모니터 삭제
     * @param monitorId 모니터 ID
     * @return 삭제된 행의 수 (0이면 존재하지 않음, 1이면 삭제 성공)
     */
    int deleteById(String monitorId);

    /**
     * 모니터 ID 중복 체크
     * @param monitorId 모니터 ID
     * @return 존재하면 1, 없으면 0
     */
    int countByMonitorId(String monitorId);

    // ==================== 목록 조회 ====================

    /**
     * 전체 모니터 목록 조회
     * @return 모니터 목록 (DTO)
     */
    List<MonitorResponse> findAll();

    /**
     * 검색 조건으로 모니터 목록 조회 (네이티브 ROWNUM 페이징)
     */
    List<MonitorResponse> findAllWithSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("useYnFilter") String useYnFilter,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 검색 조건에 맞는 모니터 수를 조회합니다.
     */
    long countAllWithSearch(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("useYnFilter") String useYnFilter);

    // ==================== 단건 조회 ====================

    /**
     * 모니터 ID로 조회 (상세 조회용)
     * @param monitorId 모니터 ID
     * @return 모니터 DTO, 없으면 null
     */
    MonitorResponse findById(@Param("monitorId") String monitorId);

    // ==================== 집계 ====================

    /**
     * 전체 모니터 수
     */
    long countAll();

    /**
     * 사용 중인 모니터 수
     */
    long countActive();
}
