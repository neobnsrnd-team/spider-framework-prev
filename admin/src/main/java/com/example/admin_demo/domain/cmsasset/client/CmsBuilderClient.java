package com.example.admin_demo.domain.cmsasset.client;

import com.example.admin_demo.domain.cmsasset.client.dto.CmsBuilderUploadApiResponse;
import com.example.admin_demo.domain.cmsasset.config.CmsBuilderProperties;
import com.example.admin_demo.global.exception.ErrorType;
import com.example.admin_demo.global.exception.base.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

/**
 * CMS Builder /cms/api/builder/upload 호출 클라이언트 — Issue #65.
 *
 * <p>Spider Admin 은 파일을 저장하지 않고 CMS 로 포워딩한다.
 * CMS 가 파일 저장 + SPW_CMS_ASSET INSERT 를 모두 수행하며, Admin 은 응답만 전달받는다.
 *
 * <p>CMS 가 실패 시에도 HTTP 200 을 반환하므로 응답 body 의 {@code ok} 필드로 판단한다.
 * 네트워크 오류 / 비정상 응답은 {@link BaseException} (HTTP 502) 로 래핑해 상위에 전파.
 */
@Slf4j
@Component
public class CmsBuilderClient {

    /**
     * CMS 자산 삭제 엔드포인트 경로 템플릿.
     * RESTful 고정 경로라 외부 설정화 이점이 크지 않아 상수로 둔다. 필요 시 baseUrl 과 함께 프로퍼티로 이동.
     */
    private static final String DELETE_PATH_TEMPLATE = "/cms/api/assets/{assetId}";

    /** CMS 자산 배포(승인 후 파일 이동) 엔드포인트 경로 템플릿. */
    private static final String DEPLOY_PATH_TEMPLATE = "/cms/api/assets/{assetId}/deploy";

    private final RestClient cmsBuilderRestClient;

    /**
     * 배포(deploy) 전용 RestClient — 업로드보다 훨씬 짧은 read-timeout 적용.
     * deploy 실패 시 saga 보상 UPDATE 도 있어 HTTP 대기를 짧게 끊어주는 것이 전체 응답성에 중요.
     */
    private final RestClient cmsBuilderDeployRestClient;

    private final CmsBuilderProperties properties;

    public CmsBuilderClient(
            @Qualifier("cmsBuilderRestClient") RestClient cmsBuilderRestClient,
            @Qualifier("cmsBuilderDeployRestClient") RestClient cmsBuilderDeployRestClient,
            CmsBuilderProperties properties) {
        this.cmsBuilderRestClient = cmsBuilderRestClient;
        this.cmsBuilderDeployRestClient = cmsBuilderDeployRestClient;
        this.properties = properties;
    }

    /**
     * CMS Builder 로 이미지 파일을 업로드한다.
     *
     * @param file             원본 이미지
     * @param userId           업로더 ID
     * @param userName         업로더 이름
     * @param businessCategory 업무 카테고리 (선택)
     * @param assetDesc        이미지 설명 (선택)
     * @return CMS 응답 (assetId, url 포함)
     * @throws BaseException 업로드 실패 또는 CMS 응답 오류 시 (HTTP 502)
     */
    public CmsBuilderUploadApiResponse upload(
            MultipartFile file, String userId, String userName, String businessCategory, String assetDesc) {

        MultiValueMap<String, Object> form = buildFormData(file, userId, userName, businessCategory, assetDesc);

        try {
            // cmsBuilderRestClient 사용 — 대용량 업로드를 위해 60초 read-timeout 유지 (#177)
            // deploy 클라이언트(10초)는 파일 이동 전용이므로 업로드에 사용하면 타임아웃 위험이 있다.
            // x-deploy-token은 빈 설정의 defaultHeader로 주입되므로 여기서 별도 지정하지 않는다.
            CmsBuilderUploadApiResponse response = cmsBuilderRestClient
                    .post()
                    .uri(properties.getUploadPath())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(CmsBuilderUploadApiResponse.class);

            if (response == null) {
                log.error("CMS Builder 응답 body 가 null. path={}", properties.getUploadPath());
                throw new BaseException(ErrorType.EXTERNAL_SERVICE_ERROR, "CMS 응답이 비어 있습니다.");
            }
            if (!response.isSuccess()) {
                String errMsg = response.getError() != null ? response.getError() : "CMS 업로드 실패";
                log.warn("CMS Builder 업로드 실패 응답: ok={}, error={}, userId={}", response.getOk(), errMsg, userId);
                throw new BaseException(ErrorType.EXTERNAL_SERVICE_ERROR, errMsg);
            }

            log.info(
                    "CMS Builder 업로드 성공: assetId={}, url={}, userId={}",
                    response.getAssetId(),
                    response.getUrl(),
                    userId);
            return response;

        } catch (RestClientException e) {
            log.error("CMS Builder 호출 중 오류 발생: userId={}", userId, e);
            throw new BaseException(ErrorType.EXTERNAL_SERVICE_ERROR, "CMS 서버와 통신할 수 없습니다. 잠시 후 다시 시도하세요.", e);
        }
    }

