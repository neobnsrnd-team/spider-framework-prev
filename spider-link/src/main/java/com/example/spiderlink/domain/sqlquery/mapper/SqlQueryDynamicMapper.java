package com.example.spiderlink.domain.sqlquery.mapper;

import com.example.spiderlink.domain.sqlquery.dto.SqlQueryRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** FWK_SQL_QUERY 테이블 조회 전용 매퍼 — 동적 SQL 로더에서만 사용한다. */
@Mapper
public interface SqlQueryDynamicMapper {

    /**
     * USE_YN = 'Y' 인 활성 SQL 전체를 조회한다.
     *
     * @return 동적 등록 대상 SQL 목록
     */
    List<SqlQueryRecord> findAllActive();

    /**
     * 지정한 queryId의 SQL을 단건 조회한다 (Reload API 용).
     *
     * @param queryId FWK_SQL_QUERY.QUERY_ID
     * @return SQL 정보, 없으면 null
     */
    SqlQueryRecord findById(@Param("queryId") String queryId);
}
