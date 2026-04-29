package com.example.spider_admin.domain.cmsstatistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsDetailRequest;
import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsDetailResponse;
import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsRequest;
import com.example.spider_admin.domain.cmsstatistics.dto.CmsStatisticsResponse;
import com.example.spider_admin.domain.cmsstatistics.mapper.CmsStatisticsMapper;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CmsStatisticsService 테스트")
class CmsStatisticsServiceTest {

    @Mock
    private CmsStatisticsMapper cmsStatisticsMapper;

    @InjectMocks
    private CmsStatisticsService cmsStatisticsService;

    private static final String PAGE_ID = "PAGE-001";

    // ─── findStatList ──────────────────────────────────────────

    @Test
    @DisplayName("[조회] 통계 목록 조회 시 PageResponse를 반환한다")
    void findStatList_returnsPageResponse() {
        CmsStatisticsRequest req = buildRequest();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();
        List<CmsStatisticsResponse> data = List.of(buildStatResponse());

        given(cmsStatisticsMapper.countStatList(req)).willReturn(1L);
        given(cmsStatisticsMapper.findStatList(any(), anyLong(), anyLong())).willReturn(data);

        PageResponse<CmsStatisticsResponse> result = cmsStatisticsService.findStatList(req, pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPageId()).isEqualTo(PAGE_ID);
        assertThat(result.getContent().get(0).getViewCount()).isEqualTo(50L);
        assertThat(result.getContent().get(0).getClickCount()).isEqualTo(20L);
    }

    @Test
    @DisplayName("[조회] 결과가 없으면 빈 목록을 반환한다")
    void findStatList_empty_returnsEmptyContent() {
        CmsStatisticsRequest req = buildRequest();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();

        given(cmsStatisticsMapper.countStatList(req)).willReturn(0L);
        given(cmsStatisticsMapper.findStatList(any(), anyLong(), anyLong())).willReturn(List.of());

        PageResponse<CmsStatisticsResponse> result = cmsStatisticsService.findStatList(req, pageRequest);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("[조회] 2페이지 요청 시 페이지 정보가 올바르게 계산된다")
    void findStatList_page2_returnsCorrectPageInfo() {
        CmsStatisticsRequest req = buildRequest();
        PageRequest pageRequest = PageRequest.builder().page(1).size(10).build();

        given(cmsStatisticsMapper.countStatList(req)).willReturn(15L);
        given(cmsStatisticsMapper.findStatList(any(), anyLong(), anyLong())).willReturn(List.of(buildStatResponse()));

        PageResponse<CmsStatisticsResponse> result = cmsStatisticsService.findStatList(req, pageRequest);

        assertThat(result.getCurrentPage()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getHasNext()).isFalse();
        assertThat(result.getHasPrevious()).isTrue();
    }

    // ─── findDetailList ────────────────────────────────────────

    @Test
    @DisplayName("[상세조회] 컴포넌트 클릭 상세 목록을 반환한다")
    void findDetailList_returnsDetailList() {
        CmsStatisticsDetailRequest req = buildDetailRequest();
        List<CmsStatisticsDetailResponse> data =
                List.of(buildDetailResponse("BTN-APPLY", 15L), buildDetailResponse("BTN-CANCEL", 3L));

        given(cmsStatisticsMapper.findDetailList(req)).willReturn(data);

        List<CmsStatisticsDetailResponse> result = cmsStatisticsService.findDetailList(req);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getComponentId()).isEqualTo("BTN-APPLY");
        assertThat(result.get(0).getClickCount()).isEqualTo(15L);
    }

    @Test
    @DisplayName("[상세조회] 클릭 데이터가 없으면 빈 목록을 반환한다")
    void findDetailList_empty_returnsEmptyList() {
        CmsStatisticsDetailRequest req = buildDetailRequest();
        given(cmsStatisticsMapper.findDetailList(req)).willReturn(List.of());

        List<CmsStatisticsDetailResponse> result = cmsStatisticsService.findDetailList(req);

        assertThat(result).isEmpty();
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────

    private CmsStatisticsRequest buildRequest() {
        CmsStatisticsRequest req = new CmsStatisticsRequest();
        req.setStartDate("2026-01-01");
        req.setEndDate("2026-04-16");
        return req;
    }

    private CmsStatisticsDetailRequest buildDetailRequest() {
        CmsStatisticsDetailRequest req = new CmsStatisticsDetailRequest();
        req.setPageId(PAGE_ID);
        req.setStartDate("2026-01-01");
        req.setEndDate("2026-04-16");
        return req;
    }

    private CmsStatisticsResponse buildStatResponse() {
        return CmsStatisticsResponse.builder()
                .pageId(PAGE_ID)
                .pageName("테스트 페이지")
                .viewCount(50L)
                .clickCount(20L)
                .build();
    }

    private CmsStatisticsDetailResponse buildDetailResponse(String componentId, long clickCount) {
        return CmsStatisticsDetailResponse.builder()
                .componentId(componentId)
                .clickCount(clickCount)
                .build();
    }
}
