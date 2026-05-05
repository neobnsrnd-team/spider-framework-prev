package com.example.spideradmin.domain.cmsapproval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.spideradmin.domain.cmsapproval.dto.CmsApprovalHistoryResponse;
import com.example.spideradmin.domain.cmsapproval.dto.CmsApprovalListRequest;
import com.example.spideradmin.domain.cmsapproval.dto.CmsApprovalPageResponse;
import com.example.spideradmin.domain.cmsapproval.dto.CmsApprovalRollbackHistoryResponse;
import com.example.spideradmin.domain.cmsapproval.dto.CmsApproveRequest;
import com.example.spideradmin.domain.cmsapproval.dto.CmsDisplayPeriodRequest;
import com.example.spideradmin.domain.cmsapproval.dto.CmsPublicStateRequest;
import com.example.spideradmin.domain.cmsapproval.dto.CmsRejectRequest;
import com.example.spideradmin.domain.cmsapproval.dto.CmsRollbackRequest;
import com.example.spideradmin.domain.cmsapproval.mapper.CmsApprovalMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CmsApprovalService 테스트")
class CmsApprovalServiceTest {

    @Mock
    private CmsApprovalMapper cmsApprovalMapper;

    @InjectMocks
    private CmsApprovalService cmsApprovalService;

    private static final String PAGE_ID = "PAGE-001";
    private static final String MODIFIER_ID = "admin";

    // ─── findPageList ─────────────────────────────────────────────────

    @Test
    @DisplayName("[조회] 목록 조회 시 PageResponse를 반환한다")
    void findPageList_returnsPageResponse() {
        CmsApprovalListRequest req = new CmsApprovalListRequest();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();
        List<CmsApprovalPageResponse> data = List.of(buildPageResponse());

        given(cmsApprovalMapper.countPageList(req)).willReturn(1L);
        given(cmsApprovalMapper.findPageList(any(), anyInt(), anyInt())).willReturn(data);

        PageResponse<CmsApprovalPageResponse> result = cmsApprovalService.findPageList(req, pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPageId()).isEqualTo(PAGE_ID);
    }

