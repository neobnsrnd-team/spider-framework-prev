package com.example.spideradmin.domain.validation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.example.spideradmin.domain.validation.dto.ValidationCreateRequest;
import com.example.spideradmin.domain.validation.dto.ValidationResponse;
import com.example.spideradmin.domain.validation.dto.ValidationSearchRequest;
import com.example.spideradmin.domain.validation.dto.ValidationUpdateRequest;
import com.example.spideradmin.domain.validation.mapper.ValidationMapper;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.ExcelExportUtil;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValidationService 테스트")
class ValidationServiceTest {

    @Mock
    private ValidationMapper validationMapper;

    @InjectMocks
    private ValidationService validationService;

    // ─── getValidationsWithSearch ─────────────────────────────────────

    @Test
    @DisplayName("[목록] 검색 조건으로 조회 시 PageResponse를 반환한다")
    void getValidationsWithSearch_returnsPageResponse() {
        ValidationSearchRequest searchDTO = ValidationSearchRequest.builder()
                .page(1)
                .size(10)
                .validationId("VLD")
                .build();
        given(validationMapper.countAllWithSearch("VLD", null)).willReturn(2L);
        given(validationMapper.findAllWithSearch(eq("VLD"), isNull(), isNull(), isNull(), eq(0), eq(10)))
                .willReturn(List.of(buildResponse("VLD001"), buildResponse("VLD002")));

        PageResponse<ValidationResponse> result = validationService.getValidationsWithSearch(searchDTO);

        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent()).hasSize(2);
    }

    // ─── getById ──────────────────────────────────────────────────────

    @Test
    @DisplayName("[단건] 존재하는 ID로 조회 시 ValidationResponse를 반환한다")
    void getById_exists_returnsResponse() {
        given(validationMapper.selectResponseById("VLD001")).willReturn(buildResponse("VLD001"));

        ValidationResponse result = validationService.getById("VLD001");

        assertThat(result.getValidationId()).isEqualTo("VLD001");
    }

    @Test
    @DisplayName("[단건] 존재하지 않는 ID로 조회 시 NotFoundException을 던진다")
    void getById_notFound_throwsNotFoundException() {
        given(validationMapper.selectResponseById("NO_SUCH")).willReturn(null);

        assertThatThrownBy(() -> validationService.getById("NO_SUCH")).isInstanceOf(NotFoundException.class);
    }

    // ─── create ───────────────────────────────────────────────────────

    @Test
    @DisplayName("[등록] 유효한 데이터로 등록 시 등록된 ValidationResponse를 반환한다")
    void create_validData_returnsResponse() {
        ValidationCreateRequest dto =
                ValidationCreateRequest.builder().validationId("VLD_NEW").build();
        given(validationMapper.selectResponseById("VLD_NEW")).willReturn(buildResponse("VLD_NEW"));

        ValidationResponse result = validationService.create(dto);

        assertThat(result.getValidationId()).isEqualTo("VLD_NEW");
        then(validationMapper).should().insert(eq(dto), anyString(), anyString());
    }

    @Test
    @DisplayName("[등록] 중복 ID 등록 시 DuplicateException을 던진다")
    void create_duplicateKey_throwsDuplicateException() {
        ValidationCreateRequest dto =
                ValidationCreateRequest.builder().validationId("VLD_DUP").build();
        willThrow(new DuplicateKeyException("dup")).given(validationMapper).insert(eq(dto), anyString(), anyString());

        assertThatThrownBy(() -> validationService.create(dto)).isInstanceOf(DuplicateException.class);
    }

    // ─── update ───────────────────────────────────────────────────────

    @Test
    @DisplayName("[수정] 존재하는 ID 수정 시 수정된 ValidationResponse를 반환한다")
    void update_exists_returnsUpdatedResponse() {
        ValidationUpdateRequest dto = new ValidationUpdateRequest();
        given(validationMapper.countById("VLD001")).willReturn(1);
        given(validationMapper.selectResponseById("VLD001")).willReturn(buildResponse("VLD001"));

        ValidationResponse result = validationService.update("VLD001", dto);

        assertThat(result.getValidationId()).isEqualTo("VLD001");
        then(validationMapper).should().update(eq("VLD001"), eq(dto), anyString(), anyString());
    }

    @Test
    @DisplayName("[수정] 존재하지 않는 ID 수정 시 NotFoundException을 던진다")
    void update_notFound_throwsNotFoundException() {
        given(validationMapper.countById("NO_SUCH")).willReturn(0);
        ValidationUpdateRequest req = new ValidationUpdateRequest();

        assertThatThrownBy(() -> validationService.update("NO_SUCH", req)).isInstanceOf(NotFoundException.class);
    }

    // ─── delete ───────────────────────────────────────────────────────

    @Test
    @DisplayName("[삭제] 존재하는 ID 삭제 시 deleteById가 호출된다")
    void delete_exists_callsDeleteById() {
        given(validationMapper.countById("VLD001")).willReturn(1);

        validationService.delete("VLD001");

        then(validationMapper).should().deleteById("VLD001");
    }

    @Test
    @DisplayName("[삭제] 존재하지 않는 ID 삭제 시 NotFoundException을 던진다")
    void delete_notFound_throwsNotFoundException() {
        given(validationMapper.countById("NO_SUCH")).willReturn(0);

        assertThatThrownBy(() -> validationService.delete("NO_SUCH")).isInstanceOf(NotFoundException.class);
    }

    // ─── exportValidations ────────────────────────────────────────────

    @Test
    @DisplayName("[엑셀] 데이터가 있으면 xlsx 바이트 배열을 반환한다")
    void exportValidations_withData_returnsBytes() {
        given(validationMapper.findAllForExport(anyString(), isNull(), isNull(), isNull()))
                .willReturn(List.of(buildResponse("VLD001"), buildResponse("VLD002")));

        byte[] result = validationService.exportValidations("VLD", null, null, null);

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀] 데이터가 없어도 빈 xlsx 바이트 배열을 반환한다")
    void exportValidations_emptyData_returnsBytes() {
        given(validationMapper.findAllForExport(isNull(), isNull(), isNull(), isNull()))
                .willReturn(List.of());

        byte[] result = validationService.exportValidations(null, null, null, null);

        assertThat(result).isNotNull().hasSizeGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀] 행 수가 한도를 초과하면 InvalidInputException을 던진다")
    void exportValidations_exceedsLimit_throwsInvalidInputException() {
        List<ValidationResponse> overLimit =
                Collections.nCopies(ExcelExportUtil.MAX_ROW_LIMIT + 1, buildResponse("VLD001"));
        given(validationMapper.findAllForExport(any(), any(), any(), any())).willReturn(overLimit);

        assertThatThrownBy(() -> validationService.exportValidations(null, null, null, null))
                .isInstanceOf(InvalidInputException.class);
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private ValidationResponse buildResponse(String validationId) {
        return ValidationResponse.builder()
                .validationId(validationId)
                .validationDesc("테스트 설명")
                .javaClassName("com.test.Validation")
                .lastUpdateDtime("20260313120000")
                .lastUpdateUserId("e2e-admin")
                .build();
    }
}
