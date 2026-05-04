package com.example.spideradmin.domain.errorcode.mapper;

import com.example.spideradmin.domain.errorcode.dto.ErrorCreateRequest;
import com.example.spideradmin.domain.errorcode.dto.ErrorResponse;
import com.example.spideradmin.domain.errorcode.dto.ErrorUpdateRequest;
import com.example.spideradmin.domain.errorcode.dto.ErrorWithHandleAppsResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis Mapper for FWK_ERROR table
 * 오류코드 관리
 */
public interface ErrorMapper {

    // 기본 CRUD
    ErrorResponse selectResponseById(@Param("errorCode") String errorCode);

    void insert(
            @Param("dto") ErrorCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void update(
            @Param("errorCode") String errorCode,
            @Param("dto") ErrorUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void deleteById(@Param("errorCode") String errorCode);

    // 존재 확인
    int countByErrorCode(@Param("errorCode") String errorCode);

    /**
     * 오류코드 검색 (핸들러 목록 포함)
     * N+1 문제 해결을 위해 LISTAGG로 핸들러 목록을 함께 조회
     */
    List<ErrorWithHandleAppsResponse> searchWithHandleApps(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("trxId") String trxId,
            @Param("handleAppId") String handleAppId,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 오류코드 검색 건수 조회
     */
    long countWithHandleApps(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("trxId") String trxId,
            @Param("handleAppId") String handleAppId);

    /**
     * 엑셀 내보내기용 전체 조회 (페이징 없음)
     */
    List<ErrorWithHandleAppsResponse> findAllForExport(
            @Param("searchField") String searchField,
            @Param("searchValue") String searchValue,
            @Param("trxId") String trxId,
            @Param("handleAppId") String handleAppId,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
