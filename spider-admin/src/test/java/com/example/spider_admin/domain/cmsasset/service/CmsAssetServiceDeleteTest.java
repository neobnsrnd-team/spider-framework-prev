package com.example.spider_admin.domain.cmsasset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.spider_admin.domain.cmsasset.client.CmsBuilderClient;
import com.example.spider_admin.domain.cmsasset.mapper.CmsAssetMapper;
import com.example.spider_admin.domain.cmsasset.validator.AssetUploadValidator;
import com.example.spider_admin.global.exception.ErrorType;
import com.example.spider_admin.global.exception.InvalidStateException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.exception.base.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CmsAssetService.deleteMyAsset 단위 테스트 — Issue #88.
 *
 * <p>검증 3단계(존재·소유자·상태) 와 CMS 위임 호출 여부를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CmsAssetService.deleteMyAsset 테스트 (#88)")
class CmsAssetServiceDeleteTest {

    @Mock
    private CmsAssetMapper cmsAssetMapper;

    @Mock
    private CmsBuilderClient cmsBuilderClient;

    @Mock
    private AssetUploadValidator assetUploadValidator;

    @InjectMocks
    private CmsAssetService cmsAssetService;

    private static final String ASSET_ID = "uuid-del-1";
    private static final String OWNER_ID = "cmsUser01";
    private static final String OTHER_ID = "cmsUser02";

    @Test
    @DisplayName("[삭제] 소유자 + WORK 상태 → CMS 삭제 호출")
    void delete_ownerWork_invokesCmsDelete() {
        given(cmsAssetMapper.findCreateUserIdByAssetId(ASSET_ID)).willReturn(OWNER_ID);
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("WORK");

        cmsAssetService.deleteMyAsset(ASSET_ID, OWNER_ID);

        then(cmsBuilderClient).should().delete(ASSET_ID, OWNER_ID);
    }

    @Test
    @DisplayName("[삭제] 소유자 + REJECTED 상태 → CMS 삭제 호출")
    void delete_ownerRejected_invokesCmsDelete() {
        given(cmsAssetMapper.findCreateUserIdByAssetId(ASSET_ID)).willReturn(OWNER_ID);
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("REJECTED");

        cmsAssetService.deleteMyAsset(ASSET_ID, OWNER_ID);

        then(cmsBuilderClient).should().delete(ASSET_ID, OWNER_ID);
    }

    @Test
    @DisplayName("[삭제] 존재하지 않는 assetId → NotFoundException, 후속 조회·CMS 호출 없음")
    void delete_notFound_throwsNotFound() {
        given(cmsAssetMapper.findCreateUserIdByAssetId(ASSET_ID)).willReturn(null);

        assertThatThrownBy(() -> cmsAssetService.deleteMyAsset(ASSET_ID, OWNER_ID))
                .isInstanceOf(NotFoundException.class);
        then(cmsAssetMapper).should(never()).findAssetStateById(any());
        then(cmsBuilderClient).should(never()).delete(any(), any());
    }

    @Test
    @DisplayName("[삭제] 소유자 아님 → FORBIDDEN BaseException, 상태 조회·CMS 호출 없음")
    void delete_notOwner_throwsForbidden() {
        given(cmsAssetMapper.findCreateUserIdByAssetId(ASSET_ID)).willReturn(OWNER_ID);

        assertThatThrownBy(() -> cmsAssetService.deleteMyAsset(ASSET_ID, OTHER_ID))
                .isInstanceOfSatisfying(
                        BaseException.class, ex -> assertThat(ex.getErrorType()).isEqualTo(ErrorType.FORBIDDEN));
        then(cmsAssetMapper).should(never()).findAssetStateById(any());
        then(cmsBuilderClient).should(never()).delete(any(), any());
    }

    @Test
    @DisplayName("[삭제] 소유자 + PENDING 상태 → InvalidStateException, CMS 호출 없음")
    void delete_ownerPending_throwsInvalidState() {
        given(cmsAssetMapper.findCreateUserIdByAssetId(ASSET_ID)).willReturn(OWNER_ID);
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("PENDING");

        assertThatThrownBy(() -> cmsAssetService.deleteMyAsset(ASSET_ID, OWNER_ID))
                .isInstanceOf(InvalidStateException.class);
        then(cmsBuilderClient).should(never()).delete(any(), any());
    }

    @Test
    @DisplayName("[삭제] 소유자 + APPROVED 상태 → InvalidStateException, CMS 호출 없음")
    void delete_ownerApproved_throwsInvalidState() {
        given(cmsAssetMapper.findCreateUserIdByAssetId(ASSET_ID)).willReturn(OWNER_ID);
        given(cmsAssetMapper.findAssetStateById(ASSET_ID)).willReturn("APPROVED");

        assertThatThrownBy(() -> cmsAssetService.deleteMyAsset(ASSET_ID, OWNER_ID))
                .isInstanceOf(InvalidStateException.class);
        then(cmsBuilderClient).should(never()).delete(any(), any());
    }
}
