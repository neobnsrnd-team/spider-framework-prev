package com.example.spideradmin.domain.datasource.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.example.spideradmin.domain.datasource.dto.DataSourceCreateRequest;
import com.example.spideradmin.domain.datasource.dto.DataSourceResponse;
import com.example.spideradmin.domain.datasource.dto.DataSourceSearchRequest;
import com.example.spideradmin.domain.datasource.dto.DataSourceUpdateRequest;
import com.example.spideradmin.domain.datasource.mapper.DataSourceMapper;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.ExcelExportUtil;
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
@DisplayName("DataSourceService 테스트")
class DataSourceServiceTest {

    @Mock
    private DataSourceMapper dataSourceMapper;

    @InjectMocks
    private DataSourceService dataSourceService;

    // ─── 목록 조회 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDataSourcesWithSearch")
    class GetDataSourcesWithSearch {

        @Test
        @DisplayName("[조회] 검색 결과를 PageResponse로 반환해야 한다")
        void search_returnsPageResponse() {
            DataSourceSearchRequest searchDTO =
                    DataSourceSearchRequest.builder().page(1).size(10).build();

            given(dataSourceMapper.countAllWithSearch(any(), any(), any())).willReturn(2L);
            given(dataSourceMapper.findAllWithSearch(
                            any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                    .willReturn(List.of(buildResponse("DS-001"), buildResponse("DS-002")));

            PageResponse<DataSourceResponse> result = dataSourceService.getDataSourcesWithSearch(searchDTO);

            assertThat(result.getTotalElements()).isEqualTo(2L);
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("[조회] 비밀번호는 항상 마스킹되어 반환해야 한다")
        void search_passwordAlwaysMasked() {
            DataSourceSearchRequest searchDTO =
                    DataSourceSearchRequest.builder().page(1).size(10).build();
            DataSourceResponse rawResponse = buildResponse("DS-001");
            rawResponse.setDbPassword("realpassword123"); // NOSONAR test-only value

            given(dataSourceMapper.countAllWithSearch(any(), any(), any())).willReturn(1L);
            given(dataSourceMapper.findAllWithSearch(
                            any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class)))
                    .willReturn(List.of(rawResponse));

            PageResponse<DataSourceResponse> result = dataSourceService.getDataSourcesWithSearch(searchDTO);

            assertThat(result.getContent().get(0).getDbPassword()).isEqualTo("****");
        }
    }

    // ─── 단건 조회 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("[조회] 존재하는 ID로 조회하면 마스킹된 Response를 반환해야 한다")
        void getById_exists_returnsMasked() {
            DataSourceResponse raw = buildResponse("DS-001");
            raw.setDbPassword("secret"); // NOSONAR test-only value
            given(dataSourceMapper.selectResponseById("DS-001")).willReturn(raw);

            DataSourceResponse result = dataSourceService.getById("DS-001");

            assertThat(result.getDbId()).isEqualTo("DS-001");
            assertThat(result.getDbPassword()).isEqualTo("****");
        }

        @Test
        @DisplayName("[조회] 존재하지 않는 ID로 조회하면 NotFoundException이 발생해야 한다")
        void getById_notExists_throwsNotFoundException() {
            given(dataSourceMapper.selectResponseById("NONE")).willReturn(null);

            assertThatThrownBy(() -> dataSourceService.getById("NONE")).isInstanceOf(NotFoundException.class);
        }
    }

    // ─── 등록 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("[등록] 정상 등록 시 마스킹된 Response를 반환해야 한다")
        void create_success_returnsMasked() {
            DataSourceCreateRequest dto = DataSourceCreateRequest.builder()
                    .dbId("DS-NEW")
                    .dbPassword("newpass") // NOSONAR test-only value
                    .jndiYn("N")
                    .build();

            DataSourceResponse saved = buildResponse("DS-NEW");
            saved.setDbPassword("newpass"); // NOSONAR test-only value
            given(dataSourceMapper.selectResponseById("DS-NEW")).willReturn(saved);

            DataSourceResponse result = dataSourceService.create(dto);

            then(dataSourceMapper).should().insert(any(), anyString(), anyString());
            assertThat(result.getDbId()).isEqualTo("DS-NEW");
            assertThat(result.getDbPassword()).isEqualTo("****");
        }

