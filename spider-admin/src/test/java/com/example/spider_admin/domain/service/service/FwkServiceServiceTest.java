package com.example.spider_admin.domain.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.example.spider_admin.domain.service.dto.FwkServiceCreateRequest;
import com.example.spider_admin.domain.service.dto.FwkServiceDetailResponse;
import com.example.spider_admin.domain.service.dto.FwkServiceResponse;
import com.example.spider_admin.domain.service.dto.FwkServiceSearchRequest;
import com.example.spider_admin.domain.service.dto.FwkServiceUpdateRequest;
import com.example.spider_admin.domain.service.dto.FwkServiceUseYnBulkRequest;
import com.example.spider_admin.domain.service.mapper.FwkServiceMapper;
import com.example.spider_admin.domain.service.mapper.FwkServiceRelationMapper;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.DuplicateException;
import com.example.spider_admin.global.exception.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
@DisplayName("FwkServiceService 테스트")
class FwkServiceServiceTest {

    @Mock
    private FwkServiceMapper fwkServiceMapper;

    @Mock
    private FwkServiceRelationMapper fwkServiceRelationMapper;

    @InjectMocks
    private FwkServiceService fwkServiceService;

    // ─── getServicesWithSearch ────────────────────────────────────────

    @Nested
    @DisplayName("getServicesWithSearch")
    class GetServicesWithSearchTests {

