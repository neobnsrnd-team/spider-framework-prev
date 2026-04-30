package com.example.spideradmin.domain.adminhistory.mapper;

import com.example.spideradmin.domain.adminhistory.dto.AdminActionLogResponse;
import com.example.spideradmin.domain.adminhistory.dto.AdminActionLogSearchRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 관리자 작업이력 Mapper (FWK_USER_ACCESS_HIS)
 */
@Mapper
public interface AdminActionLogMapper {

    /**
     * 새로운 사용자 접근 이력을 생성합니다.
     */
    void insert(
            @Param("userId") String userId,
            @Param("accessDtime") String accessDtime,
            @Param("accessIp") String accessIp,
            @Param("accessUrl") String accessUrl,
            @Param("inputData") String inputData,
            @Param("resultMessage") String resultMessage);

    /**
     * 관리자 작업이력 로그 페이징 검색
     */
    List<AdminActionLogResponse> searchLogs(
            @Param("searchDTO") AdminActionLogSearchRequest searchDTO,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    /**
     * 관리자 작업이력 로그 검색 건수
     */
    long countSearchLogs(@Param("searchDTO") AdminActionLogSearchRequest searchDTO);

    /**
     * 엑셀 내보내기용 전체 데이터 조회 (페이징 없음)
     */
    List<AdminActionLogResponse> findAllForExport(@Param("searchDTO") AdminActionLogSearchRequest searchDTO);
}
