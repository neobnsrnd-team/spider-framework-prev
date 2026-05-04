package com.example.spideradmin.domain.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.spideradmin.domain.service.dto.FwkServiceRelationItemRequest;
import com.example.spideradmin.domain.service.dto.FwkServiceRelationParamRequest;
import com.example.spideradmin.domain.service.dto.FwkServiceRelationParamResponse;
import com.example.spideradmin.domain.service.dto.FwkServiceRelationResponse;
import com.example.spideradmin.domain.service.dto.FwkServiceRelationSaveRequest;
import com.example.spideradmin.domain.service.mapper.FwkServiceMapper;
import com.example.spideradmin.domain.service.mapper.FwkServiceRelationMapper;
import com.example.spideradmin.global.exception.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FwkServiceRelationService 테스트")
class FwkServiceRelationServiceTest {

    @Mock
    private FwkServiceRelationMapper fwkServiceRelationMapper;

    @Mock
    private FwkServiceMapper fwkServiceMapper;

    @InjectMocks
    private FwkServiceRelationService fwkServiceRelationService;

    // ─── getRelations ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getRelations")
    class GetRelationsTests {

        @Test
        @DisplayName("[조회] 존재하는 서비스 ID이면 연결 컴포넌트 목록을 반환해야 한다")
        void exists_returnsRelationList() {
            given(fwkServiceMapper.countById("SVC-001")).willReturn(1);
            given(fwkServiceRelationMapper.findRelationsByServiceId("SVC-001"))
                    .willReturn(List.of(buildRelationResponse("SVC-001", 1)));

            List<FwkServiceRelationResponse> result = fwkServiceRelationService.getRelations("SVC-001");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getComponentId()).isEqualTo("CMP-001");
        }

        @Test
        @DisplayName("[조회] 존재하지 않는 서비스 ID이면 NotFoundException을 발생시켜야 한다")
        void notExists_throwsNotFoundException() {
            given(fwkServiceMapper.countById("NOT-EXIST")).willReturn(0);

            assertThatThrownBy(() -> fwkServiceRelationService.getRelations("NOT-EXIST"))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ─── saveRelations ────────────────────────────────────────────────

    @Nested
    @DisplayName("saveRelations")
    class SaveRelationsTests {

        @Test
        @DisplayName("[저장] relations가 있으면 DELETE 후 insertRelationBatch를 호출해야 한다")
        void withRelations_deletesAndInsertsRelations() {
            given(fwkServiceMapper.countById("SVC-001")).willReturn(1);
            given(fwkServiceRelationMapper.findRelationsByServiceId("SVC-001"))
                    .willReturn(List.of(buildRelationResponse("SVC-001", 1)));

            FwkServiceRelationSaveRequest dto = FwkServiceRelationSaveRequest.builder()
                    .relations(List.of(buildRelationItemRequest(1, "CMP-001")))
                    .build();

            fwkServiceRelationService.saveRelations("SVC-001", dto);

            org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(fwkServiceRelationMapper);
            inOrder.verify(fwkServiceRelationMapper).deleteParamsByServiceId("SVC-001");
            inOrder.verify(fwkServiceRelationMapper).deleteRelationsByServiceId("SVC-001");
            inOrder.verify(fwkServiceRelationMapper)
                    .insertRelationBatch(eq("SVC-001"), anyList(), anyString(), anyString());
        }

        @Test
        @DisplayName("[저장] relations의 params가 있으면 insertRelationParamBatch도 호출해야 한다")
        void withParams_insertsParams() {
            given(fwkServiceMapper.countById("SVC-001")).willReturn(1);
            given(fwkServiceRelationMapper.findRelationsByServiceId("SVC-001")).willReturn(List.of());

            FwkServiceRelationItemRequest item = FwkServiceRelationItemRequest.builder()
                    .serviceSeqNo(1)
                    .componentId("CMP-001")
                    .params(List.of(FwkServiceRelationParamRequest.builder()
                            .serviceSeqNo(1)
                            .componentId("CMP-001")
                            .paramSeqNo(1)
                            .paramValue("value1")
                            .build()))
                    .build();

            FwkServiceRelationSaveRequest dto = FwkServiceRelationSaveRequest.builder()
                    .relations(List.of(item))
                    .build();

            fwkServiceRelationService.saveRelations("SVC-001", dto);

            then(fwkServiceRelationMapper).should().insertRelationParamBatch(eq("SVC-001"), anyList());
        }

        @Test
        @DisplayName("[저장] relations가 비어있으면 insertRelationBatch를 호출해서는 안 된다")
        void withEmptyRelations_doesNotCallInsert() {
            given(fwkServiceMapper.countById("SVC-001")).willReturn(1);
            given(fwkServiceRelationMapper.findRelationsByServiceId("SVC-001")).willReturn(List.of());

            FwkServiceRelationSaveRequest dto =
                    FwkServiceRelationSaveRequest.builder().relations(List.of()).build();

            fwkServiceRelationService.saveRelations("SVC-001", dto);

            then(fwkServiceRelationMapper).should(never()).insertRelationBatch(any(), any(), any(), any());
            then(fwkServiceRelationMapper).should(never()).insertRelationParamBatch(any(), any());
        }

        @Test
        @DisplayName("[저장] relations가 null이면 insertRelationBatch를 호출해서는 안 된다")
        void withNullRelations_doesNotCallInsert() {
            given(fwkServiceMapper.countById("SVC-001")).willReturn(1);
            given(fwkServiceRelationMapper.findRelationsByServiceId("SVC-001")).willReturn(List.of());

            FwkServiceRelationSaveRequest dto =
                    FwkServiceRelationSaveRequest.builder().relations(null).build();

            fwkServiceRelationService.saveRelations("SVC-001", dto);

            then(fwkServiceRelationMapper).should(never()).insertRelationBatch(any(), any(), any(), any());
        }

        @Test
        @DisplayName("[저장] 존재하지 않는 서비스 ID이면 NotFoundException을 발생시켜야 한다")
        void notExists_throwsNotFoundException() {
            given(fwkServiceMapper.countById("NOT-EXIST")).willReturn(0);

            assertThatThrownBy(() -> fwkServiceRelationService.saveRelations(
                            "NOT-EXIST",
                            FwkServiceRelationSaveRequest.builder()
                                    .relations(List.of())
                                    .build()))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private FwkServiceRelationResponse buildRelationResponse(String serviceId, int seqNo) {
        return FwkServiceRelationResponse.builder()
                .serviceId(serviceId)
                .serviceSeqNo(seqNo)
                .componentId("CMP-001")
                .componentName("테스트 컴포넌트")
                .params(List.of(FwkServiceRelationParamResponse.builder()
                        .paramSeqNo(1)
                        .paramKey("KEY_1")
                        .paramDesc("파라미터 1")
                        .paramValue("value1")
                        .build()))
                .build();
    }

    private FwkServiceRelationItemRequest buildRelationItemRequest(int seqNo, String componentId) {
        return FwkServiceRelationItemRequest.builder()
                .serviceSeqNo(seqNo)
                .componentId(componentId)
                .params(List.of())
                .build();
    }
}
