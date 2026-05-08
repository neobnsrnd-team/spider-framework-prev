package com.example.spideradmin.domain.reactcmsadminapproval.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.spideradmin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalApproveRequest;
import com.example.spideradmin.domain.reactcmsadminapproval.dto.ReactCmsAdminApprovalRejectRequest;
import com.example.spideradmin.domain.reactcmsadminapproval.mapper.ReactCmsAdminApprovalMapper;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ReactCmsAdminApprovalService 단위 테스트.
 *
 * <p>주요 검증 대상은 매퍼 SQL의 APPROVE_STATE='PENDING' 가드와 연계된 race condition 방어 로직.
 * mapper.approve/reject가 0을 반환하면 PENDING이 아닌 페이지로 간주하고 InvalidInputException을 던져야 한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReactCmsAdminApprovalService 테스트")
class ReactCmsAdminApprovalServiceTest {

    @Mock
    private ReactCmsAdminApprovalMapper reactCmsAdminApprovalMapper;

    @InjectMocks
    private ReactCmsAdminApprovalService reactCmsAdminApprovalService;

    private static final String PAGE_ID = "REACT-PAGE-001";
    private static final String MODIFIER_ID = "approverUser";

    // ─── approve ─────────────────────────────────────────────────────

    @Test
    @DisplayName("[승인] 정상 승인 시 상태 변경 후 이력을 저장한다")
    void approve_normal_changesStateAndInsertsHistory() {
        ReactCmsAdminApprovalApproveRequest req = new ReactCmsAdminApprovalApproveRequest();
        req.setBeginningDate("2099-04-17");
        req.setExpiredDate("2099-04-18");

        given(reactCmsAdminApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(reactCmsAdminApprovalMapper.approve(PAGE_ID, "2099-04-17", "2099-04-18", MODIFIER_ID))
                .willReturn(1);
        given(reactCmsAdminApprovalMapper.getNextVersion(PAGE_ID)).willReturn(1);

        reactCmsAdminApprovalService.approve(PAGE_ID, req, MODIFIER_ID);

        then(reactCmsAdminApprovalMapper)
                .should()
                .approve(eq(PAGE_ID), eq("2099-04-17"), eq("2099-04-18"), eq(MODIFIER_ID));
        then(reactCmsAdminApprovalMapper).should().insertHistory(eq(PAGE_ID), eq(1));
    }

    @Test
    @DisplayName("[승인] 노출 기간 미지정(null)이어도 승인이 정상 처리된다")
    void approve_missingDisplayPeriod_approvesSuccessfully() {
        // 승인 요청 시 날짜를 지정하지 않을 수 있으므로 null은 허용된다.
        ReactCmsAdminApprovalApproveRequest req = new ReactCmsAdminApprovalApproveRequest();
        given(reactCmsAdminApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(reactCmsAdminApprovalMapper.approve(PAGE_ID, null, null, MODIFIER_ID))
                .willReturn(1);
        given(reactCmsAdminApprovalMapper.getNextVersion(PAGE_ID)).willReturn(1);

        assertThatCode(() -> reactCmsAdminApprovalService.approve(PAGE_ID, req, MODIFIER_ID))
                .doesNotThrowAnyException();

        then(reactCmsAdminApprovalMapper).should().insertHistory(eq(PAGE_ID), eq(1));
    }

    @Test
    @DisplayName("[승인] 페이지가 없으면 NotFoundException을 던진다")
    void approve_pageNotFound_throwsNotFoundException() {
        ReactCmsAdminApprovalApproveRequest req = new ReactCmsAdminApprovalApproveRequest();
        given(reactCmsAdminApprovalMapper.existsByPageId(PAGE_ID)).willReturn(0);

        assertThatThrownBy(() -> reactCmsAdminApprovalService.approve(PAGE_ID, req, MODIFIER_ID))
                .isInstanceOf(NotFoundException.class);
        // mapper.approve는 호출되지 않아야 한다 (존재 검증 단계에서 차단)
        then(reactCmsAdminApprovalMapper).should(never()).approve(any(), any(), any(), any());
    }

    @Test
    @DisplayName("[승인] PENDING 상태가 아니면(매퍼가 0 반환) InvalidInputException을 던진다")
    void approve_notPending_throwsInvalidInputException() {
        // SQL의 APPROVE_STATE='PENDING' 가드로 인해 PENDING이 아닌 페이지는 affected row 0을 반환한다.
        ReactCmsAdminApprovalApproveRequest req = new ReactCmsAdminApprovalApproveRequest();
        given(reactCmsAdminApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(reactCmsAdminApprovalMapper.approve(PAGE_ID, null, null, MODIFIER_ID))
                .willReturn(0);

        assertThatThrownBy(() -> reactCmsAdminApprovalService.approve(PAGE_ID, req, MODIFIER_ID))
                .isInstanceOf(InvalidInputException.class);
        // 0 반환 시 이력 INSERT는 호출되지 않아야 한다
        then(reactCmsAdminApprovalMapper).should(never()).insertHistory(any(), anyInt());
    }

    @Test
    @DisplayName("[승인] 노출 종료일이 시작일보다 빠르면 InvalidInputException을 던진다")
    void approve_invalidDisplayPeriod_throwsInvalidInputException() {
        ReactCmsAdminApprovalApproveRequest req = new ReactCmsAdminApprovalApproveRequest();
        req.setBeginningDate("2099-04-18");
        req.setExpiredDate("2099-04-17");
        given(reactCmsAdminApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);

        assertThatThrownBy(() -> reactCmsAdminApprovalService.approve(PAGE_ID, req, MODIFIER_ID))
                .isInstanceOf(InvalidInputException.class);
        // 날짜 검증 실패 시 mapper.approve는 호출되지 않아야 한다
        then(reactCmsAdminApprovalMapper).should(never()).approve(any(), any(), any(), any());
    }

    // ─── reject ──────────────────────────────────────────────────────

    @Test
    @DisplayName("[반려] 정상 반려 시 상태 변경 후 이력을 저장한다")
    void reject_normal_changesStateAndInsertsHistory() {
        ReactCmsAdminApprovalRejectRequest req = new ReactCmsAdminApprovalRejectRequest();
        req.setRejectedReason("내용 부적합");

        given(reactCmsAdminApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(reactCmsAdminApprovalMapper.reject(PAGE_ID, "내용 부적합", MODIFIER_ID))
                .willReturn(1);
        given(reactCmsAdminApprovalMapper.getNextVersion(PAGE_ID)).willReturn(2);

        reactCmsAdminApprovalService.reject(PAGE_ID, req, MODIFIER_ID);

        then(reactCmsAdminApprovalMapper).should().reject(eq(PAGE_ID), eq("내용 부적합"), eq(MODIFIER_ID));
        then(reactCmsAdminApprovalMapper).should().insertHistory(eq(PAGE_ID), eq(2));
    }

    @Test
    @DisplayName("[반려] 페이지가 없으면 NotFoundException을 던진다")
    void reject_pageNotFound_throwsNotFoundException() {
        ReactCmsAdminApprovalRejectRequest req = new ReactCmsAdminApprovalRejectRequest();
        req.setRejectedReason("이유");
        given(reactCmsAdminApprovalMapper.existsByPageId(PAGE_ID)).willReturn(0);

        assertThatThrownBy(() -> reactCmsAdminApprovalService.reject(PAGE_ID, req, MODIFIER_ID))
                .isInstanceOf(NotFoundException.class);
        then(reactCmsAdminApprovalMapper).should(never()).reject(any(), any(), any());
    }

    @Test
    @DisplayName("[반려] PENDING 상태가 아니면(매퍼가 0 반환) InvalidInputException을 던진다")
    void reject_notPending_throwsInvalidInputException() {
        ReactCmsAdminApprovalRejectRequest req = new ReactCmsAdminApprovalRejectRequest();
        req.setRejectedReason("이유");
        given(reactCmsAdminApprovalMapper.existsByPageId(PAGE_ID)).willReturn(1);
        given(reactCmsAdminApprovalMapper.reject(PAGE_ID, "이유", MODIFIER_ID)).willReturn(0);

        assertThatThrownBy(() -> reactCmsAdminApprovalService.reject(PAGE_ID, req, MODIFIER_ID))
                .isInstanceOf(InvalidInputException.class);
        then(reactCmsAdminApprovalMapper).should(never()).insertHistory(any(), anyInt());
    }
}
