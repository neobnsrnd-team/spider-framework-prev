/**
 * @file ReactApprovalServiceTest.java
 * @description ReactApprovalService 단위 테스트.
 *     승인·반려 워크플로우, 자기승인 방지, Race Condition 처리,
 *     afterCommit 타이밍, 이력 조회 입력값 검증을 Mockito로 검증한다.
 * @see ReactApprovalService
 */
package com.example.reactplatform.domain.reactapproval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.reactplatform.domain.reactdeploy.service.ReactDeployService;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateApprovalResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateHistoryResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateResponse;
import com.example.reactplatform.domain.reactgenerate.enums.ReactGenerateStatus;
import com.example.reactplatform.domain.reactgenerate.mapper.ReactGenerateMapper;
import com.example.reactplatform.global.exception.base.BaseException;
import com.example.reactplatform.global.exception.InvalidInputException;
import com.example.reactplatform.global.exception.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ReactApprovalServiceTest {

    @Mock
    private ReactGenerateMapper reactGenerateMapper;

    @Mock
    private ReactDeployService reactDeployService;

    @InjectMocks
    private ReactApprovalService service;

    // ========== approve — 성공 경로 ==========

    @Nested
    @DisplayName("approve()")
    class Approve {

        @Test
        @DisplayName("정상 승인 시 APPROVED 상태와 승인자 정보를 담은 응답을 반환한다")
        void approve_success_returnsApprovedResponse() {
            try (MockedStatic<TransactionSynchronizationManager> mocked =
                    mockStatic(TransactionSynchronizationManager.class)) {
                mocked.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                        .thenAnswer(invocation -> null);
                when(reactGenerateMapper.selectById("code-01")).thenReturn(pendingRecord("creator"));
                when(reactGenerateMapper.updateStatusConditional(
                                anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                        .thenReturn(1);

                ReactGenerateApprovalResponse result = service.approve("code-01", "approver");

                assertThat(result.getCodeId()).isEqualTo("code-01");
                assertThat(result.getStatus()).isEqualTo(ReactGenerateStatus.APPROVED.name());
                assertThat(result.getApprovalUserId()).isEqualTo("approver");
                assertThat(result.getApprovalDtime()).isNotBlank();
            }
        }

        @Test
        @DisplayName("approve() 직후에는 deployAndRecord()가 호출되지 않고 afterCommit() 이후 호출된다")
        void approve_deployCalledAfterTransactionCommit_notBefore() {
            try (MockedStatic<TransactionSynchronizationManager> mocked =
                    mockStatic(TransactionSynchronizationManager.class)) {
                ArgumentCaptor<TransactionSynchronization> syncCaptor =
                        ArgumentCaptor.forClass(TransactionSynchronization.class);
                mocked.when(() ->
                                TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()))
                        .thenAnswer(invocation -> null);

                ReactGenerateResponse pending = pendingRecord("creator");
                pending.setReactCode("export default function Foo() {}");
                when(reactGenerateMapper.selectById("code-01")).thenReturn(pending);
                when(reactGenerateMapper.updateStatusConditional(
                                anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                        .thenReturn(1);

                service.approve("code-01", "approver");

                // approve() 반환 직후 — deployAndRecord 미호출 확인
                verify(reactDeployService, never()).deployAndRecord(any(), any(), any());

                // 트랜잭션 커밋 시뮬레이션
                syncCaptor.getValue().afterCommit();

                // afterCommit() 이후 — deployAndRecord 호출 확인
                verify(reactDeployService).deployAndRecord(
                        eq("code-01"),
                        eq("export default function Foo() {}"),
                        eq("approver"));
            }
        }

        @Test
        @DisplayName("존재하지 않는 codeId 승인 시 NotFoundException이 발생하고 deployAndRecord는 실행되지 않는다")
        void approve_notFound_throwsNotFoundException() {
            when(reactGenerateMapper.selectById("unknown")).thenReturn(null);

            assertThatThrownBy(() -> service.approve("unknown", "approver"))
                    .isInstanceOf(NotFoundException.class);
            verify(reactDeployService, never()).deployAndRecord(any(), any(), any());
        }

        @Test
        @DisplayName("PENDING_APPROVAL이 아닌 상태의 코드 승인 시 InvalidInputException이 발생한다")
        void approve_notPendingApproval_throwsInvalidInputException() {
            ReactGenerateResponse approved = ReactGenerateResponse.builder()
                    .codeId("code-01")
                    .status(ReactGenerateStatus.APPROVED.name())
                    .createUserId("creator")
                    .build();
            when(reactGenerateMapper.selectById("code-01")).thenReturn(approved);

            assertThatThrownBy(() -> service.approve("code-01", "approver"))
                    .isInstanceOf(InvalidInputException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getDetailMessage()).contains("승인 대기"));
        }

        @Test
        @DisplayName("코드 요청자 본인이 승인 시도하면 InvalidInputException이 발생한다")
        void approve_selfApproval_throwsInvalidInputException() {
            when(reactGenerateMapper.selectById("code-01")).thenReturn(pendingRecord("sameUser"));

            assertThatThrownBy(() -> service.approve("code-01", "sameUser"))
                    .isInstanceOf(InvalidInputException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getDetailMessage()).contains("요청자"));
        }

        @Test
        @DisplayName("동시 요청으로 updateStatusConditional이 0을 반환하면 InvalidInputException이 발생한다")
        void approve_raceCondition_updateReturnsZero_throwsInvalidInputException() {
            try (MockedStatic<TransactionSynchronizationManager> mocked =
                    mockStatic(TransactionSynchronizationManager.class)) {
                mocked.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                        .thenAnswer(invocation -> null);
                when(reactGenerateMapper.selectById("code-01")).thenReturn(pendingRecord("creator"));
                when(reactGenerateMapper.updateStatusConditional(
                                anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                        .thenReturn(0);

                assertThatThrownBy(() -> service.approve("code-01", "approver"))
                        .isInstanceOf(InvalidInputException.class)
                        .satisfies(ex -> assertThat(((BaseException) ex).getDetailMessage()).contains("이미 처리된"));
            }
        }

        @Test
        @DisplayName("updateStatusConditional은 requiredStatus=PENDING_APPROVAL 조건으로 호출된다")
        void approve_conditionalUpdate_requiresPendingApprovalStatus() {
            try (MockedStatic<TransactionSynchronizationManager> mocked =
                    mockStatic(TransactionSynchronizationManager.class)) {
                mocked.when(() -> TransactionSynchronizationManager.registerSynchronization(any()))
                        .thenAnswer(invocation -> null);
                when(reactGenerateMapper.selectById("code-01")).thenReturn(pendingRecord("creator"));
                when(reactGenerateMapper.updateStatusConditional(
                                anyString(), anyString(), anyString(), anyString(), any(), anyString()))
                        .thenReturn(1);

                service.approve("code-01", "approver");

                verify(reactGenerateMapper)
                        .updateStatusConditional(
                                eq("code-01"),
                                eq(ReactGenerateStatus.APPROVED.name()),
                                eq("approver"),
                                anyString(),
                                isNull(),
                                eq(ReactGenerateStatus.PENDING_APPROVAL.name()));
            }
        }
    }

    // ========== reject ==========

    @Nested
    @DisplayName("reject()")
    class Reject {

        @Test
        @DisplayName("정상 반려 시 REJECTED 상태와 반려자 정보를 담은 응답을 반환한다")
        void reject_success_returnsRejectedResponse() {
            when(reactGenerateMapper.selectById("code-01")).thenReturn(pendingRecord("creator"));

            ReactGenerateApprovalResponse result = service.reject("code-01", "rejector", "요건 불충족");

            verify(reactGenerateMapper)
                    .updateStatus(
                            eq("code-01"),
                            eq(ReactGenerateStatus.REJECTED.name()),
                            eq("rejector"),
                            anyString(),
                            eq("요건 불충족"));
            assertThat(result.getStatus()).isEqualTo(ReactGenerateStatus.REJECTED.name());
            assertThat(result.getApprovalUserId()).isEqualTo("rejector");
        }

        @Test
        @DisplayName("reason이 null이어도 반려는 정상 처리된다")
        void reject_nullReason_succeeds() {
            when(reactGenerateMapper.selectById("code-01")).thenReturn(pendingRecord("creator"));

            assertThatNoException().isThrownBy(() -> service.reject("code-01", "rejector", null));
            verify(reactGenerateMapper).updateStatus(anyString(), anyString(), anyString(), anyString(), isNull());
        }

        @Test
        @DisplayName("존재하지 않는 codeId 반려 시 NotFoundException이 발생한다")
        void reject_notFound_throwsNotFoundException() {
            when(reactGenerateMapper.selectById("unknown")).thenReturn(null);

            assertThatThrownBy(() -> service.reject("unknown", "rejector", null))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ========== getHistory — 입력값 검증 ==========

    @Nested
    @DisplayName("getHistory()")
    class GetHistory {

        @Test
        @DisplayName("날짜 형식이 yyyyMMdd가 아니면 InvalidInputException이 발생한다")
        void getHistory_invalidDateFormat_throwsInvalidInputException() {
            assertThatThrownBy(
                            () -> service.getHistory(1, 10, null, null, null, null, null, "2024-01-01", null))
                    .isInstanceOf(InvalidInputException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getDetailMessage()).contains("yyyyMMdd"));
        }

        @Test
        @DisplayName("8자리 숫자 형식이 아닌 날짜도 거부된다")
        void getHistory_nonNumericDate_throwsInvalidInputException() {
            assertThatThrownBy(
                            () -> service.getHistory(1, 10, null, null, null, null, null, null, "abcdefgh"))
                    .isInstanceOf(InvalidInputException.class);
        }

        @Test
        @DisplayName("fromDate가 toDate보다 이후이면 InvalidInputException이 발생한다")
        void getHistory_dateRangeReversed_throwsInvalidInputException() {
            assertThatThrownBy(
                            () -> service.getHistory(
                                    1, 10, null, null, null, null, null, "20240201", "20240101"))
                    .isInstanceOf(InvalidInputException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getDetailMessage()).contains("이전"));
        }

        @Test
        @DisplayName("fromDate와 toDate가 같으면 정상 처리된다")
        void getHistory_sameDates_succeeds() {
            when(reactGenerateMapper.selectApprovalHistory(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());
            when(reactGenerateMapper.selectApprovalHistoryCount(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(0);

            assertThatNoException()
                    .isThrownBy(() -> service.getHistory(1, 10, null, null, null, null, null, "20240101", "20240101"));
        }

        @Test
        @DisplayName("APPROVED / REJECTED 이외의 status 값이면 InvalidInputException이 발생한다")
        void getHistory_invalidStatus_throwsInvalidInputException() {
            assertThatThrownBy(
                            () -> service.getHistory(1, 10, "PENDING_APPROVAL", null, null, null, null, null, null))
                    .isInstanceOf(InvalidInputException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getDetailMessage()).contains("APPROVED"));
        }

        @Test
        @DisplayName("status=APPROVED이면 정상 처리된다")
        void getHistory_validStatusApproved_succeeds() {
            when(reactGenerateMapper.selectApprovalHistory(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());
            when(reactGenerateMapper.selectApprovalHistoryCount(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(0);

            assertThatNoException()
                    .isThrownBy(() -> service.getHistory(1, 10, "APPROVED", null, null, null, null, null, null));
        }

        @Test
        @DisplayName("%, _, \\이 포함된 입력은 이스케이프되어 mapper에 전달된다")
        void getHistory_likeWildcard_escapedBeforePassedToMapper() {
            when(reactGenerateMapper.selectApprovalHistory(
                            anyInt(), anyInt(), any(), any(), any(), anyString(), anyString(), any(), any()))
                    .thenReturn(List.of());
            when(reactGenerateMapper.selectApprovalHistoryCount(
                            any(), any(), any(), anyString(), anyString(), any(), any()))
                    .thenReturn(0);

            service.getHistory(1, 10, null, null, null, "user%01", "dept_A", null, null);

            verify(reactGenerateMapper)
                    .selectApprovalHistory(
                            anyInt(), anyInt(), isNull(), isNull(), isNull(), eq("user\\%01"), eq("dept\\_A"), isNull(), isNull());
        }

        @Test
        @DisplayName("\\을 포함한 입력은 \\\\로 이스케이프된다")
        void getHistory_backslash_doubleEscaped() {
            when(reactGenerateMapper.selectApprovalHistory(
                            anyInt(), anyInt(), any(), any(), any(), anyString(), any(), any(), any()))
                    .thenReturn(List.of());
            when(reactGenerateMapper.selectApprovalHistoryCount(
                            any(), any(), any(), anyString(), any(), any(), any()))
                    .thenReturn(0);

            service.getHistory(1, 10, null, null, null, "user\\01", null, null, null);

            verify(reactGenerateMapper)
                    .selectApprovalHistory(
                            anyInt(), anyInt(), isNull(), isNull(), isNull(), eq("user\\\\01"), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("빈 문자열 필터는 null로 변환되어 전체 조회로 동작한다")
        void getHistory_blankFilters_convertedToNull() {
            when(reactGenerateMapper.selectApprovalHistory(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());
            when(reactGenerateMapper.selectApprovalHistoryCount(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(0);

            service.getHistory(1, 10, "", "", "", "  ", "", "", "");

            verify(reactGenerateMapper)
                    .selectApprovalHistory(
                            anyInt(), anyInt(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull());
        }

        @Test
        @DisplayName("결과를 list, totalCount, page, size를 포함한 Map으로 반환한다")
        void getHistory_returnsMapWithExpectedKeys() {
            when(reactGenerateMapper.selectApprovalHistory(
                            anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of(new ReactGenerateHistoryResponse()));
            when(reactGenerateMapper.selectApprovalHistoryCount(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(1);

            var result = service.getHistory(2, 5, null, null, null, null, null, null, null);

            assertThat(result).containsKeys("list", "totalCount", "page", "size");
            assertThat(result.get("page")).isEqualTo(2);
            assertThat(result.get("size")).isEqualTo(5);
            assertThat(result.get("totalCount")).isEqualTo(1);
        }
    }

    // ========== helpers ==========

    /** PENDING_APPROVAL 상태의 테스트용 레코드를 생성한다. */
    private ReactGenerateResponse pendingRecord(String createUserId) {
        return ReactGenerateResponse.builder()
                .codeId("code-01")
                .status(ReactGenerateStatus.PENDING_APPROVAL.name())
                .createUserId(createUserId)
                .reactCode("export default function TestComp() {}")
                .build();
    }
}