        @Test
        @DisplayName("[조회] 검색 결과를 PageResponse로 반환해야 한다")
        void returnsPageResponse() {
            FwkServiceSearchRequest searchDTO =
                    FwkServiceSearchRequest.builder().page(1).size(10).build();
            List<FwkServiceResponse> data = List.of(buildResponse("SVC-001"), buildResponse("SVC-002"));
            given(fwkServiceMapper.countAllWithSearch(
                            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                            any()))
                    .willReturn(2L);
            given(fwkServiceMapper.findAllWithSearch(
                            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                            any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(data);

            PageResponse<FwkServiceResponse> result = fwkServiceService.getServicesWithSearch(searchDTO);

            assertThat(result.getTotalElements()).isEqualTo(2L);
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getServiceId()).isEqualTo("SVC-001");
        }

        @Test
        @DisplayName("[조회] 검색 결과가 없으면 빈 content를 반환해야 한다")
        void noResult_returnsEmptyContent() {
            FwkServiceSearchRequest searchDTO =
                    FwkServiceSearchRequest.builder().page(1).size(10).build();
            given(fwkServiceMapper.countAllWithSearch(
                            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                            any()))
                    .willReturn(0L);
            given(fwkServiceMapper.findAllWithSearch(
                            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                            any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(List.of());

            PageResponse<FwkServiceResponse> result = fwkServiceService.getServicesWithSearch(searchDTO);

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ─── getById ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("[조회] 존재하는 ID이면 relations 포함 FwkServiceDetailResponse를 반환해야 한다")
        void exists_returnsDetailWithRelations() {
            given(fwkServiceMapper.selectResponseById("SVC-001")).willReturn(buildResponse("SVC-001"));
            given(fwkServiceRelationMapper.findRelationsByServiceId("SVC-001")).willReturn(List.of());

            FwkServiceDetailResponse result = fwkServiceService.getById("SVC-001");

            assertThat(result.getServiceId()).isEqualTo("SVC-001");
            assertThat(result.getRelations()).isNotNull();
        }

        @Test
        @DisplayName("[조회] 존재하지 않는 ID이면 NotFoundException을 발생시켜야 한다")
        void notExists_throwsNotFoundException() {
            given(fwkServiceMapper.selectResponseById("NOT-EXIST")).willReturn(null);

            assertThatThrownBy(() -> fwkServiceService.getById("NOT-EXIST")).isInstanceOf(NotFoundException.class);
        }
    }

    // ─── create ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("[등록] 정상 등록 시 insertFwkService를 호출하고 상세 응답을 반환해야 한다")
        void create_success_callsInsertAndReturnsDetail() {
            FwkServiceCreateRequest dto = buildCreateRequest("SVC-NEW");
            given(fwkServiceMapper.selectResponseById("SVC-NEW")).willReturn(buildResponse("SVC-NEW"));
            given(fwkServiceRelationMapper.findRelationsByServiceId("SVC-NEW")).willReturn(List.of());

            FwkServiceDetailResponse result = fwkServiceService.create(dto);

            assertThat(result.getServiceId()).isEqualTo("SVC-NEW");
            then(fwkServiceMapper).should().insertFwkService(eq(dto), anyString(), anyString());
        }

        @Test
        @DisplayName("[등록] 중복 ID이면 DuplicateException을 발생시켜야 한다")
        void duplicateId_throwsDuplicateException() {
            FwkServiceCreateRequest dto = buildCreateRequest("SVC-DUP");
            willThrow(new DuplicateKeyException("duplicate"))
                    .given(fwkServiceMapper)
                    .insertFwkService(any(), anyString(), anyString());

            assertThatThrownBy(() -> fwkServiceService.create(dto)).isInstanceOf(DuplicateException.class);
        }
    }

    // ─── update ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("[수정] 존재하는 ID이면 updateFwkService를 호출하고 상세 응답을 반환해야 한다")
        void update_exists_callsUpdateAndReturnsDetail() {
            FwkServiceUpdateRequest dto = buildUpdateRequest();
            given(fwkServiceMapper.countById("SVC-001")).willReturn(1);
            given(fwkServiceMapper.selectResponseById("SVC-001")).willReturn(buildResponse("SVC-001"));
            given(fwkServiceRelationMapper.findRelationsByServiceId("SVC-001")).willReturn(List.of());

            FwkServiceDetailResponse result = fwkServiceService.update("SVC-001", dto);

            assertThat(result.getServiceId()).isEqualTo("SVC-001");
            then(fwkServiceMapper).should().updateFwkService(eq("SVC-001"), eq(dto), anyString(), anyString());
        }

        @Test
        @DisplayName("[수정] 존재하지 않는 ID이면 NotFoundException을 발생시켜야 한다")
        void notExists_throwsNotFoundException() {
            given(fwkServiceMapper.countById("NOT-EXIST")).willReturn(0);

            assertThatThrownBy(() -> fwkServiceService.update("NOT-EXIST", buildUpdateRequest()))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ─── delete ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("[삭제] FK 순서대로 RELATION_PARAM → RELATION → SERVICE 순으로 삭제해야 한다")
        void delete_exists_deletesInFkOrder() {
            given(fwkServiceMapper.countById("SVC-001")).willReturn(1);

            fwkServiceService.delete("SVC-001", "TYPE-A");

            org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(fwkServiceRelationMapper, fwkServiceMapper);
            inOrder.verify(fwkServiceRelationMapper).deleteParamsByServiceId("SVC-001");
            inOrder.verify(fwkServiceRelationMapper).deleteRelationsByServiceId("SVC-001");
            inOrder.verify(fwkServiceMapper).deleteById("SVC-001");
        }

        @Test
        @DisplayName("[삭제] 존재하지 않는 ID이면 NotFoundException을 발생시켜야 한다")
        void notExists_throwsNotFoundException() {
            given(fwkServiceMapper.countById("NOT-EXIST")).willReturn(0);

            assertThatThrownBy(() -> fwkServiceService.delete("NOT-EXIST", "TYPE-A"))
                    .isInstanceOf(NotFoundException.class);
            then(fwkServiceMapper).should(never()).deleteById(any());
        }
    }

    // ─── bulkUpdateUseYn ──────────────────────────────────────────────

    @Nested
    @DisplayName("bulkUpdateUseYn")
    class BulkUpdateUseYnTests {

        @Test
        @DisplayName("[일괄변경] 서비스 ID 목록의 USE_YN을 일괄 변경해야 한다")
        void bulk_callsUpdateUseYnBatch() {
            FwkServiceUseYnBulkRequest dto = FwkServiceUseYnBulkRequest.builder()
                    .serviceIds(List.of("SVC-001", "SVC-002"))
                    .useYn("N")
                    .build();

            fwkServiceService.bulkUpdateUseYn(dto);

            then(fwkServiceMapper)
                    .should()
                    .updateUseYnBatch(eq(List.of("SVC-001", "SVC-002")), eq("N"), anyString(), anyString());
        }
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private FwkServiceResponse buildResponse(String serviceId) {
        return FwkServiceResponse.builder()
                .serviceId(serviceId)
                .serviceName("테스트 서비스")
                .serviceType("B")
                .className("com.example.TestService")
                .methodName("execute")
                .bizGroupId("BIZ-001")
                .useYn("Y")
                .lastUpdateDtime("20260317120000")
                .lastUpdateUserId("e2e-admin")
                .build();
    }

    private FwkServiceCreateRequest buildCreateRequest(String serviceId) {
        return FwkServiceCreateRequest.builder()
                .serviceId(serviceId)
                .serviceName("테스트 서비스")
                .serviceType("B")
                .className("com.example.TestService")
                .methodName("execute")
                .bizGroupId("BIZ-001")
                .useYn("Y")
                .build();
    }

    private FwkServiceUpdateRequest buildUpdateRequest() {
        return FwkServiceUpdateRequest.builder()
                .serviceName("수정된 서비스")
                .serviceType("B")
                .useYn("Y")
                .build();
    }
}
