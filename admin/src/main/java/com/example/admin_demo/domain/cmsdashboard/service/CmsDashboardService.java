package com.example.admin_demo.domain.cmsdashboard.service;

import com.example.admin_demo.domain.cmsdashboard.dto.CmsDashboardApproveRequestDto;
import com.example.admin_demo.domain.cmsdashboard.dto.CmsDashboardCreateRequest;
import com.example.admin_demo.domain.cmsdashboard.dto.CmsDashboardListRequest;
import com.example.admin_demo.domain.cmsdashboard.dto.CmsDashboardPageResponse;
import com.example.admin_demo.domain.cmsdashboard.dto.CmsTemplateResponse;
import com.example.admin_demo.domain.cmsdashboard.mapper.CmsDashboardMapper;
import com.example.admin_demo.domain.cmsdashboard.util.CmsViewModeUtil;
import com.example.admin_demo.global.dto.PageRequest;
import com.example.admin_demo.global.dto.PageResponse;
import com.example.admin_demo.global.exception.InvalidInputException;
import com.example.admin_demo.global.exception.NotFoundException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CMS 사용자 대시보드 서비스
 *
 * <p>로그인한 사용자가 본인이 생성한 CMS 페이지를 조회·생성·삭제·승인 요청할 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CmsDashboardService {

    private final CmsDashboardMapper cmsDashboardMapper;

    /** 템플릿 목록 조회 — PAGE_TYPE = 'TEMPLATE', USE_YN = 'Y' */
    public List<CmsTemplateResponse> findTemplateList() {
        return cmsDashboardMapper.findTemplateList();
    }

    /** 내 페이지 목록 조회 */
    public PageResponse<CmsDashboardPageResponse> findMyPageList(
            CmsDashboardListRequest req, String userId, PageRequest pageRequest) {

        long total = cmsDashboardMapper.countMyPageList(req, userId);
        List<CmsDashboardPageResponse> list =
                cmsDashboardMapper.findMyPageList(req, userId, pageRequest.getOffset(), pageRequest.getEndRow());

        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    /**
     * 새 페이지 생성
     *
     * <p>PAGE_ID는 CMS 실제 구현(crypto.randomUUID())에 맞춰 UUID로 생성한다.
     *
     * @return 생성된 pageId
     */
    @Transactional
    public String createPage(CmsDashboardCreateRequest req, String userId, String userName) {
        // CMS 원본(crypto.randomUUID())과 동일한 UUID 방식으로 생성
        String pageId = UUID.randomUUID().toString();
        String templateId = "blank".equals(req.getTemplateId()) ? null : req.getTemplateId();

        // 클라이언트 전달값 검증 — PAGE_TYPE='TEMPLATE' AND USE_YN='Y' 인 페이지인지 확인 (임의 pageId 주입 방지)
        if (templateId != null) {
            CmsTemplateResponse template = cmsDashboardMapper.findTemplateById(templateId);
            if (template == null) {
                throw new NotFoundException("유효하지 않은 템플릿입니다. templateId=" + templateId);
            }
            if (!CmsViewModeUtil.isTemplateCompatible(req.getViewMode(), template.getViewMode())) {
                throw new InvalidInputException("선택한 레이아웃과 템플릿의 레이아웃이 일치하지 않습니다. templateId=" + templateId);
            }
        }

        cmsDashboardMapper.insertPage(pageId, req.getPageName(), req.getViewMode(), templateId, userId, userName);
        log.info("CMS 페이지 생성: pageId={}, userId={}", pageId, userId);
        return pageId;
    }

    /**
     * 페이지 삭제
     *
     * <p>CMS page.repository.ts의 deletePage()와 동일한 분기:
     * 이력이 있으면 소프트 삭제(USE_YN='N'), 없으면 하드 삭제(물리 행 삭제).
     */
    @Transactional
    public void deletePage(String pageId, String userId) {
        checkPageOwner(pageId, userId);

        int historyCount = cmsDashboardMapper.hasHistory(pageId);
        if (historyCount > 0) {
            cmsDashboardMapper.deleteSoft(pageId, userId);
            log.info("CMS 페이지 소프트 삭제 (이력 존재): pageId={}, userId={}", pageId, userId);
        } else {
            cmsDashboardMapper.deleteHard(pageId, userId);
            log.info("CMS 페이지 하드 삭제 (이력 없음): pageId={}, userId={}", pageId, userId);
        }
    }

    /**
     * 승인 요청 — APPROVE_STATE: WORK / REJECTED / APPROVED → PENDING
     *
     * <p>CMS DashboardClient.tsx 구현 기준:
     * WORK → "승인요청", REJECTED / APPROVED → "재승인요청" (텍스트만 다르고 로직 동일).
     * 만료 여부와 무관하게 approveState 조건만으로 버튼이 표시되므로 별도 만료 체크 없음.
     */
    @Transactional
    public void requestApproval(String pageId, CmsDashboardApproveRequestDto req, String userId) {
        checkPageOwner(pageId, userId);

        // 클라이언트 전달값 대신 DB에서 직접 승인자 이름 조회 — 위변조 방지
        String approverName = cmsDashboardMapper.findApproverNameById(req.getApproverId());
        if (approverName == null) {
            throw new InvalidInputException("유효하지 않은 승인자입니다. approverId=" + req.getApproverId());
        }

        int updated = cmsDashboardMapper.requestApproval(
                pageId, req.getApproverId(), approverName, req.getBeginningDate(), req.getExpiredDate(), userId);
        if (updated == 0) {
            throw new InvalidInputException("현재 상태에서는 승인 요청할 수 없습니다. pageId=" + pageId);
        }
        log.info("CMS 페이지 승인 요청: pageId={}, approverId={}, userId={}", pageId, req.getApproverId(), userId);
    }

    /** 페이지 소유권 확인 — 존재하지 않거나 본인 페이지가 아니면 예외 */
    private void checkPageOwner(String pageId, String userId) {
        if (cmsDashboardMapper.existsByPageIdAndUserId(pageId, userId) == 0) {
            throw new NotFoundException("페이지를 찾을 수 없습니다. pageId=" + pageId);
        }
    }
}
