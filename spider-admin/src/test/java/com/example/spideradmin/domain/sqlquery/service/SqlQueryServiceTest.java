package com.example.spideradmin.domain.sqlquery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.example.spideradmin.domain.sqlquery.dto.SqlQueryCreateRequest;
import com.example.spideradmin.domain.sqlquery.dto.SqlQueryResponse;
import com.example.spideradmin.domain.sqlquery.dto.SqlQuerySearchRequest;
import com.example.spideradmin.domain.sqlquery.dto.SqlQueryUpdateRequest;
import com.example.spideradmin.domain.sqlquery.mapper.SqlQueryMapper;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
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
@DisplayName("SqlQueryService 테스트")
class SqlQueryServiceTest {

    @Mock
    private SqlQueryMapper sqlQueryMapper;

    @InjectMocks
    private SqlQueryService sqlQueryService;

    // ─── getSqlQueriesWithSearch ─────────────────────────────────────

    @Test
    @DisplayName("[조회] 검색 결과를 PageResponse로 반환해야 한다")
    void getSqlQueriesWithSearch_returnsPageResponse() {
        SqlQuerySearchRequest searchDTO =
                SqlQuerySearchRequest.builder().page(1).size(10).build();

        List<SqlQueryResponse> data = List.of(buildResponse("Q001"), buildResponse("Q002"));
        given(sqlQueryMapper.countAllWithSearch(any(), any(), any(), any(), any(), any()))
                .willReturn(2L);
        given(sqlQueryMapper.findAllWithSearch(
                        any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .willReturn(data);

        PageResponse<SqlQueryResponse> result = sqlQueryService.getSqlQueriesWithSearch(searchDTO);

        assertThat(result.getTotalElements()).isEqualTo(2L);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getQueryId()).isEqualTo("Q001");
    }

    @Test
    @DisplayName("[조회] 검색 결과가 없으면 빈 content를 반환해야 한다")
    void getSqlQueriesWithSearch_noResult_returnsEmptyContent() {
        SqlQuerySearchRequest searchDTO =
                SqlQuerySearchRequest.builder().page(1).size(10).build();

        given(sqlQueryMapper.countAllWithSearch(any(), any(), any(), any(), any(), any()))
                .willReturn(0L);
        given(sqlQueryMapper.findAllWithSearch(
                        any(), any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .willReturn(List.of());

        PageResponse<SqlQueryResponse> result = sqlQueryService.getSqlQueriesWithSearch(searchDTO);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    // ─── getById ────────────────────────────────────────────────────

    @Test
    @DisplayName("[조회] 존재하는 ID이면 SqlQueryResponse를 반환해야 한다")
    void getById_exists_returnsSqlQueryResponse() {
        given(sqlQueryMapper.selectResponseById("Q001")).willReturn(buildResponse("Q001"));

        SqlQueryResponse result = sqlQueryService.getById("Q001");

        assertThat(result.getQueryId()).isEqualTo("Q001");
        assertThat(result.getQueryName()).isEqualTo("테스트 쿼리");
    }

    @Test
    @DisplayName("[조회] 존재하지 않는 ID이면 NotFoundException을 발생시켜야 한다")
    void getById_notExists_throwsNotFoundException() {
        given(sqlQueryMapper.selectResponseById("NOT-EXIST")).willReturn(null);

        assertThatThrownBy(() -> sqlQueryService.getById("NOT-EXIST")).isInstanceOf(NotFoundException.class);
    }

    // ─── create ─────────────────────────────────────────────────────

    @Test
    @DisplayName("[등록] 정상 등록 시 selectResponseById 결과를 반환해야 한다")
    void create_success_returnsSqlQueryResponse() {
        SqlQueryCreateRequest dto = buildCreateRequest("Q-NEW");
        given(sqlQueryMapper.selectResponseById("Q-NEW")).willReturn(buildResponse("Q-NEW"));

        SqlQueryResponse result = sqlQueryService.create(dto);

        assertThat(result.getQueryId()).isEqualTo("Q-NEW");
        then(sqlQueryMapper).should().insert(eq(dto), anyString(), anyString());
    }

    @Test
    @DisplayName("[등록] 중복 ID이면 DuplicateException을 발생시켜야 한다")
    void create_duplicateId_throwsDuplicateException() {
        SqlQueryCreateRequest dto = buildCreateRequest("Q-DUP");
        org.mockito.BDDMockito.willThrow(new DuplicateKeyException("duplicate"))
                .given(sqlQueryMapper)
                .insert(any(), anyString(), anyString());

        assertThatThrownBy(() -> sqlQueryService.create(dto)).isInstanceOf(DuplicateException.class);
    }

    @Test
    @DisplayName("[등록] sqlQuery에 XSS 태그 포함 시 InvalidInputException을 발생시켜야 한다")
    void create_xssSqlQuery_throwsInvalidInputException() {
        SqlQueryCreateRequest dto = buildCreateRequest("Q-XSS");
        dto.setSqlQuery("<script>alert(1)</script>");

        assertThatThrownBy(() -> sqlQueryService.create(dto)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("[등록] sqlQuery2에 XSS 태그 포함 시 InvalidInputException을 발생시켜야 한다")
    void create_xssSqlQuery2_throwsInvalidInputException() {
        SqlQueryCreateRequest dto = buildCreateRequest("Q-XSS2");
        dto.setSqlQuery2("<iframe src='evil'></iframe>");

        assertThatThrownBy(() -> sqlQueryService.create(dto)).isInstanceOf(InvalidInputException.class);
    }

    // ─── update ─────────────────────────────────────────────────────

    @Test
    @DisplayName("[수정] 존재하는 ID 수정 시 selectResponseById 결과를 반환해야 한다")
    void update_exists_returnsSqlQueryResponse() {
        SqlQueryUpdateRequest dto = buildUpdateRequest();
        given(sqlQueryMapper.countByQueryId("Q001")).willReturn(1);
        given(sqlQueryMapper.selectResponseById("Q001")).willReturn(buildResponse("Q001"));

        SqlQueryResponse result = sqlQueryService.update("Q001", dto);

        assertThat(result.getQueryId()).isEqualTo("Q001");
        then(sqlQueryMapper).should().update(eq("Q001"), eq(dto), anyString(), anyString());
    }

    @Test
    @DisplayName("[수정] 존재하지 않는 ID 수정 시 NotFoundException을 발생시켜야 한다")
    void update_notExists_throwsNotFoundException() {
        given(sqlQueryMapper.countByQueryId("NOT-EXIST")).willReturn(0);
        SqlQueryUpdateRequest dto = buildUpdateRequest();

        assertThatThrownBy(() -> sqlQueryService.update("NOT-EXIST", dto)).isInstanceOf(NotFoundException.class);
    }

    // ─── delete ─────────────────────────────────────────────────────

    @Test
    @DisplayName("[삭제] 존재하는 ID 삭제 시 deleteById를 호출해야 한다")
    void delete_exists_callsDeleteById() {
        given(sqlQueryMapper.countByQueryId("Q001")).willReturn(1);

        sqlQueryService.delete("Q001");

        then(sqlQueryMapper).should().deleteById("Q001");
    }

    @Test
    @DisplayName("[삭제] 존재하지 않는 ID 삭제 시 NotFoundException을 발생시켜야 한다")
    void delete_notExists_throwsNotFoundException() {
        given(sqlQueryMapper.countByQueryId("NOT-EXIST")).willReturn(0);

        assertThatThrownBy(() -> sqlQueryService.delete("NOT-EXIST")).isInstanceOf(NotFoundException.class);
    }

    // ─── update XSS ────────────────────────────────────────────────

    @Test
    @DisplayName("[수정] sqlQuery에 XSS 태그 포함 시 InvalidInputException을 발생시켜야 한다")
    void update_xssSqlQuery_throwsInvalidInputException() {
        given(sqlQueryMapper.countByQueryId("Q001")).willReturn(1);
        SqlQueryUpdateRequest dto = buildUpdateRequest();
        dto.setSqlQuery("<script>alert('xss')</script>");

        assertThatThrownBy(() -> sqlQueryService.update("Q001", dto)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("[수정] sqlQuery2에 XSS 태그 포함 시 InvalidInputException을 발생시켜야 한다")
    void update_xssSqlQuery2_throwsInvalidInputException() {
        given(sqlQueryMapper.countByQueryId("Q001")).willReturn(1);
        SqlQueryUpdateRequest dto = buildUpdateRequest();
        dto.setSqlQuery2("<embed src='evil'>");

        assertThatThrownBy(() -> sqlQueryService.update("Q001", dto)).isInstanceOf(InvalidInputException.class);
    }

    // ─── exportExcel ──────────────────────────────────────────────

    @Test
    @DisplayName("[엑셀] 정상 데이터가 있으면 바이트 배열을 반환해야 한다")
    void exportExcel_withData_returnsBytes() {
        SqlQuerySearchRequest searchDTO = SqlQuerySearchRequest.builder().build();
        given(sqlQueryMapper.findAllForExport(any(), any(), any(), any(), any(), any()))
                .willReturn(List.of(buildResponse("Q001"), buildResponse("Q002")));

        byte[] result = sqlQueryService.exportExcel(searchDTO);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀] 빈 데이터이면 빈 엑셀 바이트를 반환해야 한다")
    void exportExcel_emptyData_returnsBytes() {
        SqlQuerySearchRequest searchDTO = SqlQuerySearchRequest.builder().build();
        given(sqlQueryMapper.findAllForExport(any(), any(), any(), any(), any(), any()))
                .willReturn(Collections.emptyList());

        byte[] result = sqlQueryService.exportExcel(searchDTO);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("[엑셀] 검색 조건이 적용된 데이터로 엑셀을 생성해야 한다")
    void exportExcel_withSearchFilter_passesFilterToMapper() {
        SqlQuerySearchRequest searchDTO = SqlQuerySearchRequest.builder()
                .queryId("Q001")
                .queryName("테스트")
                .useYn("Y")
                .build();
        given(sqlQueryMapper.findAllForExport("Q001", "테스트", "Y", null, null, null))
                .willReturn(List.of(buildResponse("Q001")));

        byte[] result = sqlQueryService.exportExcel(searchDTO);

        assertThat(result).isNotNull();
        then(sqlQueryMapper).should().findAllForExport("Q001", "테스트", "Y", null, null, null);
    }

    // ─── getSqlQueriesWithSearch (검색 필터 적용) ─────────────────

    @Test
    @DisplayName("[조회] 검색 필터(queryId, queryName, useYn)가 mapper에 전달되어야 한다")
    void getSqlQueriesWithSearch_withFilters_passesFiltersToMapper() {
        SqlQuerySearchRequest searchDTO = SqlQuerySearchRequest.builder()
                .page(1)
                .size(10)
                .queryId("Q001")
                .queryName("테스트")
                .useYn("Y")
                .build();

        given(sqlQueryMapper.countAllWithSearch("Q001", "테스트", "Y", null, null, null))
                .willReturn(1L);
        given(sqlQueryMapper.findAllWithSearch(
                        eq("Q001"), eq("테스트"), eq("Y"), isNull(), isNull(), isNull(), any(), any(), anyInt(), anyInt()))
                .willReturn(List.of(buildResponse("Q001")));

        PageResponse<SqlQueryResponse> result = sqlQueryService.getSqlQueriesWithSearch(searchDTO);

        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        then(sqlQueryMapper).should().countAllWithSearch("Q001", "테스트", "Y", null, null, null);
    }

    @Test
    @DisplayName("[조회] 정렬 조건이 pageRequest를 통해 전달되어야 한다")
    void getSqlQueriesWithSearch_withSort_passesSortToMapper() {
        SqlQuerySearchRequest searchDTO = SqlQuerySearchRequest.builder()
                .page(1)
                .size(10)
                .sortBy("queryName")
                .sortDirection("DESC")
                .build();

        given(sqlQueryMapper.countAllWithSearch(any(), any(), any(), any(), any(), any()))
                .willReturn(0L);
        given(sqlQueryMapper.findAllWithSearch(
                        any(), any(), any(), any(), any(), any(), eq("queryName"), eq("DESC"), anyInt(), anyInt()))
                .willReturn(List.of());

        PageResponse<SqlQueryResponse> result = sqlQueryService.getSqlQueriesWithSearch(searchDTO);

        assertThat(result.getContent()).isEmpty();
        then(sqlQueryMapper)
                .should()
                .findAllWithSearch(
                        any(), any(), any(), any(), any(), any(), eq("queryName"), eq("DESC"), anyInt(), anyInt());
    }

    // ─── create (null sqlQuery2 — validateSqlText null path) ─────

    @Test
    @DisplayName("[등록] sqlQuery2가 null이어도 정상 등록되어야 한다")
    void create_nullSqlQuery2_success() {
        SqlQueryCreateRequest dto = buildCreateRequest("Q-NULL");
        dto.setSqlQuery2(null);
        given(sqlQueryMapper.selectResponseById("Q-NULL")).willReturn(buildResponse("Q-NULL"));

        SqlQueryResponse result = sqlQueryService.create(dto);

        assertThat(result.getQueryId()).isEqualTo("Q-NULL");
    }

    // ─── 헬퍼 ───────────────────────────────────────────────────────

    private SqlQueryResponse buildResponse(String queryId) {
        return SqlQueryResponse.builder()
                .queryId(queryId)
                .queryName("테스트 쿼리")
                .sqlGroupId("GRP01")
                .sqlGroupName("테스트 그룹")
                .dbId("DB01")
                .dbName("테스트 DB")
                .sqlType("SELECT")
                .execType("SYNC")
                .cacheYn("N")
                .timeOut("30")
                .resultType("MAP")
                .useYn("Y")
                .sqlQuery("SELECT 1 FROM DUAL")
                .queryDesc("테스트 설명")
                .lastUpdateDtime("20260317120000")
                .lastUpdateUserId("e2e-admin")
                .build();
    }

    private SqlQueryCreateRequest buildCreateRequest(String queryId) {
        return SqlQueryCreateRequest.builder()
                .queryId(queryId)
                .queryName("테스트 쿼리")
                .sqlGroupId("GRP01")
                .dbId("DB01")
                .sqlType("SELECT")
                .execType("SYNC")
                .cacheYn("N")
                .timeOut("30")
                .resultType("MAP")
                .useYn("Y")
                .sqlQuery("SELECT 1 FROM DUAL")
                .queryDesc("테스트 설명")
                .build();
    }

    private SqlQueryUpdateRequest buildUpdateRequest() {
        return SqlQueryUpdateRequest.builder()
                .queryName("수정된 쿼리")
                .sqlGroupId("GRP02")
                .dbId("DB02")
                .sqlType("INSERT")
                .execType("ASYNC")
                .cacheYn("Y")
                .timeOut("60")
                .resultType("LIST")
                .useYn("N")
                .sqlQuery("INSERT INTO T VALUES(1)")
                .queryDesc("수정된 설명")
                .build();
    }
}