        @Test
        @DisplayName("[등록] 중복 DB ID로 등록하면 DuplicateException이 발생해야 한다")
        void create_duplicateKey_throwsDuplicateException() {
            DataSourceCreateRequest dto =
                    DataSourceCreateRequest.builder().dbId("DS-DUP").jndiYn("N").build();
            willThrow(new DuplicateKeyException("PK_FWK_SQL_CONF"))
                    .given(dataSourceMapper)
                    .insert(any(), any(), any());

            assertThatThrownBy(() -> dataSourceService.create(dto)).isInstanceOf(DuplicateException.class);
        }
    }

    // ─── 수정 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("[수정] 존재하는 ID 수정 시 마스킹된 Response를 반환해야 한다")
        void update_exists_returnsMasked() {
            DataSourceUpdateRequest dto =
                    DataSourceUpdateRequest.builder().dbName("Updated").build();
            DataSourceResponse saved = buildResponse("DS-001");
            saved.setDbPassword("stored"); // NOSONAR test-only value
            given(dataSourceMapper.countByDbId("DS-001")).willReturn(1);
            given(dataSourceMapper.selectResponseById("DS-001")).willReturn(saved);

            DataSourceResponse result = dataSourceService.update("DS-001", dto);

            then(dataSourceMapper).should().update(anyString(), any(), anyString(), anyString());
            assertThat(result.getDbPassword()).isEqualTo("****");
        }

        @Test
        @DisplayName("[수정] 존재하지 않는 ID 수정 시 NotFoundException이 발생해야 한다")
        void update_notExists_throwsNotFoundException() {
            given(dataSourceMapper.countByDbId("NONE")).willReturn(0);

            DataSourceUpdateRequest dto = new DataSourceUpdateRequest();
            assertThatThrownBy(() -> dataSourceService.update("NONE", dto)).isInstanceOf(NotFoundException.class);
        }
    }

    // ─── 엑셀 내보내기 ───────────────────────────────────────────────

    @Nested
    @DisplayName("exportDataSources")
    class ExportDataSources {

        @Test
        @DisplayName("[내보내기] 정상 데이터 조회 시 byte[] 를 반환해야 한다")
        void export_success_returnsByteArray() {
            given(dataSourceMapper.findAllForExport(any(), any(), any(), any(), any()))
                    .willReturn(List.of(buildResponse("DS-001"), buildResponse("DS-002")));

            byte[] result = dataSourceService.exportDataSources(null, null, null, null, null);

            assertThat(result).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("[내보내기] 최대 건수 초과 시 InvalidInputException이 발생해야 한다")
        void export_exceedsLimit_throwsInvalidInputException() {
            List<DataSourceResponse> oversized =
                    java.util.Collections.nCopies(ExcelExportUtil.MAX_ROW_LIMIT + 1, buildResponse("DS-001"));
            given(dataSourceMapper.findAllForExport(any(), any(), any(), any(), any()))
                    .willReturn(oversized);

            assertThatThrownBy(() -> dataSourceService.exportDataSources(null, null, null, null, null))
                    .isInstanceOf(InvalidInputException.class);
        }
    }

    // ─── 삭제 ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("[삭제] 존재하는 ID 삭제 시 deleteById를 호출해야 한다")
        void delete_exists_callsDeleteById() {
            given(dataSourceMapper.countByDbId("DS-001")).willReturn(1);

            dataSourceService.delete("DS-001");

            then(dataSourceMapper).should().deleteById("DS-001");
        }

        @Test
        @DisplayName("[삭제] 존재하지 않는 ID 삭제 시 NotFoundException이 발생해야 한다")
        void delete_notExists_throwsNotFoundException() {
            given(dataSourceMapper.countByDbId("NONE")).willReturn(0);

            assertThatThrownBy(() -> dataSourceService.delete("NONE")).isInstanceOf(NotFoundException.class);
        }
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────

    private DataSourceResponse buildResponse(String dbId) {
        return DataSourceResponse.builder()
                .dbId(dbId)
                .dbName("테스트 DB")
                .dbUserId("sa")
                .jndiYn("N")
                .lastUpdateDtime("20260101000000")
                .lastUpdateUserId("system")
                .build();
    }
}
