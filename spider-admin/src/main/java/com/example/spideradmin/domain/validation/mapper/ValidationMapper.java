package com.example.spideradmin.domain.validation.mapper;

import com.example.spideradmin.domain.validation.dto.ValidationCreateRequest;
import com.example.spideradmin.domain.validation.dto.ValidationResponse;
import com.example.spideradmin.domain.validation.dto.ValidationUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** Validation Mapper (CRUD + Query) */
@Mapper
public interface ValidationMapper {

    ValidationResponse selectResponseById(@Param("validationId") String validationId);

    int countById(@Param("validationId") String validationId);

    void insert(
            @Param("dto") ValidationCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void update(
            @Param("validationId") String validationId,
            @Param("dto") ValidationUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    void deleteById(@Param("validationId") String validationId);

    List<ValidationResponse> findAllWithSearch(
            @Param("validationId") String validationId,
            @Param("validationDesc") String validationDesc,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("endRow") int endRow);

    long countAllWithSearch(@Param("validationId") String validationId, @Param("validationDesc") String validationDesc);

    List<ValidationResponse> findAllForExport(
            @Param("validationId") String validationId,
            @Param("validationDesc") String validationDesc,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection);
}
