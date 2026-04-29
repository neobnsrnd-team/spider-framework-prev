package com.example.spider_admin.domain.cmsstatistics.service;

import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsDetailRequest;
import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsDetailResponse;
import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsRequest;
import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsResponse;
import com.example.spider_admin.domain.cmsstatistics.mapper.CmsStatisticsMapper;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CMS 통계 조회 서비스 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CmsStatisticsService {

    private final CmsStatisticsMapper cmsStatisticsMapper;

    /** 페이지별 조회/클릭 통계 목록 조회 (페이지네이션) */
    public PageResponse<CmsStatisticsResponse> findStatList(CmsStatisticsRequest req, PageRequest pageRequest) {
        long total = cmsStatisticsMapper.countStatList(req);
        List<CmsStatisticsResponse> list =
                cmsStatisticsMapper.findStatList(req, pageRequest.getOffset(), pageRequest.getEndRow());
        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    /** 컴포넌트별 클릭 수 상세 조회 */
    public List<CmsStatisticsDetailResponse> findDetailList(CmsStatisticsDetailRequest req) {
        return cmsStatisticsMapper.findDetailList(req);
    }
}
