package com.example.admin_demo.domain.component.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.example.admin_demo.domain.component.dto.ComponentCreateRequest;
import com.example.admin_demo.domain.component.dto.ComponentParamRequest;
import com.example.admin_demo.domain.component.dto.ComponentParamResponse;
import com.example.admin_demo.domain.component.dto.ComponentResponse;
import com.example.admin_demo.domain.component.dto.ComponentSearchRequest;
import com.example.admin_demo.domain.component.dto.ComponentUpdateRequest;
import com.example.admin_demo.domain.component.mapper.ComponentMapper;
import com.example.admin_demo.global.dto.PageResponse;
import com.example.admin_demo.global.exception.DuplicateException;
import com.example.admin_demo.global.exception.NotFoundException;
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
@DisplayName("ComponentService 테스트")
class ComponentServiceTest {

    @Mock
    private ComponentMapper componentMapper;

    @InjectMocks
    private ComponentService componentService;

    // ─── getComponentsWithSearch ──────────────────────────────────────

    @Nested
    @DisplayName("getComponentsWithSearch")
    class GetComponentsWithSearchTests {

        @Test
        @DisplayName("[조회] 검색 결과를 PageResponse로 반환해야 한다")
        void returnsPageResponse() {
            ComponentSearchRequest searchDTO =
                    ComponentSearchRequest.builder().page(1).size(10).build();
            List<ComponentResponse> data = List.of(buildResponse("CMP-001"), buildResponse("CMP-002"));
            given(componentMapper.countAllWithSearch(any(), any(), any(), any()))
                    .willReturn(2L);
            given(componentMapper.findAllWithSearch(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(data);

            PageResponse<ComponentResponse> result = componentService.getComponentsWithSearch(searchDTO);

            assertThat(result.getTotalElements()).isEqualTo(2L);
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getComponentId()).isEqualTo("CMP-001");
        }

        @Test
        @DisplayName("[조회] 검색 결과가 없으면 빈 content를 반환해야 한다")
        void noResult_returnsEmptyContent() {
            ComponentSearchRequest searchDTO =
                    ComponentSearchRequest.builder().page(1).size(10).build();
            given(componentMapper.countAllWithSearch(any(), any(), any(), any()))
                    .willReturn(0L);
            given(componentMapper.findAllWithSearch(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                    .willReturn(List.of());

            PageResponse<ComponentResponse> result = componentService.getComponentsWithSearch(searchDTO);

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ─── getById ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("[조회] 존재하는 ID이면 파라미터 포함 ComponentResponse를 반환해야 한다")
        void exists_returnsResponseWithParams() {
            ComponentResponse master = buildResponse("CMP-001");
            List<ComponentParamResponse> params = List.of(buildParamResponse("CMP-001", 1));
            given(componentMapper.selectResponseById("CMP-001")).willReturn(master);
            given(componentMapper.findParamsByComponentId("CMP-001")).willReturn(params);

            ComponentResponse result = componentService.getById("CMP-001");

            assertThat(result.getComponentId()).isEqualTo("CMP-001");
            assertThat(result.getParams()).hasSize(1);
            assertThat(result.getParams().get(0).getParamKey()).isEqualTo("KEY_1");
        }

        @Test
        @DisplayName("[조회] 존재하지 않는 ID이면 NotFoundException을 발생시켜야 한다")
        void notExists_throwsNotFoundException() {
            given(componentMapper.selectResponseById("NOT-EXIST")).willReturn(null);

            assertThatThrownBy(() -> componentService.getById("NOT-EXIST")).isInstanceOf(NotFoundException.class);
        }
    }

    // ─── create ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("[등록] params가 있으면 insertComponent 후 insertParamBatch를 호출해야 한다")
        void withParams_insertsComponentAndBatchParams() {
            ComponentCreateRequest dto = buildCreateRequest("CMP-NEW", List.of(buildParamRequest(1)));
            ComponentResponse master = buildResponse("CMP-NEW");
            given(componentMapper.selectResponseById("CMP-NEW")).willReturn(master);
            given(componentMapper.findParamsByComponentId("CMP-NEW"))
                    .willReturn(List.of(buildParamResponse("CMP-NEW", 1)));

            ComponentResponse result = componentService.create(dto);

            assertThat(result.getComponentId()).isEqualTo("CMP-NEW");
            then(componentMapper).should().insertComponent(eq(dto), anyString(), anyString());
            then(componentMapper).should().insertParamBatch(eq("CMP-NEW"), anyList());
        }

        @Test
        @DisplayName("[등록] params가 null이면 insertParamBatch를 호출해서는 안 된다")
        void withNullParams_doesNotCallInsertParamBatch() {
            ComponentCreateRequest dto = buildCreateRequest("CMP-NEW", null);
            given(componentMapper.selectResponseById("CMP-NEW")).willReturn(buildResponse("CMP-NEW"));
            given(componentMapper.findParamsByComponentId("CMP-NEW")).willReturn(List.of());

            componentService.create(dto);

            then(componentMapper).should(never()).insertParamBatch(any(), any());
        }

        @Test
        @DisplayName("[등록] params가 빈 리스트이면 insertParamBatch를 호출해서는 안 된다")
        void withEmptyParams_doesNotCallInsertParamBatch() {
            ComponentCreateRequest dto = buildCreateRequest("CMP-NEW", List.of());
            given(componentMapper.selectResponseById("CMP-NEW")).willReturn(buildResponse("CMP-NEW"));
            given(componentMapper.findParamsByComponentId("CMP-NEW")).willReturn(List.of());

            componentService.create(dto);

            then(componentMapper).should(never()).insertParamBatch(any(), any());
        }

        @Test
        @DisplayName("[등록] 중복 ID이면 DuplicateException을 발생시켜야 한다")
        void duplicateId_throwsDuplicateException() {
            ComponentCreateRequest dto = buildCreateRequest("CMP-DUP", null);
            willThrow(new DuplicateKeyException("duplicate"))
                    .given(componentMapper)
                    .insertComponent(any(), anyString(), anyString());

            assertThatThrownBy(() -> componentService.create(dto)).isInstanceOf(DuplicateException.class);
        }
    }

    // ─── update ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("[수정] params가 있으면 deleteParams 후 insertParamBatch를 호출해야 한다")
        void withParams_deletesAndReinserts() {
            ComponentUpdateRequest dto = buildUpdateRequest(List.of(buildParamRequest(1)));
            given(componentMapper.countById("CMP-001")).willReturn(1);
            given(componentMapper.selectResponseById("CMP-001")).willReturn(buildResponse("CMP-001"));
            given(componentMapper.findParamsByComponentId("CMP-001"))
                    .willReturn(List.of(buildParamResponse("CMP-001", 1)));

            componentService.update("CMP-001", dto);

            then(componentMapper).should().deleteParamsByComponentId("CMP-001");
            then(componentMapper).should().insertParamBatch(eq("CMP-001"), anyList());
        }

        @Test
        @DisplayName("[수정] params가 없으면 deleteParamsByComponentId만 호출하고 insertParamBatch는 호출해서는 안 된다")
        void withoutParams_deletesParamsOnly() {
            ComponentUpdateRequest dto = buildUpdateRequest(null);
            given(componentMapper.countById("CMP-001")).willReturn(1);
            given(componentMapper.selectResponseById("CMP-001")).willReturn(buildResponse("CMP-001"));
            given(componentMapper.findParamsByComponentId("CMP-001")).willReturn(List.of());

            componentService.update("CMP-001", dto);

            then(componentMapper).should().deleteParamsByComponentId("CMP-001");
            then(componentMapper).should(never()).insertParamBatch(any(), any());
        }

        @Test
        @DisplayName("[수정] 존재하지 않는 ID이면 NotFoundException을 발생시켜야 한다")
        void notExists_throwsNotFoundException() {
            given(componentMapper.countById("NOT-EXIST")).willReturn(0);
            ComponentUpdateRequest req = buildUpdateRequest(null);

            assertThatThrownBy(() -> componentService.update("NOT-EXIST", req)).isInstanceOf(NotFoundException.class);
        }
    }

    // ─── delete ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class DeleteTests {

        @Test
        @DisplayName("[삭제] 존재하는 ID이면 deleteParams 후 deleteById를 호출해야 한다")
        void exists_deletesParamsThenComponent() {
            given(componentMapper.countById("CMP-001")).willReturn(1);

            componentService.delete("CMP-001", "TYPE-A");

            then(componentMapper).should().deleteParamsByComponentId("CMP-001");
            then(componentMapper).should().deleteById("CMP-001");
        }

        @Test
        @DisplayName("[삭제] 존재하지 않는 ID이면 NotFoundException을 발생시켜야 한다")
        void notExists_throwsNotFoundException() {
            given(componentMapper.countById("NOT-EXIST")).willReturn(0);

            assertThatThrownBy(() -> componentService.delete("NOT-EXIST", "TYPE-A")).isInstanceOf(NotFoundException.class);
        }
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private ComponentResponse buildResponse(String componentId) {
        return ComponentResponse.builder()
                .componentId(componentId)
                .componentName("테스트 컴포넌트")
                .componentDesc("설명")
                .componentType("J")
                .componentClassName("com.example.TestComponent")
                .componentMethodName("execute")
                .componentCreateType("A")
                .bizGroupId("BIZ-001")
                .useYn("Y")
                .lastUpdateDtime("20260316120000")
                .lastUpdateUserId("e2e-admin")
                .build();
    }

    private ComponentParamResponse buildParamResponse(String componentId, int seq) {
        return ComponentParamResponse.builder()
                .componentId(componentId)
                .paramSeqNo(seq)
                .paramKey("KEY_" + seq)
                .paramDesc("파라미터 " + seq)
                .defaultParamValue("DEFAULT_" + seq)
                .build();
    }

    private ComponentParamRequest buildParamRequest(int seq) {
        return ComponentParamRequest.builder()
                .paramSeqNo(seq)
                .paramKey("KEY_" + seq)
                .paramDesc("파라미터 " + seq)
                .defaultParamValue("DEFAULT_" + seq)
                .build();
    }

    private ComponentCreateRequest buildCreateRequest(String componentId, List<ComponentParamRequest> params) {
        return ComponentCreateRequest.builder()
                .componentId(componentId)
                .componentName("테스트 컴포넌트")
                .componentDesc("설명")
                .componentType("J")
                .componentClassName("com.example.TestComponent")
                .componentMethodName("execute")
                .componentCreateType("A")
                .bizGroupId("BIZ-001")
                .useYn("Y")
                .params(params)
                .build();
    }

    private ComponentUpdateRequest buildUpdateRequest(List<ComponentParamRequest> params) {
        return ComponentUpdateRequest.builder()
                .componentName("수정된 컴포넌트")
                .componentDesc("수정된 설명")
                .componentType("J")
                .componentClassName("com.example.UpdatedComponent")
                .componentMethodName("execute")
                .componentCreateType("M")
                .bizGroupId("BIZ-002")
                .useYn("Y")
                .params(params)
                .build();
    }
}
