package com.example.spideradmin.domain.errorhistory.mapper;

import com.example.spideradmin.domain.errorhistory.dto.ErrorHisCreateRequest;
import com.example.spideradmin.domain.errorhistory.dto.ErrorHisResponse;
import com.example.spideradmin.domain.errorhistory.dto.ErrorHisSearchRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis Command Mapper for FWK_ERROR_HIS table
 * 오류 발생 이력 관리 (CQRS-lite: Command)
 */
@Mapper
public interface ErrorHisMapper {

    /**
     * 복합키로 오류 발생 이력 응답 DTO 조회
     */
    ErrorHisResponse selectResponseById(@Param("errorCode") String errorCode, @Param("errorSerNo") String errorSerNo);

    /**
     * 오류 발생 이력 등록
     */
    void insert(@Param("dto") ErrorHisCreateRequest dto);

    /**
     * 오류 발생 이력 검색 (오류코드 정보 포함)
     */
    List<ErrorHisResponse> searchWithErrorInfo(
            @Param("search") ErrorHisSearchRequest search, @Param("offset") int offset, @Param("endRow") int endRow);

    /**
     * 오류 발생 이력 건수 조회
     */
    long countWithErrorInfo(@Param("search") ErrorHisSearchRequest search);

    /**
     * 엑셀 내보내기용 전체 조회 (페이징 없음)
     */
    List<ErrorHisResponse> findAllForExport(@Param("search") ErrorHisSearchRequest search);
}
