package com.example.spider_admin.domain.cmsdeployment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.spider_admin.domain.cmsdeployment.config.CmsDeployProperties;
import com.example.spider_admin.domain.cmsdeployment.dto.CmsDeployHistoryRequest;
import com.example.spider_admin.domain.cmsdeployment.dto.CmsDeployHistoryResponse;
import com.example.spider_admin.domain.cmsdeployment.dto.CmsDeployPageRequest;
import com.example.spider_admin.domain.cmsdeployment.dto.CmsDeployPageResponse;
import com.example.spider_admin.domain.cmsdeployment.mapper.CmsDeployMapper;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("CmsDeployService 테스트")
class CmsDeployServiceTest {

    @Mock
    private CmsDeployMapper cmsDeployMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CmsDeployProperties deployProperties;

    @InjectMocks
    private CmsDeployService cmsDeployService;

    private static final String PAGE_ID = "PAGE-001";
    private static final String USER_ID = "admin";
    private static final String HTML = "<html><body>test</body></html>";

    // ─── findApprovedPageList ──────────────────────────────────────────

    @Test
    @DisplayName("[조회] 배포 대상 페이지 목록 조회 시 PageResponse를 반환한다")
    void findApprovedPageList_returnsPageResponse() {
        CmsDeployPageRequest req = new CmsDeployPageRequest();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();
        List<CmsDeployPageResponse> data = List.of(buildPageResponse());

        given(cmsDeployMapper.countApprovedPageList(req)).willReturn(1L);
        given(cmsDeployMapper.findApprovedPageList(any(), anyLong(), anyLong())).willReturn(data);

        PageResponse<CmsDeployPageResponse> result = cmsDeployService.findApprovedPageList(req, pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPageId()).isEqualTo(PAGE_ID);
    }

    @Test
    @DisplayName("[조회] 결과가 없으면 빈 목록을 반환한다")
    void findApprovedPageList_empty_returnsEmptyContent() {
        CmsDeployPageRequest req = new CmsDeployPageRequest();
        PageRequest pageRequest = PageRequest.builder().page(0).size(10).build();

        given(cmsDeployMapper.countApprovedPageList(req)).willReturn(0L);
        given(cmsDeployMapper.findApprovedPageList(any(), anyLong(), anyLong())).willReturn(List.of());

        PageResponse<CmsDeployPageResponse> result = cmsDeployService.findApprovedPageList(req, pageRequest);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // ─── findHistoryList ───────────────────────────────────────────────

    @Test
    @DisplayName("[이력조회] 배포 이력 목록을 반환한다")
    void findHistoryList_returnsPageResponse() {
        CmsDeployHistoryRequest req = new CmsDeployHistoryRequest();
        req.setPageId(PAGE_ID);
        PageRequest pageRequest = PageRequest.builder().page(0).size(20).build();
        List<CmsDeployHistoryResponse> data = List.of(buildHistoryResponse());

        given(cmsDeployMapper.countHistoryList(req)).willReturn(1L);
        given(cmsDeployMapper.findHistoryList(any(), anyLong(), anyLong())).willReturn(data);

        PageResponse<CmsDeployHistoryResponse> result = cmsDeployService.findHistoryList(req, pageRequest);

        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFileId()).isEqualTo(PAGE_ID + "_v1.html");
    }

    @Test
    @DisplayName("[이력조회] 이력이 없으면 빈 목록을 반환한다")
    void findHistoryList_empty_returnsEmptyContent() {
        CmsDeployHistoryRequest req = new CmsDeployHistoryRequest();
        PageRequest pageRequest = PageRequest.builder().page(0).size(20).build();

        given(cmsDeployMapper.countHistoryList(req)).willReturn(0L);
        given(cmsDeployMapper.findHistoryList(any(), anyLong(), anyLong())).willReturn(List.of());

        PageResponse<CmsDeployHistoryResponse> result = cmsDeployService.findHistoryList(req, pageRequest);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // ─── push ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("[배포] 정상 배포 시 CMS push API를 호출한다")
    @SuppressWarnings("unchecked")
    void push_success_callsCmsPushApi() {
        given(cmsDeployMapper.findApprovedPageHtml(PAGE_ID)).willReturn(HTML);
        given(deployProperties.getPushUrl()).willReturn("http://cms/api/deploy/push");
        given(deployProperties.getSecret()).willReturn("secret-token");
        given(restTemplate.exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class)))
                .willReturn(ResponseEntity.ok(Map.of("ok", Boolean.TRUE)));

        cmsDeployService.push(PAGE_ID, USER_ID);

        then(restTemplate).should().exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    @DisplayName("[배포] CMS push API가 ok=false 응답 시 InternalException을 던진다")
    @SuppressWarnings("unchecked")
    void push_cmsPushReturnsError_throwsInternalException() {
        given(cmsDeployMapper.findApprovedPageHtml(PAGE_ID)).willReturn(HTML);
        given(deployProperties.getPushUrl()).willReturn("http://cms/api/deploy/push");
        given(deployProperties.getSecret()).willReturn("secret-token");
        given(restTemplate.exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class)))
                .willReturn(ResponseEntity.ok(Map.of("ok", Boolean.FALSE, "error", "배포 실패")));

        assertThatThrownBy(() -> cmsDeployService.push(PAGE_ID, USER_ID)).isInstanceOf(InternalException.class);
    }

    @Test
    @DisplayName("[배포] CMS push API 네트워크 오류 시 InternalException을 던진다")
    @SuppressWarnings("unchecked")
    void push_cmsPushThrowsException_throwsInternalException() {
        given(cmsDeployMapper.findApprovedPageHtml(PAGE_ID)).willReturn(HTML);
        given(deployProperties.getPushUrl()).willReturn("http://cms/api/deploy/push");
        given(deployProperties.getSecret()).willReturn("secret-token");
        given(restTemplate.exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class)))
                .willThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> cmsDeployService.push(PAGE_ID, USER_ID)).isInstanceOf(InternalException.class);
    }

    @Test
    @DisplayName("[배포] 승인된 페이지가 없으면 NotFoundException을 던진다")
    void push_pageNotFound_throwsNotFoundException() {
        given(cmsDeployMapper.findApprovedPageHtml(PAGE_ID)).willReturn(null);

        assertThatThrownBy(() -> cmsDeployService.push(PAGE_ID, USER_ID)).isInstanceOf(NotFoundException.class);
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private CmsDeployPageResponse buildPageResponse() {
        return CmsDeployPageResponse.builder()
                .pageId(PAGE_ID)
                .pageName("테스트 페이지")
                .createUserName("홍길동")
                .deployedUrl("http://133.186.135.23:8080/cms/deployed/" + PAGE_ID + ".html")
                .build();
    }

    private CmsDeployHistoryResponse buildHistoryResponse() {
        return CmsDeployHistoryResponse.builder()
                .instanceId("INST-001")
                .instanceName("배포서버-1")
                .instanceIp("133.186.135.23")
                .instancePort("8080")
                .fileId(PAGE_ID + "_v1.html")
                .fileSize(1024L)
                .fileCrcValue("abc123def456abcd")
                .lastModifierId(USER_ID)
                .lastModifiedDtime("20260416120000")
                .build();
    }
}
