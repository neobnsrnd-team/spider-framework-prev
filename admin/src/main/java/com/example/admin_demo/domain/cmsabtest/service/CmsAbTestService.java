package com.example.admin_demo.domain.cmsabtest.service;

import com.example.admin_demo.domain.cmsabtest.dto.CmsAbGroupPageResponse;
import com.example.admin_demo.domain.cmsabtest.dto.CmsAbGroupResponse;
import com.example.admin_demo.domain.cmsabtest.dto.CmsAbGroupSaveRequest;
import com.example.admin_demo.domain.cmsabtest.dto.CmsAbPageResponse;
import com.example.admin_demo.domain.cmsabtest.dto.CmsAbPageWeightRequest;
import com.example.admin_demo.domain.cmsabtest.dto.CmsAbTestDashboardResponse;
import com.example.admin_demo.domain.cmsabtest.dto.CmsAbTestListRequest;
import com.example.admin_demo.domain.cmsabtest.dto.CmsAbWeightUpdateRequest;
import com.example.admin_demo.domain.cmsabtest.mapper.CmsAbTestMapper;
import com.example.admin_demo.global.dto.PageRequest;
import com.example.admin_demo.global.dto.PageResponse;
import com.example.admin_demo.global.exception.InvalidInputException;
import com.example.admin_demo.global.exception.NotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CMS A/B 테스트의 비즈니스 규칙을 검증하고 그룹 상태를 변경하는 서비스.
 *
 * <p>A/B 테스트는 승인 상태, 공개 가능 여부, 그룹 중복, 가중치 유효성 같은 제약을 함께 확인해야 하므로
 * 단순 매퍼 호출로 처리하기 어렵다. 이 서비스는 화면 요청을 실험 가능한 데이터 구조로 조합하고,
 * 실험군 저장과 승격 시 데이터 일관성을 보장하기 위해 필요하다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CmsAbTestService {

    private static final Pattern GROUP_ID_PATTERN = Pattern.compile("^[a-z0-9-]{1,64}$");

    private final CmsAbTestMapper cmsAbTestMapper;

    /** 대시보드 화면에 필요한 페이지 목록, 그룹 목록, 선택 후보 목록을 한 번에 조합한다. */
    public CmsAbTestDashboardResponse findDashboard(CmsAbTestListRequest req, PageRequest pageRequest) {
        req.setSearch(normalize(req.getSearch()));
        long total = cmsAbTestMapper.countApprovedPages(req);
        List<CmsAbPageResponse> pages =
                cmsAbTestMapper.findApprovedPages(req, pageRequest.getOffset(), pageRequest.getEndRow());
        List<CmsAbPageResponse> groupSourcePages = cmsAbTestMapper.findAbGroupPages(req.getSearch());
        List<CmsAbPageResponse> availablePages = cmsAbTestMapper.findApprovedPageOptions(req.getSearch());
        Map<String, CmsAbGroupResponse> groupMap = new LinkedHashMap<>();

        for (CmsAbPageResponse page : groupSourcePages) {
            if (page.getAbGroupId() == null || page.getAbGroupId().isBlank()) {
                continue;
            }
            CmsAbGroupResponse group =
                    groupMap.computeIfAbsent(page.getAbGroupId(), groupId -> CmsAbGroupResponse.builder()
                            .groupId(groupId)
                            .pages(new ArrayList<>())
                            .build());
            group.getPages()
                    .add(CmsAbGroupPageResponse.builder()
                            .pageId(page.getPageId())
                            .pageName(page.getPageName())
                            .viewMode(page.getViewMode())
                            .isPublic(page.getIsPublic())
                            .abWeight(page.getAbWeight())
                            .viewCount(page.getViewCount())
                            .clickCount(page.getClickCount())
                            .build());
        }

        return CmsAbTestDashboardResponse.builder()
                .pages(PageResponse.of(pages, total, pageRequest.getPage(), pageRequest.getSize()))
                .availablePages(availablePages)
                .groups(new ArrayList<>(groupMap.values()))
                .build();
    }

    /** 특정 그룹의 구성 페이지와 현재 가중치를 조회한다. */
    public CmsAbGroupResponse findGroup(String groupId) {
        validateGroupId(groupId);
        return CmsAbGroupResponse.builder()
                .groupId(groupId)
                .pages(cmsAbTestMapper.findGroupPages(groupId))
                .build();
    }

    /**
     * 그룹 전체 구성을 저장한다.
     *
     * <p>기존 그룹이 있더라도 먼저 연결을 비운 뒤 다시 채워 요청 시점의 구성을 최종 상태로 맞춘다.
     */
    @Transactional
    public void saveGroup(CmsAbGroupSaveRequest req, String modifierId) {
        validateGroupId(req.getGroupId());
        boolean existingGroup = cmsAbTestMapper.countGroupPages(req.getGroupId()) > 0;
        List<CmsAbPageWeightRequest> pages = validatePages(req.getPages(), true, existingGroup);
        validateApprovedPages(pages);
        validateNoOtherGroupConflict(req.getGroupId(), pages);

        cmsAbTestMapper.clearAbGroup(req.getGroupId(), modifierId);
        for (CmsAbPageWeightRequest page : pages) {
            int updated =
                    cmsAbTestMapper.updateAbGroup(page.getPageId(), req.getGroupId(), page.getWeight(), modifierId);
            if (updated == 0) {
                throw new InvalidInputException("A/B group cannot include page: " + page.getPageId());
            }
        }
    }

    /** 기존 그룹 구성원들의 노출 가중치만 수정한다. */
    @Transactional
    public void updateWeights(String groupId, CmsAbWeightUpdateRequest req, String modifierId) {
        validateGroupExists(groupId);
        List<CmsAbPageWeightRequest> pages = validatePages(req.getPages(), true, true);
        validateAllPagesBelongToGroup(groupId, pages);

        for (CmsAbPageWeightRequest page : pages) {
            cmsAbTestMapper.updateAbGroup(page.getPageId(), groupId, page.getWeight(), modifierId);
        }
    }

    /** 그룹에 속한 모든 페이지의 A/B 연결을 해제한다. */
    @Transactional
    public void clearGroup(String groupId, String modifierId) {
        validateGroupExists(groupId);
        cmsAbTestMapper.clearAbGroup(groupId, modifierId);
    }

    /** 특정 페이지 하나만 A/B 그룹에서 제거한다. */
    @Transactional
    public void clearPage(String pageId, String modifierId) {
        validateText(pageId, "pageId is required.");
        int updated = cmsAbTestMapper.clearPageAbGroup(pageId, modifierId);
        if (updated == 0) {
            throw new NotFoundException("A/B page not found: " + pageId);
        }
    }

    /**
     * 지정한 우승 페이지를 최종안으로 승격한다.
     *
     * <p>우승 페이지가 실제 그룹 구성원인지 검증한 뒤 나머지 후보를 정리하고 우승안 상태를 반영한다.
     */
    @Transactional
    public void promote(String groupId, String winnerPageId, String modifierId) {
        validateGroupExists(groupId);
        validateText(winnerPageId, "winnerPageId is required.");
        if (cmsAbTestMapper.countPageInGroup(groupId, winnerPageId) == 0) {
            throw new InvalidInputException("winnerPageId does not belong to group: " + groupId);
        }
        cmsAbTestMapper.promoteLosers(groupId, winnerPageId, modifierId);
        cmsAbTestMapper.setWinner(winnerPageId, modifierId);
    }

    private void validateGroupExists(String groupId) {
        validateGroupId(groupId);
        if (cmsAbTestMapper.countGroupPages(groupId) == 0) {
            throw new NotFoundException("A/B group not found: " + groupId);
        }
    }

    private void validateApprovedPages(List<CmsAbPageWeightRequest> pages) {
        List<String> pageIds =
                pages.stream().map(CmsAbPageWeightRequest::getPageId).toList();
        if (cmsAbTestMapper.countApprovedPagesByIds(pageIds) != pageIds.size()) {
            throw new InvalidInputException("Only approved public pages can be used for A/B tests.");
        }
    }

    private void validateNoOtherGroupConflict(String groupId, List<CmsAbPageWeightRequest> pages) {
        List<String> pageIds =
                pages.stream().map(CmsAbPageWeightRequest::getPageId).toList();
        if (cmsAbTestMapper.countConflictingPages(groupId, pageIds) > 0) {
            throw new InvalidInputException("One or more pages already belong to another A/B group.");
        }
    }

    private void validateAllPagesBelongToGroup(String groupId, List<CmsAbPageWeightRequest> pages) {
        for (CmsAbPageWeightRequest page : pages) {
            if (cmsAbTestMapper.countPageInGroup(groupId, page.getPageId()) == 0) {
                throw new InvalidInputException("Page does not belong to A/B group: " + page.getPageId());
            }
        }
    }

    private List<CmsAbPageWeightRequest> validatePages(
            List<CmsAbPageWeightRequest> pages, boolean requireAtLeastTwo, boolean allowZeroWeight) {
        if (pages == null || pages.size() < (requireAtLeastTwo ? 2 : 1)) {
            throw new InvalidInputException("At least two pages are required.");
        }

        Set<String> pageIds = new HashSet<>();
        boolean hasPositiveWeight = false;
        for (CmsAbPageWeightRequest page : pages) {
            if (page == null) {
                throw new InvalidInputException("Page item is required.");
            }
            validateText(page.getPageId(), "pageId is required.");
            if (!pageIds.add(page.getPageId())) {
                throw new InvalidInputException("Duplicate pageId is not allowed: " + page.getPageId());
            }
            if (page.getWeight() == null) {
                throw new InvalidInputException("weight is required.");
            }
            if (page.getWeight().compareTo(BigDecimal.ZERO) < 0
                    || (!allowZeroWeight && page.getWeight().compareTo(BigDecimal.ZERO) == 0)) {
                throw new InvalidInputException(
                        allowZeroWeight ? "weight must be zero or greater." : "weight must be greater than zero.");
            }
            if (page.getWeight().compareTo(new BigDecimal("999.99")) > 0) {
                throw new InvalidInputException("weight must be 999.99 or less.");
            }
            if (page.getWeight().compareTo(BigDecimal.ZERO) > 0) {
                hasPositiveWeight = true;
            }
        }
        if (!hasPositiveWeight) {
            throw new InvalidInputException("At least one page must have a positive weight.");
        }
        return pages;
    }

    private void validateGroupId(String groupId) {
        validateText(groupId, "groupId is required.");
        if (!GROUP_ID_PATTERN.matcher(groupId).matches()) {
            throw new InvalidInputException(
                    "groupId must use lowercase letters, numbers, or hyphens and be 64 chars or less.");
        }
    }

    private void validateText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException(message);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
