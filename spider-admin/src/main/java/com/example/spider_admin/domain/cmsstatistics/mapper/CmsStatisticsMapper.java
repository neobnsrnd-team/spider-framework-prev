package com.example.spider_admin.domain.cmsstatistics.mapper;

import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsDetailRequest;
import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsDetailResponse;
import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsRequest;
import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** CMS 통계 조회 Mapper */
public interface CmsStatisticsMapper {

    // ── 통계 목록 ──────────────────────────────────────────────────────────────

    List<CmsStatisticsResponse> findStatList(
            @Param("req") CmsStatisticsRequest req, @Param("offset") long offset, @Param("endRow") long endRow);

    long countStatList(@Param("req") CmsStatisticsRequest req);

    // ── 컴포넌트 클릭 상세 ───────────────────────────────────────────────────

    List<CmsStatisticsDetailResponse> findDetailList(@Param("req") CmsStatisticsDetailRequest req);
}
