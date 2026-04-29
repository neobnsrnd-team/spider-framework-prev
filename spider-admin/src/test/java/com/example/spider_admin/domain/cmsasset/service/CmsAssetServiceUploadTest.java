package com.example.spider_admin.domain.cmsasset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.example.spider_admin.domain.cmsasset.client.CmsBuilderClient;
import com.example.spider_admin.domain.cmsasset.client.dto.CmsBuilderUploadApiResponse;
import com.example.spider_admin.domain.cmsasset.dto.CmsAssetUploadResponse;
import com.example.spider_admin.domain.cmsasset.mapper.CmsAssetMapper;
import com.example.spider_admin.domain.cmsasset.validator.AssetUploadValidator;
import com.example.spider_admin.domain.code.dto.CodeResponse;
import com.example.spider_admin.domain.code.service.CodeService;
import com.example.spider_admin.global.exception.InvalidInputException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("CmsAssetService.uploadAsset 테스트 (#65)")
class CmsAssetServiceUploadTest {

    @Mock
    private CmsAssetMapper cmsAssetMapper;

    @Mock
    private CmsBuilderClient cmsBuilderClient;

    @Mock
    private AssetUploadValidator assetUploadValidator;

    @Mock
    private CodeService codeService;

    @InjectMocks
    private CmsAssetService cmsAssetService;

    private static final String USER_ID = "cmsUser01";
    private static final String USER_NAME = "CMS 현업01";

    /**
     * normalizeBusinessCategory()가 codeService.getCodesByCodeGroupId()를 호출하므로
     * 카테고리 검증을 통과하도록 stub 설정 — USE_YN='Y' 코드 포함
     */
    private void givenCategoryExists(String... codes) {
        List<CodeResponse> responses = java.util.Arrays.stream(codes)
                .map(c -> CodeResponse.builder().code(c).useYn("Y").build())
                .toList();
        given(codeService.getCodesByCodeGroupId(CmsAssetService.ASSET_CATEGORY_CODE_GROUP_ID))
                .willReturn(responses);
    }

    @Test
    @DisplayName("[업로드] 정상 흐름 — validator → CMS 호출 → CmsAssetUploadResponse 반환")
    void uploadAsset_happyPath_returnsResponse() {
        givenCategoryExists("카테고리A");
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1, 2, 3});
        given(cmsBuilderClient.upload(eq(file), eq("배너.png"), eq(USER_ID), eq(USER_NAME), eq("카테고리A"), eq("설명")))
                .willReturn(buildCmsResponse("uuid-1", "/static/a.png"));

        CmsAssetUploadResponse result = cmsAssetService.uploadAsset(file, "배너.png", "카테고리A", "설명", USER_ID, USER_NAME);

        assertThat(result.getAssetId()).isEqualTo("uuid-1");
        assertThat(result.getUrl()).isEqualTo("/static/a.png");
        then(assetUploadValidator).should().validate(file);
        then(cmsBuilderClient).should().upload(file, "배너.png", USER_ID, USER_NAME, "카테고리A", "설명");
    }

    @Test
    @DisplayName("[업로드] 이미지명이 200바이트 초과이면 CMS 호출하지 않고 400 예외 전파")
    void uploadAsset_assetNameTooLong_throwsInvalidInputException() {
        // 한글 1자 = 3바이트 → 68자 × 3 = 204바이트 > 200바이트
        String longName = "가".repeat(68) + ".png";
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1});

        assertThatThrownBy(() -> cmsAssetService.uploadAsset(file, longName, null, null, USER_ID, USER_NAME))
                .isInstanceOf(InvalidInputException.class);
        then(cmsBuilderClient).should(never()).upload(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("[업로드] Validator 가 실패하면 CMS 호출하지 않고 예외 전파")
    void uploadAsset_validatorFails_noCmsCall() {
        MockMultipartFile file = new MockMultipartFile("file", "a.exe", "application/x-msdownload", new byte[] {1});
        willThrow(new InvalidInputException("허용하지 않는 형식"))
                .given(assetUploadValidator)
                .validate(any(MultipartFile.class));

        assertThatThrownBy(() -> cmsAssetService.uploadAsset(file, "a.exe", null, null, USER_ID, USER_NAME))
                .isInstanceOf(InvalidInputException.class);
        then(cmsBuilderClient).should(never()).upload(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("[업로드] 공백 카테고리는 DEFAULT_BUSINESS_CATEGORY(COMMON)로 정규화되어 CMS 호출에 전달")
    void uploadAsset_blankCategory_normalizedToDefault() {
        // 공백 카테고리 → DEFAULT_BUSINESS_CATEGORY("COMMON")로 폴백 → 검증 통과 stub
        givenCategoryExists(CmsAssetService.DEFAULT_BUSINESS_CATEGORY);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[] {1});
        given(cmsBuilderClient.upload(any(), any(), any(), any(), any(), any()))
                .willReturn(buildCmsResponse("uuid-2", "/static/b.png"));

        cmsAssetService.uploadAsset(file, "a.png", "   ", "", USER_ID, USER_NAME);

        // 공백 businessCategory 는 normalizeBusinessCategory() 에서 COMMON 으로 정규화된다.
        // 빈 문자열 assetDesc 는 null/blank 체크 없이 그대로 CMS 로 전달된다.
        then(cmsBuilderClient)
                .should()
                .upload(file, "a.png", USER_ID, USER_NAME, CmsAssetService.DEFAULT_BUSINESS_CATEGORY, "");
    }

    private CmsBuilderUploadApiResponse buildCmsResponse(String assetId, String url) {
        CmsBuilderUploadApiResponse r = new CmsBuilderUploadApiResponse();
        r.setAssetId(assetId);
        r.setUrl(url);
        return r;
    }
}
