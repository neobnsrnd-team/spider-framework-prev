package com.example.spider_admin.domain.cmsasset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.example.spider_admin.domain.cmsasset.dto.CmsAssetApprovalListRequest;
import com.example.spider_admin.domain.cmsasset.dto.CmsAssetDetailResponse;
import com.example.spider_admin.domain.cmsasset.dto.CmsAssetListResponse;
import com.example.spider_admin.domain.cmsasset.dto.CmsAssetRequestListRequest;
import com.example.spider_admin.domain.cmsasset.mapper.CmsAssetMapper;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.ErrorType;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.InvalidStateException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.exception.base.BaseException;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CmsAssetService 테스트")
class CmsAssetServiceTest {

    @Mock
    private CmsAssetMapper cmsAssetMapper;

    @Mock
    private com.example.spider_admin.domain.cmsasset.client.CmsBuilderClient cmsBuilderClient;

    @Mock
    private com.example.spider_admin.domain.cmsasset.validator.AssetUploadValidator assetUploadValidator;

    @Mock
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @InjectMocks
    private CmsAssetService cmsAssetService;

    /**
     * TransactionTemplate mock 이 받은 callback 을 동기 실행하도록 설정.
     * 실제 트랜잭션 경계는 단위 테스트 범위 밖이며, 여기서는 approve() 내부에서
     * UPDATE → CMS → (보상 UPDATE) 흐름이 호출되는지만 검증한다.
     */
    @BeforeEach
    void setUpTransactionTemplate() {
        // lenient — approve 외 테스트는 TransactionTemplate 를 사용하지 않아 strict stubbing 에 걸리는 것을 방지.
        lenient()
                .doAnswer(inv -> {
                    Consumer<org.springframework.transaction.TransactionStatus> cb = inv.getArgument(0);
                    cb.accept(null);
                    return null;
                })
                .when(transactionTemplate)
                .executeWithoutResult(any());
    }

    private static final String ASSET_ID = "ASSET-001";
    private static final String USER_ID = "cmsUser01";
    private static final String USER_NAME = "CMS 현업01";

    // ─── findMyRequestList ─────────────────────────────────────────────

