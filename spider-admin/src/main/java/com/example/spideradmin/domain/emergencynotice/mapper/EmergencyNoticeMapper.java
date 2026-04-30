package com.example.spideradmin.domain.emergencynotice.mapper;

import com.example.spideradmin.domain.emergencynotice.dto.EmergencyNoticeResponse;
import com.example.spideradmin.domain.emergencynotice.dto.EmergencyNoticeSaveRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 긴급공지 MyBatis Mapper
 *
 * FWK_PROPERTY 테이블의 'notice' 그룹(EMERGENCY_KO, EMERGENCY_EN, USE_YN)을 다룬다.
 */
@Mapper
public interface EmergencyNoticeMapper {

    /**
     * 언어별 긴급공지 목록 조회 (EMERGENCY_KO, EMERGENCY_EN)
     */
    List<EmergencyNoticeResponse> selectAll();

    /**
     * 노출 타입 조회 (USE_YN 행의 DEFAULT_VALUE)
     */
    String selectDisplayType();

    /**
     * 언어별 긴급공지 수정 (PROPERTY_DESC=제목, DEFAULT_VALUE=내용)
     */
    void updateNotice(
            @Param("dto") EmergencyNoticeSaveRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 노출 타입 수정 (USE_YN 행의 DEFAULT_VALUE)
     */
    void updateDisplayType(
            @Param("displayType") String displayType,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 프로퍼티 ID 존재 여부 확인 (초기 데이터 검증용)
     */
    int countByPropertyId(@Param("propertyId") String propertyId);
}