    @Test
    @DisplayName("[조회] 결과가 없으면 빈 목록을 반환한다")
    void findPageList_empty_returnsEmptyContent() {
        CmsApprovalListRequest req = new CmsApprovalListRequest();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();

        given(cmsApprovalMapper.countPageList(req)).willReturn(0L);
        given(cmsApprovalMapper.findPageList(any(), anyInt(), anyInt())).willReturn(List.of());

        PageResponse<CmsApprovalPageResponse> result = cmsApprovalService.findPageList(req, pageRequest);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // ─── approve ─────────────────────────────────────────────────────

    @Test
    @DisplayName("[승인] 정상 승인 시 상태 변경 후 이력을 저장한다")
    void approve_normal_changesStateAndInsertsHistory() {
        CmsApproveRequest req = new CmsApproveRequest();
        req.setBeginningDate("2099-04-17");
        req.setExpiredDate("2099-04-18");

        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(cmsApprovalMapper.approve(PAGE_ID, "2099-04-17", "2099-04-18", MODIFIER_ID))
                .willReturn(1);
        given(cmsApprovalMapper.getNextVersion(PAGE_ID)).willReturn(1);

        cmsApprovalService.approve(PAGE_ID, req, MODIFIER_ID);

        then(cmsApprovalMapper).should().approve(eq(PAGE_ID), eq("2099-04-17"), eq("2099-04-18"), eq(MODIFIER_ID));
        then(cmsApprovalMapper).should().insertHistory(eq(PAGE_ID), eq(1));
    }

    @Test
    @DisplayName("[승인] 노출 기간 미지정(null)이어도 승인이 정상 처리된다")
    void approve_missingDisplayPeriod_approvesSuccessfully() {
        // 승인 요청 시 날짜를 지정하지 않을 수 있으므로 null은 허용
        CmsApproveRequest req = new CmsApproveRequest();
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(cmsApprovalMapper.approve(PAGE_ID, null, null, MODIFIER_ID)).willReturn(1);
        given(cmsApprovalMapper.getNextVersion(PAGE_ID)).willReturn(1);

        cmsApprovalService.approve(PAGE_ID, req, MODIFIER_ID);

        then(cmsApprovalMapper).should().approve(eq(PAGE_ID), eq(null), eq(null), eq(MODIFIER_ID));
        then(cmsApprovalMapper).should().insertHistory(eq(PAGE_ID), eq(1));
    }

    @Test
    @DisplayName("[승인] 재요청 건의 기존 과거 시작일을 유지해도 승인할 수 있다")
    void approve_pastBeginningDateWithFutureExpiredDate_allowsApproval() {
        String beginningDate = LocalDate.now().minusDays(1).toString();
        String expiredDate = LocalDate.now().plusDays(7).toString();
        CmsApproveRequest req = new CmsApproveRequest();
        req.setBeginningDate(beginningDate);
        req.setExpiredDate(expiredDate);

        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(cmsApprovalMapper.approve(PAGE_ID, beginningDate, expiredDate, MODIFIER_ID))
                .willReturn(1);
        given(cmsApprovalMapper.getNextVersion(PAGE_ID)).willReturn(1);

        cmsApprovalService.approve(PAGE_ID, req, MODIFIER_ID);

        then(cmsApprovalMapper).should().approve(eq(PAGE_ID), eq(beginningDate), eq(expiredDate), eq(MODIFIER_ID));
        then(cmsApprovalMapper).should().insertHistory(eq(PAGE_ID), eq(1));
    }

    @Test
    @DisplayName("[승인] 페이지가 없으면 NotFoundException을 던진다")
    void approve_pageNotFound_throwsNotFoundException() {
        CmsApproveRequest req = new CmsApproveRequest();
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(0);

        assertThatThrownBy(() -> cmsApprovalService.approve(PAGE_ID, req, MODIFIER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    // ─── reject ──────────────────────────────────────────────────────

    @Test
    @DisplayName("[반려] 정상 반려 시 상태 변경 후 이력을 저장한다")
    void reject_normal_changesStateAndInsertsHistory() {
        CmsRejectRequest req = new CmsRejectRequest();
        req.setRejectedReason("내용 부적합");

        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(cmsApprovalMapper.reject(PAGE_ID, "내용 부적합", MODIFIER_ID)).willReturn(1);
        given(cmsApprovalMapper.getNextVersion(PAGE_ID)).willReturn(2);

        cmsApprovalService.reject(PAGE_ID, req, MODIFIER_ID);

        then(cmsApprovalMapper).should().reject(eq(PAGE_ID), eq("내용 부적합"), eq(MODIFIER_ID));
        then(cmsApprovalMapper).should().insertHistory(eq(PAGE_ID), eq(2));
    }

    @Test
    @DisplayName("[반려] 페이지가 없으면 NotFoundException을 던진다")
    void reject_pageNotFound_throwsNotFoundException() {
        CmsRejectRequest req = new CmsRejectRequest();
        req.setRejectedReason("이유");
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(0);

        assertThatThrownBy(() -> cmsApprovalService.reject(PAGE_ID, req, MODIFIER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("[승인] PENDING 상태가 아니면 InvalidInputException을 던진다")
    void approve_notPending_throwsInvalidInputException() {
        CmsApproveRequest req = new CmsApproveRequest();
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(cmsApprovalMapper.approve(PAGE_ID, null, null, MODIFIER_ID)).willReturn(0);

        assertThatThrownBy(() -> cmsApprovalService.approve(PAGE_ID, req, MODIFIER_ID))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("[반려] PENDING 상태가 아니면 InvalidInputException을 던진다")
    void reject_notPending_throwsInvalidInputException() {
        CmsRejectRequest req = new CmsRejectRequest();
        req.setRejectedReason("이유");
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(cmsApprovalMapper.reject(PAGE_ID, "이유", MODIFIER_ID)).willReturn(0);

        assertThatThrownBy(() -> cmsApprovalService.reject(PAGE_ID, req, MODIFIER_ID))
                .isInstanceOf(InvalidInputException.class);
    }

    // ─── updatePublicState ────────────────────────────────────────────

    @Test
    @DisplayName("[공개상태] 공개로 변경 시 mapper를 호출한다")
    void updatePublicState_toPublic_callsMapper() {
        CmsPublicStateRequest req = new CmsPublicStateRequest();
        req.setIsPublic("Y");
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);

        cmsApprovalService.updatePublicState(PAGE_ID, req, MODIFIER_ID);

        then(cmsApprovalMapper).should().updatePublicState(eq(PAGE_ID), eq("Y"), eq(MODIFIER_ID));
    }

    @Test
    @DisplayName("[공개상태] 비공개로 변경 시 mapper를 호출한다")
    void updatePublicState_toPrivate_callsMapper() {
        CmsPublicStateRequest req = new CmsPublicStateRequest();
        req.setIsPublic("N");
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);

        cmsApprovalService.updatePublicState(PAGE_ID, req, MODIFIER_ID);

        then(cmsApprovalMapper).should().updatePublicState(eq(PAGE_ID), eq("N"), eq(MODIFIER_ID));
    }

    @Test
    @DisplayName("[공개상태] 페이지가 없으면 NotFoundException을 던진다")
    void updatePublicState_pageNotFound_throwsNotFoundException() {
        CmsPublicStateRequest req = new CmsPublicStateRequest();
        req.setIsPublic("Y");
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(0);

        assertThatThrownBy(() -> cmsApprovalService.updatePublicState(PAGE_ID, req, MODIFIER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    // ─── updateDisplayPeriod ──────────────────────────────────────────

    @Test
    @DisplayName("[노출기간] 정상 수정 시 mapper를 호출한다")
    void updateDisplayPeriod_normal_callsMapper() {
        CmsDisplayPeriodRequest req = new CmsDisplayPeriodRequest();
        req.setBeginningDate("2026-04-01");
        req.setExpiredDate("2026-12-31");
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);

        cmsApprovalService.updateDisplayPeriod(PAGE_ID, req, MODIFIER_ID);

        then(cmsApprovalMapper)
                .should()
                .updateDisplayPeriod(eq(PAGE_ID), eq("2026-04-01"), eq("2026-12-31"), eq(MODIFIER_ID));
    }

    @Test
    @DisplayName("[노출기간] 페이지가 없으면 NotFoundException을 던진다")
    void updateDisplayPeriod_pageNotFound_throwsNotFoundException() {
        CmsDisplayPeriodRequest req = new CmsDisplayPeriodRequest();
        req.setBeginningDate("2026-04-01");
        req.setExpiredDate("2026-12-31");
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(0);

        assertThatThrownBy(() -> cmsApprovalService.updateDisplayPeriod(PAGE_ID, req, MODIFIER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    // ─── findHistoryList ──────────────────────────────────────────────

    @Test
    @DisplayName("[이력조회] 이력 목록을 반환한다")
    void findHistoryList_returnsHistory() {
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(cmsApprovalMapper.findHistoryList(PAGE_ID)).willReturn(List.of(buildHistoryResponse()));

        List<CmsApprovalHistoryResponse> result = cmsApprovalService.findHistoryList(PAGE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getApproveState()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("[이력조회] 이력이 없으면 빈 목록을 반환한다")
    void findHistoryList_empty_returnsEmptyList() {
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(cmsApprovalMapper.findHistoryList(PAGE_ID)).willReturn(List.of());

        List<CmsApprovalHistoryResponse> result = cmsApprovalService.findHistoryList(PAGE_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[이력조회] 페이지가 없으면 NotFoundException을 던진다")
    void findHistoryList_pageNotFound_throwsNotFoundException() {
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(0);

        assertThatThrownBy(() -> cmsApprovalService.findHistoryList(PAGE_ID)).isInstanceOf(NotFoundException.class);
    }

    // ─── rollback ────────────────────────────────────────────────────

    @Test
    @DisplayName("[롤백] 정상 롤백 시 지정 버전으로 복원한다")
    void rollback_normal_restoresVersion() {
        CmsRollbackRequest req = new CmsRollbackRequest();
        req.setVersion(1);
        CmsApprovalRollbackHistoryResponse history = CmsApprovalRollbackHistoryResponse.builder()
                .pageHtml("<html/>")
                .filePath("/path/to/file.html")
                .build();

        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(cmsApprovalMapper.findHistoryByVersion(PAGE_ID, 1)).willReturn(history);

        cmsApprovalService.rollback(PAGE_ID, req, MODIFIER_ID);

        then(cmsApprovalMapper)
                .should()
                .rollback(eq(PAGE_ID), eq("<html/>"), eq("/path/to/file.html"), eq(MODIFIER_ID));
    }

    @Test
    @DisplayName("[롤백] 페이지가 없으면 NotFoundException을 던진다")
    void rollback_pageNotFound_throwsNotFoundException() {
        CmsRollbackRequest req = new CmsRollbackRequest();
        req.setVersion(1);
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(0);

        assertThatThrownBy(() -> cmsApprovalService.rollback(PAGE_ID, req, MODIFIER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("[롤백] 버전 이력이 없으면 NotFoundException을 던진다")
    void rollback_historyNotFound_throwsNotFoundException() {
        CmsRollbackRequest req = new CmsRollbackRequest();
        req.setVersion(99);
        given(cmsApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(cmsApprovalMapper.findHistoryByVersion(PAGE_ID, 99)).willReturn(null);

        assertThatThrownBy(() -> cmsApprovalService.rollback(PAGE_ID, req, MODIFIER_ID))
                .isInstanceOf(NotFoundException.class);
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private CmsApprovalPageResponse buildPageResponse() {
        return CmsApprovalPageResponse.builder()
                .pageId(PAGE_ID)
                .pageName("테스트 페이지")
                .approveState("PENDING")
                .displayState("PENDING")
                .beginningDate("2099-04-17")
                .expiredDate("2099-04-18")
                .isPublic("Y")
                .build();
    }

    private CmsApprovalHistoryResponse buildHistoryResponse() {
        return CmsApprovalHistoryResponse.builder()
                .version(1)
                .approveState("APPROVED")
                .lastModifierName("홍길동")
                .approveDate("2026-04-01 10:00:00")
                .build();
    }
}
