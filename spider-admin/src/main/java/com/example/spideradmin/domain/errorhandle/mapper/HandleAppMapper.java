package com.example.spideradmin.domain.errorhandle.mapper;

import com.example.spideradmin.domain.errorhandle.dto.HandleAppResponse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis Mapper for FWK_HANDLE_APP table
 * 핸들러 APP 관리
 */
public interface HandleAppMapper {

    // 존재 확인
    int countByHandleAppId(@Param("handleAppId") String handleAppId);

    // PK로 응답 DTO 조회
    HandleAppResponse selectResponseById(@Param("handleAppId") String handleAppId);

    // 전체 응답 DTO 목록 조회
    List<HandleAppResponse> selectAllResponse();

    // 삭제
    void deleteById(@Param("handleAppId") String handleAppId);
}