    /**
     * CMS Builder 에 이미지 자산 삭제를 요청한다 — Issue #88.
     *
     * <p>DELETE 엔드포인트는 성공 시 빈 body(204) 또는 비JSON body 를 반환할 수 있어
     * 업로드와 달리 body 파싱을 하지 않는다. HTTP status 기반으로 판정하며,
     * 2xx 이면 성공, 4xx/5xx 는 {@link RestClientException} 으로 전파되어 {@link BaseException}(502) 로 래핑된다.
     *
     * @param assetId 삭제 대상 자산 식별자 (CMS 가 발급한 UUID)
     * @param userId  삭제 수행자 ID (로그용)
     * @throws BaseException 삭제 실패 시 (HTTP 502)
     */
    public void delete(String assetId, String userId) {
        try {
            // x-deploy-token은 빈 설정의 defaultHeader로 주입되므로 여기서 별도 지정하지 않는다.
            cmsBuilderRestClient
                    .delete()
                    .uri(DELETE_PATH_TEMPLATE, assetId)
                    .retrieve()
                    .toBodilessEntity();

            log.info("CMS Builder 삭제 성공: assetId={}, userId={}", assetId, userId);

        } catch (RestClientException e) {
            log.error("CMS Builder 삭제 호출 중 오류 발생: assetId={}, userId={}", assetId, userId, e);
            throw new BaseException(ErrorType.EXTERNAL_SERVICE_ERROR, "CMS 서버와 통신할 수 없습니다. 잠시 후 다시 시도하세요.", e);
        }
    }

    /**
     * CMS Builder 에 이미지 승인 후 파일 배포(운영 경로로 이동)를 요청한다 — Issue #55.
     *
     * <p>Admin 의 승인 트랜잭션 내부에서 호출된다. 실패 시 {@link BaseException}(502) 을 던져
     * 호출자(@Transactional Service) 의 DB 커밋을 롤백시키는 구조이므로, 삭제와 마찬가지로
     * body 파싱은 생략하고 HTTP status 기반으로 판정한다.
     *
     * <p>성공 응답은 {@code {"ok":true,"data":{"url":...}}} 이나 Admin 은 반환 URL 을 사용하지 않는다
     * (CMS 가 {@code SPW_CMS_ASSET.ASSET_URL} 을 직접 갱신).
     *
     * @param assetId 배포 대상 자산 식별자
     * @throws BaseException 배포 실패 시 (HTTP 502)
     */
    /**
     * CMS 이미지를 바이트 배열로 가져온다.
     *
     * <p>CMS의 {@code GET /cms/api/assets/{assetId}/image} 는 DB에서 현재 파일 위치를 읽어 302 redirect 한다.
     * 미승인(임시 경로)·승인(운영 경로) 상태 구분 없이 동일 URL로 접근 가능하다.
     * Admin → CMS 서버 간 호출이므로 x-deploy-token 인증(defaultHeader 주입)을 사용한다.
     *
     * @param assetId 이미지 자산 ID
     * @return 이미지 바이트 및 Content-Type. CMS 오류 시 빈 404 반환
     */
    public ResponseEntity<byte[]> fetchImage(String assetId) {
        try {
            return cmsBuilderDeployRestClient
                    .get()
                    .uri("/cms/api/assets/{assetId}/image", assetId)
                    .retrieve()
                    .toEntity(byte[].class);
        } catch (RestClientException e) {
            log.warn("CMS 이미지 조회 실패: assetId={}", assetId, e);
            return ResponseEntity.notFound().build();
        }
    }

    public void deployAsset(String assetId) {
        try {
            cmsBuilderDeployRestClient
                    .post()
                    .uri(DEPLOY_PATH_TEMPLATE, assetId)
                    .retrieve()
                    .toBodilessEntity();

            log.info("CMS Builder 파일 배포 성공: assetId={}", assetId);

        } catch (RestClientException e) {
            log.error("CMS Builder 파일 배포 호출 중 오류 발생: assetId={}", assetId, e);
            throw new BaseException(ErrorType.EXTERNAL_SERVICE_ERROR, "CMS 서버와 통신할 수 없습니다. 잠시 후 다시 시도하세요.", e);
        }
    }

    /** 멀티파트 form-data 구성. file 은 파일명·Content-Type 을 보존하여 전송한다. */
    private MultiValueMap<String, Object> buildFormData(
            MultipartFile file, String userId, String userName, String businessCategory, String assetDesc) {

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", toFilePart(file));
        if (userId != null) {
            form.add("userId", userId);
        }
        if (userName != null) {
            form.add("userName", userName);
        }
        if (businessCategory != null && !businessCategory.isBlank()) {
            form.add("businessCategory", businessCategory);
        }
        if (assetDesc != null && !assetDesc.isBlank()) {
            form.add("assetDesc", assetDesc);
        }
        return form;
    }

    /**
     * MultipartFile 을 RestClient 전송 가능한 HttpEntity(Resource) 로 변환.
     *
     * <p>{@link MultipartFile#getResource()} 는 내부 저장소(임시 파일·InputStream)를 직접 참조하는
     * Resource 를 반환하므로 전체 바이트를 힙에 복사하지 않는다. 20MB 다건 업로드 시 OOM·GC 압박을 피하기 위함.
     * 반환되는 {@code MultipartFileResource} 는 원본 파일명을 그대로 노출한다.
     */
    private HttpEntity<Resource> toFilePart(MultipartFile file) {
        HttpHeaders partHeaders = new HttpHeaders();
        if (file.getContentType() != null) {
            partHeaders.setContentType(MediaType.parseMediaType(file.getContentType()));
        } else {
            partHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }
        return new HttpEntity<>(file.getResource(), partHeaders);
    }
}
