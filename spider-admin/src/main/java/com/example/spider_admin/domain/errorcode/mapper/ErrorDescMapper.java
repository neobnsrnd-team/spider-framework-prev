package com.example.spider_admin.domain.errorcode.mapper;

import com.example.spider_admin.domain.errorcode.dto.ErrorDescCreateRequest;
import com.example.spider_admin.domain.errorcode.dto.ErrorDescResponse;
import com.example.spider_admin.domain.errorcode.dto.ErrorDescUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis Mapper for FWK_ERROR_DESC table
 * 오류 설명 (다국어)
 */
public interface ErrorDescMapper {

    // 기본 CRUD
    ErrorDescResponse selectResponseById(@Param("errorCode") String errorCode, @Param("localeCode") String localeCode);

    List<ErrorDescResponse> selectByErrorCode(@Param("errorCode") String errorCode);

    void insert(
            @Param("dto") ErrorDescCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void update(
            @Param("errorCode") String errorCode,
            @Param("localeCode") String localeCode,
            @Param("dto") ErrorDescUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void deleteById(@Param("errorCode") String errorCode, @Param("localeCode") String localeCode);

    void deleteByErrorCode(@Param("errorCode") String errorCode);

    int countById(@Param("errorCode") String errorCode, @Param("localeCode") String localeCode);
}
