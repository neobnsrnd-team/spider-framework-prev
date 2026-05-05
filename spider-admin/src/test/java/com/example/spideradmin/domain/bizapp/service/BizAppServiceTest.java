package com.example.spideradmin.domain.bizapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.spideradmin.domain.bizapp.dto.BizAppCreateRequest;
import com.example.spideradmin.domain.bizapp.dto.BizAppResponse;
import com.example.spideradmin.domain.bizapp.dto.BizAppSearchRequest;
import com.example.spideradmin.domain.bizapp.dto.BizAppUpdateRequest;
import com.example.spideradmin.domain.bizapp.mapper.BizAppMapper;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
@DisplayName("BizAppService 테스트")
class BizAppServiceTest {

    @Mock
    private BizAppMapper bizAppMapper;

    @InjectMocks
    private BizAppService bizAppService;

    // ─── getBizAppsWithSearch ─────────────────────────────────────────

    @Test
    @DisplayName("[조회] 검색 결과를 PageResponse로 반환해야 한다")
    void getBizAppsWithSearch_returnsPageResponse() {
        BizAppSearchRequest searchDTO =
                BizAppSearchRequest.builder().page(1).size(10).build();

        List<BizAppResponse> data = List.of(buildResponse("APP-001"), buildResponse("APP-002"));
        given(bizAppMapper.countAllWithSearch(any(), any(), any(), any())).willReturn(2L);
        given(bizAppMapper.findAllWithSearch(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .willReturn(data);

        PageResponse<BizAppResponse> result = bizAppService.getBizAppsWithSearch(searchDTO);

        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getBizAppId()).isEqualTo("APP-001");
    }

    @Test
    @DisplayName("[조회] 검색 결과가 없으면 빈 content를 반환해야 한다")
    void getBizAppsWithSearch_noResult_returnsEmptyContent() {
        BizAppSearchRequest searchDTO =
                BizAppSearchRequest.builder().page(1).size(10).build();

        given(bizAppMapper.countAllWithSearch(any(), any(), any(), any())).willReturn(0L);
        given(bizAppMapper.findAllWithSearch(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .willReturn(List.of());

        PageResponse<BizAppResponse> result = bizAppService.getBizAppsWithSearch(searchDTO);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // ─── getById ──────────────────────────────────────────────────────

    @Test
    @DisplayName("[조회] 존재하는 ID이면 BizAppResponse를 반환해야 한다")
    void getById_exists_returnsBizAppResponse() {
        given(bizAppMapper.selectResponseById("APP-001")).willReturn(buildResponse("APP-001"));

        BizAppResponse result = bizAppService.getById("APP-001");

        assertThat(result.getBizAppId()).isEqualTo("APP-001");
        assertThat(result.getBizAppName()).isEqualTo("테스트 App");
    }

    @Test
    @DisplayName("[조회] 존재하지 않는 ID이면 NotFoundException을 발생시켜야 한다")
    void getById_notExists_throwsNotFoundException() {
        given(bizAppMapper.selectResponseById("NOT-EXIST")).willReturn(null);

        assertThatThrownBy(() -> bizAppService.getById("NOT-EXIST")).isInstanceOf(NotFoundException.class);
    }

    // ─── create ───────────────────────────────────────────────────────

    @Test
    @DisplayName("[등록] 정상 등록 시 selectResponseById 결과를 반환해야 한다")
    void create_success_returnsBizAppResponse() {
        BizAppCreateRequest dto = buildCreateRequest("APP-NEW");
        given(bizAppMapper.selectResponseById("APP-NEW")).willReturn(buildResponse("APP-NEW"));

        BizAppResponse result = bizAppService.create(dto);

        assertThat(result.getBizAppId()).isEqualTo("APP-NEW");
        then(bizAppMapper).should().insert(eq(dto), anyString(), anyString());
    }

    @Test
    @DisplayName("[등록] 중복 ID이면 DuplicateException을 발생시켜야 한다")
    void create_duplicateId_throwsDuplicateException() {
        BizAppCreateRequest dto = buildCreateRequest("APP-DUP");
        org.mockito.BDDMockito.willThrow(new DuplicateKeyException("duplicate"))
                .given(bizAppMapper)
                .insert(any(), anyString(), anyString());

        assertThatThrownBy(() -> bizAppService.create(dto)).isInstanceOf(DuplicateException.class);
    }

    // ─── update ───────────────────────────────────────────────────────

    @Test
    @DisplayName("[수정] 존재하는 ID 수정 시 selectResponseById 결과를 반환해야 한다")
    void update_exists_returnsBizAppResponse() {
        BizAppUpdateRequest dto = buildUpdateRequest();
        given(bizAppMapper.countById("APP-001")).willReturn(1);
        given(bizAppMapper.selectResponseById("APP-001")).willReturn(buildResponse("APP-001"));

        BizAppResponse result = bizAppService.update("APP-001", dto);

        assertThat(result.getBizAppId()).isEqualTo("APP-001");
        then(bizAppMapper).should().update(eq("APP-001"), eq(dto), anyString(), anyString());
    }

    @Test
    @DisplayName("[수정] 존재하지 않는 ID 수정 시 NotFoundException을 발생시켜야 한다")
    void update_notExists_throwsNotFoundException() {
        given(bizAppMapper.countById("NOT-EXIST")).willReturn(0);
        BizAppUpdateRequest req = buildUpdateRequest();

        assertThatThrownBy(() -> bizAppService.update("NOT-EXIST", req)).isInstanceOf(NotFoundException.class);
    }

    // ─── delete ───────────────────────────────────────────────────────

    @Test
    @DisplayName("[삭제] 존재하는 ID 삭제 시 deleteById를 호출해야 한다")
    void delete_exists_callsDeleteById() {
        given(bizAppMapper.countById("APP-001")).willReturn(1);

        bizAppService.delete("APP-001");

        then(bizAppMapper).should().deleteById("APP-001");
    }

    @Test
    @DisplayName("[삭제] 존재하지 않는 ID 삭제 시 NotFoundException을 발생시켜야 한다")
    void delete_notExists_throwsNotFoundException() {
        given(bizAppMapper.countById("NOT-EXIST")).willReturn(0);

        assertThatThrownBy(() -> bizAppService.delete("NOT-EXIST")).isInstanceOf(NotFoundException.class);
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private BizAppResponse buildResponse(String bizAppId) {
        return BizAppResponse.builder()
                .bizAppId(bizAppId)
                .bizAppName("테스트 App")
                .bizAppDesc("설명")
                .dupCheckYn("Y")
                .queName("QUEUE_01")
                .queNameDisplay("큐 01")
                .logYn("Y")
                .lastUpdateDtime("20260313120000")
                .lastUpdateUserId("e2e-admin")
                .build();
    }

    private BizAppCreateRequest buildCreateRequest(String bizAppId) {
        return BizAppCreateRequest.builder()
                .bizAppId(bizAppId)
                .bizAppName("테스트 App")
                .bizAppDesc("설명")
                .dupCheckYn("Y")
                .queName("QUEUE_01")
                .logYn("Y")
                .build();
    }

    private BizAppUpdateRequest buildUpdateRequest() {
        return BizAppUpdateRequest.builder()
                .bizAppName("수정된 App")
                .bizAppDesc("수정된 설명")
                .dupCheckYn("N")
                .queName("QUEUE_02")
                .logYn("N")
                .build();
    }
}