    @Test
    @DisplayName("[내 이미지 조회] 클라이언트가 보낸 createUserId를 인증 주체 ID로 덮어쓴다")
    void findMyRequestList_forcesCurrentUserIdOverride() {
        CmsAssetRequestListRequest req = new CmsAssetRequestListRequest();
        req.setCreateUserId("attacker-id"); // 악의적 입력

        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();
        given(cmsAssetMapper.countMyList(any())).willReturn(1L);
        given(cmsAssetMapper.findMyList(any(), anyInt(), anyInt())).willReturn(List.of(buildListResponse("WORK")));

        PageResponse<CmsAssetListResponse> result = cmsAssetService.findMyRequestList(USER_ID, req, pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(1L);
        ArgumentCaptor<CmsAssetRequestListRequest> captor = ArgumentCaptor.forClass(CmsAssetRequestListRequest.class);
        then(cmsAssetMapper).should().countMyList(captor.capture());
        assertThat(captor.getValue().getCreateUserId()).isEqualTo(USER_ID);
    }

    // ─── findApprovalList ──────────────────────────────────────────────

    @Test
    @DisplayName("[승인 관리 조회] 결과가 없으면 빈 목록을 반환한다")
    void findApprovalList_empty_returnsEmptyContent() {
        CmsAssetApprovalListRequest req = new CmsAssetApprovalListRequest();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();
        given(cmsAssetMapper.countApprovalList(req)).willReturn(0L);
        given(cmsAssetMapper.findApprovalList(any(), anyInt(), anyInt())).willReturn(List.of());

        PageResponse<CmsAssetListResponse> result = cmsAssetService.findApprovalList(req, pageRequest);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // ─── findById ──────────────────────────────────────────────────────

    @Test
    @DisplayName("[상세 조회] 존재하지 않으면 NotFoundException")
    void findById_notFound_throws() {
        given(cmsAssetMapper.findDetailById(ASSET_ID)).willReturn(null);

        assertThatThrownBy(() -> cmsAssetService.findById(ASSET_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("[상세 조회] 정상 조회 시 DTO 반환")
    void findById_returnsDetail() {
        CmsAssetDetailResponse detail = CmsAssetDetailResponse.builder()
                .assetId(ASSET_ID)
                .assetState("PENDING")
                .build();
        given(cmsAssetMapper.findDetailById(ASSET_ID)).willReturn(detail);

        assertThat(cmsAssetService.findById(ASSET_ID).getAssetId()).isEqualTo(ASSET_ID);
    }

    // ─── requestApproval ───────────────────────────────────────────────

    @Test
    @DisplayName("[승인 요청] WORK → PENDING 정상 전이")
    void requestApproval_fromWork_succeeds() {
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("WORK");
        given(cmsAssetMapper.updateState(eq(ASSET_ID), eq("WORK"), eq("PENDING"), isNull(), eq(USER_ID), eq(USER_NAME)))
                .willReturn(1);

        cmsAssetService.requestApproval(ASSET_ID, USER_ID, USER_NAME);

        then(cmsAssetMapper).should().updateState(ASSET_ID, "WORK", "PENDING", null, USER_ID, USER_NAME);
    }

    @Test
    @DisplayName("[승인 요청] 존재하지 않으면 NotFoundException")
    void requestApproval_notFound_throws() {
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn(null);

        assertThatThrownBy(() -> cmsAssetService.requestApproval(ASSET_ID, USER_ID, USER_NAME))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("[승인 요청] PENDING 상태에서 재요청 시 InvalidStateException")
    void requestApproval_fromPending_throwsInvalidState() {
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("PENDING");

        assertThatThrownBy(() -> cmsAssetService.requestApproval(ASSET_ID, USER_ID, USER_NAME))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    @DisplayName("[승인 요청] UPDATE가 0행을 갱신하면 race 조건으로 InvalidStateException")
    void requestApproval_zeroRowUpdated_throwsInvalidState() {
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("WORK");
        given(cmsAssetMapper.updateState(any(), any(), any(), any(), any(), any()))
                .willReturn(0);

        assertThatThrownBy(() -> cmsAssetService.requestApproval(ASSET_ID, USER_ID, USER_NAME))
                .isInstanceOf(InvalidStateException.class);
    }

    // ─── approve ───────────────────────────────────────────────────────

    @Test
    @DisplayName("[승인] PENDING → APPROVED 전이 + CMS 배포 호출까지 성공")
    void approve_fromPending_succeedsAndDeploys() {
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("PENDING");
        given(cmsAssetMapper.updateState(any(), any(), any(), any(), any(), any()))
                .willReturn(1);

        cmsAssetService.approve(ASSET_ID, USER_ID, USER_NAME);

        then(cmsAssetMapper).should().updateState(ASSET_ID, "PENDING", "APPROVED", null, USER_ID, USER_NAME);
        then(cmsBuilderClient).should().deployAsset(ASSET_ID);
    }

    @Test
    @DisplayName("[승인] WORK 상태에서 승인 시 InvalidStateException, CMS 배포 호출 없음")
    void approve_fromWork_throwsInvalidState() {
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("WORK");

        assertThatThrownBy(() -> cmsAssetService.approve(ASSET_ID, USER_ID, USER_NAME))
                .isInstanceOf(InvalidStateException.class);
        then(cmsBuilderClient).should(never()).deployAsset(any());
    }

    @Test
    @DisplayName("[승인] CMS 배포 실패 시 보상 UPDATE(APPROVED→PENDING) 후 BaseException 재전파")
    void approve_cmsDeployFails_compensatesAndRethrows() {
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("PENDING");
        given(cmsAssetMapper.updateState(any(), any(), any(), any(), any(), any()))
                .willReturn(1);
        willThrow(new BaseException(ErrorType.EXTERNAL_SERVICE_ERROR, "CMS 통신 오류"))
                .given(cmsBuilderClient)
                .deployAsset(ASSET_ID);

        assertThatThrownBy(() -> cmsAssetService.approve(ASSET_ID, USER_ID, USER_NAME))
                .isInstanceOfSatisfying(BaseException.class, ex -> assertThat(ex.getErrorType())
                        .isEqualTo(ErrorType.EXTERNAL_SERVICE_ERROR));

        // 메인 UPDATE(PENDING→APPROVED) + 보상 UPDATE(APPROVED→PENDING) 두 번 호출됐어야 한다.
        then(cmsAssetMapper).should().updateState(ASSET_ID, "PENDING", "APPROVED", null, USER_ID, USER_NAME);
        then(cmsAssetMapper).should().updateState(ASSET_ID, "APPROVED", "PENDING", null, USER_ID, USER_NAME);
        then(cmsAssetMapper).should(times(2)).updateState(any(), any(), any(), any(), any(), any());
    }

    // ─── reject ────────────────────────────────────────────────────────

    @Test
    @DisplayName("[반려] 반려 사유 포함 정상 전이")
    void reject_withReason_persistsReason() {
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("PENDING");
        given(cmsAssetMapper.updateState(any(), any(), any(), any(), any(), any()))
                .willReturn(1);

        cmsAssetService.reject(ASSET_ID, "해상도 기준 미달", USER_ID, USER_NAME);

        then(cmsAssetMapper).should().updateState(ASSET_ID, "PENDING", "REJECTED", "해상도 기준 미달", USER_ID, USER_NAME);
    }

    @Test
    @DisplayName("[반려] 반려 사유가 null이면 null 그대로 전달된다 (선택 입력)")
    void reject_nullReason_ok() {
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("PENDING");
        given(cmsAssetMapper.updateState(any(), any(), any(), any(), any(), any()))
                .willReturn(1);

        cmsAssetService.reject(ASSET_ID, null, USER_ID, USER_NAME);

        then(cmsAssetMapper).should().updateState(ASSET_ID, "PENDING", "REJECTED", null, USER_ID, USER_NAME);
    }

    @Test
    @DisplayName("[반려] 공백만 있는 반려 사유는 null로 정규화된다")
    void reject_blankReason_normalizedToNull() {
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("PENDING");
        given(cmsAssetMapper.updateState(any(), any(), any(), any(), any(), any()))
                .willReturn(1);

        cmsAssetService.reject(ASSET_ID, "   ", USER_ID, USER_NAME);

        then(cmsAssetMapper).should().updateState(ASSET_ID, "PENDING", "REJECTED", null, USER_ID, USER_NAME);
    }

    @Test
    @DisplayName("[반려] 1000자 초과 반려 사유는 InvalidInputException")
    void reject_reasonTooLong_throwsInvalidInput() {
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("PENDING");
        String tooLong = "x".repeat(1001);

        assertThatThrownBy(() -> cmsAssetService.reject(ASSET_ID, tooLong, USER_ID, USER_NAME))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("[반려] UTF-8 1000바이트 초과(한글 334자 = 1002바이트) 반려 사유는 InvalidInputException")
    void reject_reasonTooManyBytes_throwsInvalidInput() {
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("PENDING");
        // '가' = UTF-8 3바이트, 334자 → 1002바이트. 문자 수 상한(1000자)은 통과하지만 바이트 상한은 초과한다.
        String koreanOverflow = "가".repeat(334);

        assertThatThrownBy(() -> cmsAssetService.reject(ASSET_ID, koreanOverflow, USER_ID, USER_NAME))
                .isInstanceOf(InvalidInputException.class);
    }

    // ─── helpers ───────────────────────────────────────────────────────

    private CmsAssetListResponse buildListResponse(String state) {
        return CmsAssetListResponse.builder()
                .assetId(ASSET_ID)
                .assetName("test.png")
                .assetState(state)
                .createUserId(USER_ID)
                .createUserName(USER_NAME)
                .build();
    }
}
