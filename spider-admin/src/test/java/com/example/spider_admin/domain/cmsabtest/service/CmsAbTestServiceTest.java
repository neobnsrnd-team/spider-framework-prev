package com.example.spider_admin.domain.cmsabtest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.spider_admin.domain.cmsabtest.dto.CmsAbGroupSaveRequest;
import com.example.spider_admin.domain.cmsabtest.dto.CmsAbPageResponse;
import com.example.spider_admin.domain.cmsabtest.dto.CmsAbPageWeightRequest;
import com.example.spider_admin.domain.cmsabtest.dto.CmsAbTestDashboardResponse;
import com.example.spider_admin.domain.cmsabtest.dto.CmsAbTestListRequest;
import com.example.spider_admin.domain.cmsabtest.dto.CmsAbWeightUpdateRequest;
import com.example.spider_admin.domain.cmsabtest.mapper.CmsAbTestMapper;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.exception.InvalidInputException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CmsAbTestService tests")
class CmsAbTestServiceTest {

    @Mock
    private CmsAbTestMapper cmsAbTestMapper;

    @InjectMocks
    private CmsAbTestService cmsAbTestService;

    private static final String GROUP_ID = "main-banner";
    private static final String PAGE_ID = "PAGE-001";
    private static final String PAGE_ID_2 = "PAGE-002";
    private static final String MODIFIER_ID = "admin";

    @Test
    @DisplayName("findDashboard uses lightweight page options separately from grouped stats")
    void findDashboard_usesSeparatedQueries() {
        CmsAbTestListRequest req = new CmsAbTestListRequest();
        req.setSearch("banner");
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();

        given(cmsAbTestMapper.countApprovedPages(any())).willReturn(1L);
        given(cmsAbTestMapper.findApprovedPages(any(), anyInt(), anyInt())).willReturn(List.of(page(PAGE_ID)));
        given(cmsAbTestMapper.findAbGroupPages("banner")).willReturn(List.of(groupedPage(PAGE_ID)));
        given(cmsAbTestMapper.findApprovedPageOptions("banner")).willReturn(List.of(page(PAGE_ID_2)));

        CmsAbTestDashboardResponse result = cmsAbTestService.findDashboard(req, pageRequest);

        then(cmsAbTestMapper).should().findAbGroupPages("banner");
        then(cmsAbTestMapper).should().findApprovedPageOptions("banner");
        assertThat(result.getPages().getContent()).hasSize(1);
        assertThat(result.getGroups()).hasSize(1);
        assertThat(result.getAvailablePages()).extracting("pageId").containsExactly(PAGE_ID_2);
    }

    @Test
    @DisplayName("saveGroup allows zero weight when editing an existing promoted group")
    void saveGroup_existingGroup_allowsZeroWeight() {
        CmsAbGroupSaveRequest req = groupSaveRequest(BigDecimal.ONE, BigDecimal.ZERO);
        given(cmsAbTestMapper.countGroupPages(GROUP_ID)).willReturn(2L);
        given(cmsAbTestMapper.countApprovedPagesByIds(List.of(PAGE_ID, PAGE_ID_2)))
                .willReturn(2L);
        given(cmsAbTestMapper.countConflictingPages(eq(GROUP_ID), eq(List.of(PAGE_ID, PAGE_ID_2))))
                .willReturn(0L);
        given(cmsAbTestMapper.updateAbGroup(any(), eq(GROUP_ID), any(), eq(MODIFIER_ID)))
                .willReturn(1);

        cmsAbTestService.saveGroup(req, MODIFIER_ID);

        then(cmsAbTestMapper).should().clearAbGroup(GROUP_ID, MODIFIER_ID);
        then(cmsAbTestMapper).should().updateAbGroup(PAGE_ID, GROUP_ID, BigDecimal.ONE, MODIFIER_ID);
        then(cmsAbTestMapper).should().updateAbGroup(PAGE_ID_2, GROUP_ID, BigDecimal.ZERO, MODIFIER_ID);
    }

    @Test
    @DisplayName("saveGroup rejects zero weight for a new group")
    void saveGroup_newGroup_rejectsZeroWeight() {
        CmsAbGroupSaveRequest req = groupSaveRequest(BigDecimal.ONE, BigDecimal.ZERO);
        given(cmsAbTestMapper.countGroupPages(GROUP_ID)).willReturn(0L);

        assertThatThrownBy(() -> cmsAbTestService.saveGroup(req, MODIFIER_ID))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("updateWeights allows zero weight but requires one positive weight")
    void updateWeights_allowsZeroWeightWithPositiveWinner() {
        CmsAbWeightUpdateRequest req = new CmsAbWeightUpdateRequest();
        req.setPages(List.of(weight(PAGE_ID, BigDecimal.ONE), weight(PAGE_ID_2, BigDecimal.ZERO)));
        given(cmsAbTestMapper.countGroupPages(GROUP_ID)).willReturn(2L);
        given(cmsAbTestMapper.countPageInGroup(GROUP_ID, PAGE_ID)).willReturn(1L);
        given(cmsAbTestMapper.countPageInGroup(GROUP_ID, PAGE_ID_2)).willReturn(1L);

        cmsAbTestService.updateWeights(GROUP_ID, req, MODIFIER_ID);

        then(cmsAbTestMapper).should().updateAbGroup(PAGE_ID, GROUP_ID, BigDecimal.ONE, MODIFIER_ID);
        then(cmsAbTestMapper).should().updateAbGroup(PAGE_ID_2, GROUP_ID, BigDecimal.ZERO, MODIFIER_ID);
    }

    @Test
    @DisplayName("updateWeights rejects all-zero weights")
    void updateWeights_allZeroWeights_rejects() {
        CmsAbWeightUpdateRequest req = new CmsAbWeightUpdateRequest();
        req.setPages(List.of(weight(PAGE_ID, BigDecimal.ZERO), weight(PAGE_ID_2, BigDecimal.ZERO)));
        given(cmsAbTestMapper.countGroupPages(GROUP_ID)).willReturn(2L);

        assertThatThrownBy(() -> cmsAbTestService.updateWeights(GROUP_ID, req, MODIFIER_ID))
                .isInstanceOf(InvalidInputException.class);
    }

    private CmsAbGroupSaveRequest groupSaveRequest(BigDecimal firstWeight, BigDecimal secondWeight) {
        CmsAbGroupSaveRequest req = new CmsAbGroupSaveRequest();
        req.setGroupId(GROUP_ID);
        req.setPages(List.of(weight(PAGE_ID, firstWeight), weight(PAGE_ID_2, secondWeight)));
        return req;
    }

    private CmsAbPageWeightRequest weight(String pageId, BigDecimal value) {
        CmsAbPageWeightRequest req = new CmsAbPageWeightRequest();
        req.setPageId(pageId);
        req.setWeight(value);
        return req;
    }

    private CmsAbPageResponse page(String pageId) {
        return CmsAbPageResponse.builder().pageId(pageId).pageName(pageId).build();
    }

    private CmsAbPageResponse groupedPage(String pageId) {
        return CmsAbPageResponse.builder()
                .pageId(pageId)
                .pageName(pageId)
                .abGroupId(GROUP_ID)
                .abWeight(BigDecimal.ONE)
                .build();
    }
}
