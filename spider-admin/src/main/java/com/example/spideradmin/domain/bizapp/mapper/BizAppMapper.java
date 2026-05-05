package com.example.spideradmin.domain.bizapp.mapper;

import com.example.spideradmin.domain.bizapp.dto.BizAppCreateRequest;
import com.example.spideradmin.domain.bizapp.dto.BizAppResponse;
import com.example.spideradmin.domain.bizapp.dto.BizAppUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Biz App Mapper (CRUD + Query) */
@Mapper
public interface BizAppMapper {

    /** Biz App ID로 단건 조회 */
    BizAppResponse selectResponseById(@Param("bizAppId") String bizAppId);

    /** Biz App ID 존재 확인용 카운트 */
    int countById(@Param("bizAppId") String bizAppId);

    /** 새 Biz App 등록 */
    void insert(
            @Param("dto") BizAppCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /** Biz App 수정 */
    void update(
            @Param("bizAppId") String bizAppId,
            @Param("dto") BizAppUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /** Biz App 삭제 */
    void deleteById(@Param("bizAppId") String bizAppId);

    /** 검색 조건으로 Biz App 목록 조회 (페이지네이션) */
    @SuppressWarnings("java:S107")
    List<BizAppResponse> findAllWithSearch(
            @Param("bizAppId") String bizAppId,
            @Param("bizAppName") String bizAppName,
            @Param("dupCheckYn") String dupCheckYn,
            @Param("logYn") String logYn,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /** 검색 조건으로 Biz App 전체 건수 조회 */
    long countAllWithSearch(
            @Param("bizAppId") String bizAppId,
            @Param("bizAppName") String bizAppName,
            @Param("dupCheckYn") String dupCheckYn,
            @Param("logYn") String logYn);
}
