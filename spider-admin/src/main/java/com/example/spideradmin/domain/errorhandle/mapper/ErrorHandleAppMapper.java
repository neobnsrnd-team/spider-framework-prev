package com.example.spideradmin.domain.errorhandle.mapper;

import com.example.spideradmin.domain.errorhandle.dto.ErrorHandleAppCreateRequest;
import com.example.spideradmin.domain.errorhandle.dto.ErrorHandleAppResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis Mapper for FWK_ERROR_HANDLE_APP table
 * 오류별 핸들러 APP 매핑
 */
public interface ErrorHandleAppMapper {

    // 복합 PK 존재 확인
    int countById(@Param("errorCode") String errorCode, @Param("handleAppId") String handleAppId);

    // 복합 PK로 응답 DTO 조회
    ErrorHandleAppResponse selectResponseById(
            @Param("errorCode") String errorCode, @Param("handleAppId") String handleAppId);

    // 등록
    void insert(@Param("dto") ErrorHandleAppCreateRequest dto);

    // 오류코드별 삭제
    void deleteByErrorCode(@Param("errorCode") String errorCode);

    // 핸들러 이름 포함 조회 (N+1 문제 해결용)
    List<ErrorHandleAppResponse> selectByErrorCodeWithHandleAppName(@Param("errorCode") String errorCode);
}
