package com.example.spideradmin.domain.sqlquery.mapper;

import com.example.spideradmin.domain.sqlquery.dto.SqlGroupResponse;
import com.example.spideradmin.domain.sqlquery.dto.SqlQueryCreateRequest;
import com.example.spideradmin.domain.sqlquery.dto.SqlQueryHistoryResponse;
import com.example.spideradmin.domain.sqlquery.dto.SqlQueryResponse;
import com.example.spideradmin.domain.sqlquery.dto.SqlQueryUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SqlQueryMapper {

    SqlQueryResponse selectResponseById(@Param("queryId") String queryId);

    int countByQueryId(@Param("queryId") String queryId);

    void insert(
            @Param("dto") SqlQueryCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void update(
            @Param("queryId") String queryId,
            @Param("dto") SqlQueryUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void deleteById(@Param("queryId") String queryId);

    @SuppressWarnings("java:S107")
    List<SqlQueryResponse> findAllWithSearch(
            @Param("queryId") String queryId,
            @Param("queryName") String queryName,
            @Param("useYn") String useYn,
            @Param("sqlGroupId") String sqlGroupId,
            @Param("sqlGroupName") String sqlGroupName,
            @Param("sqlType") String sqlType,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    long countAllWithSearch(
            @Param("queryId") String queryId,
            @Param("queryName") String queryName,
            @Param("useYn") String useYn,
            @Param("sqlGroupId") String sqlGroupId,
            @Param("sqlGroupName") String sqlGroupName,
            @Param("sqlType") String sqlType);

    List<SqlQueryResponse> findAllForExport(
            @Param("queryId") String queryId,
            @Param("queryName") String queryName,
            @Param("useYn") String useYn,
            @Param("sqlGroupId") String sqlGroupId,
            @Param("sqlGroupName") String sqlGroupName,
            @Param("sqlType") String sqlType);

    /**
     * 수정 직전 현재 상태를 FWK_SQL_QUERY_HIS 테이블에 백업 삽입
     *
     * <p>PK: (versionId, queryId) — versionId는 System.currentTimeMillis() 문자열 사용
     */
    void insertHistory(
            @Param("data") SqlQueryResponse data,
            @Param("versionId") String versionId,
            @Param("backupDtime") String backupDtime,
            @Param("backupUserId") String backupUserId);

    /**
     * SQL 그룹 ID/명 키워드 검색 (autocomplete용, 최대 20건)
     *
     * <p>keyword가 null이면 전체 목록을 최대 20건 반환한다.
     */
    List<SqlGroupResponse> searchSqlGroups(@Param("keyword") String keyword);

    /** USE_YN 단독 업데이트 (인라인 토글용) */
    void updateUseYn(
            @Param("queryId") String queryId,
            @Param("useYn") String useYn,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /** queryId에 해당하는 이력 목록 최신순 조회 */
    List<SqlQueryHistoryResponse> findHistoryList(@Param("queryId") String queryId);

    /** 특정 VERSION_ID의 이력 단건 조회 */
    SqlQueryHistoryResponse findHistoryByVersion(
            @Param("queryId") String queryId, @Param("versionId") String versionId);
}
