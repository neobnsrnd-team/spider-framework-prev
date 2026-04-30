package com.example.spideradmin.domain.errorcode.service;

import com.example.spideradmin.domain.errorcode.dto.ErrorCreateRequest;
import com.example.spideradmin.domain.errorcode.dto.ErrorDescCreateRequest;
import com.example.spideradmin.domain.errorcode.dto.ErrorDescResponse;
import com.example.spideradmin.domain.errorcode.dto.ErrorDescUpdateRequest;
import com.example.spideradmin.domain.errorcode.dto.ErrorDetailResponse;
import com.example.spideradmin.domain.errorcode.dto.ErrorResponse;
import com.example.spideradmin.domain.errorcode.dto.ErrorSearchRequest;
import com.example.spideradmin.domain.errorcode.dto.ErrorUpdateRequest;
import com.example.spideradmin.domain.errorcode.dto.ErrorWithHandleAppsResponse;
import com.example.spideradmin.domain.errorcode.enums.ErrorLevel;
import com.example.spideradmin.domain.errorcode.mapper.ErrorDescMapper;
import com.example.spideradmin.domain.errorcode.mapper.ErrorMapper;
import com.example.spideradmin.domain.errorhandle.dto.ErrorHandleAppCreateRequest;
import com.example.spideradmin.domain.errorhandle.dto.ErrorHandleAppRequest;
import com.example.spideradmin.domain.errorhandle.dto.ErrorHandleAppResponse;
import com.example.spideradmin.domain.errorhandle.service.ErrorHandleAppService;
import com.example.spideradmin.global.aop.WorkListRecord;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.AuditUtil;
import com.example.spideradmin.global.util.ExcelColumnDefinition;
import com.example.spideradmin.global.util.ExcelExportUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 오류코드 관리 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ErrorService {

    private final ErrorMapper errorMapper;
    private final ErrorDescMapper errorDescMapper;
    private final ErrorHandleAppService errorHandleAppService;

    public PageResponse<ErrorWithHandleAppsResponse> getErrors(PageRequest pageRequest, ErrorSearchRequest searchDTO) {
        String searchField = searchDTO != null ? searchDTO.getSearchField() : null;
        String searchValue = searchDTO != null ? searchDTO.getSearchValue() : null;
        String trxId = searchDTO != null ? searchDTO.getTrxId() : null;
        String handleAppId = searchDTO != null ? searchDTO.getHandleAppId() : null;

        long total = errorMapper.countWithHandleApps(searchField, searchValue, trxId, handleAppId);

        // N+1 문제 해결: 핸들러 목록을 JOIN하여 한 번의 쿼리로 조회
        List<ErrorWithHandleAppsResponse> dtos = errorMapper.searchWithHandleApps(
                searchField,
                searchValue,
                trxId,
                handleAppId,
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        // errorLevelName 설정
        dtos.forEach(dto -> dto.setErrorLevelName(ErrorLevel.getDescriptionByCode(dto.getErrorLevel())));

        return PageResponse.of(dtos, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public byte[] exportErrors(
            String searchField,
            String searchValue,
            String trxId,
            String handleAppId,
            String sortBy,
            String sortDirection) {
        List<ErrorWithHandleAppsResponse> data =
                errorMapper.findAllForExport(searchField, searchValue, trxId, handleAppId, sortBy, sortDirection);

        // errorLevelName 설정
        data.forEach(dto -> dto.setErrorLevelName(ErrorLevel.getDescriptionByCode(dto.getErrorLevel())));

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("오류코드", 15, "errorCode"),
                new ExcelColumnDefinition("오류제목", 30, "errorTitle"),
                new ExcelColumnDefinition("오류 발생원인", 15, "orgErrorCode"),
                new ExcelColumnDefinition("발생회수", 12, "errorCount"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("errorCode", item.getErrorCode());
            row.put("errorTitle", item.getErrorTitle());
            row.put("orgErrorCode", item.getOrgErrorCode());
            row.put("errorCount", item.getErrorCount());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("오류코드", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    public ErrorDetailResponse getErrorDetail(String errorCode) {
        ErrorResponse error = errorMapper.selectResponseById(errorCode);
        if (error == null) {
            throw new NotFoundException("errorCode: " + errorCode);
        }

        ErrorDetailResponse dto = ErrorDetailResponse.builder()
                .errorCode(error.getErrorCode())
                .trxId(error.getTrxId())
                .errorTitle(error.getErrorTitle())
                .errorLevel(error.getErrorLevel())
                .orgId(error.getOrgId())
                .orgErrorCode(error.getOrgErrorCode())
                .errorHttpcode(error.getErrorHttpcode())
                .lastUpdateDtime(error.getLastUpdateDtime())
                .lastUpdateUserId(error.getLastUpdateUserId())
                .errorLevelName(ErrorLevel.getDescriptionByCode(error.getErrorLevel()))
                .build();

        // 다국어 설명 조회
        List<ErrorDescResponse> descriptions = errorDescMapper.selectByErrorCode(errorCode);
        for (ErrorDescResponse desc : descriptions) {
            switch (desc.getLocaleCode()) {
                case "KO" -> {
                    dto.setKoErrorTitle(desc.getErrorTitle());
                    dto.setKoErrorCauseDesc(desc.getErrorCauseDesc());
                    dto.setKoErrorGuideDesc(desc.getErrorGuideDesc());
                    dto.setKoHelpPageUrl(desc.getHelpPageUrl());
                    dto.setKoFaqPageUrl(desc.getEtcErrorGuideDesc());
                    dto.setKoMessage(desc.getPbErrorTitle());
                }
                case "EN" -> {
                    dto.setEnErrorTitle(desc.getErrorTitle());
                    dto.setEnErrorCauseDesc(desc.getErrorCauseDesc());
                    dto.setEnErrorGuideDesc(desc.getErrorGuideDesc());
                    dto.setEnHelpPageUrl(desc.getHelpPageUrl());
                    dto.setEnFaqPageUrl(desc.getEtcErrorGuideDesc());
                    dto.setEnMessage(desc.getPbErrorTitle());
                }
                default -> {
                    dto.setKoErrorTitle(desc.getErrorTitle());
                    dto.setKoErrorCauseDesc(desc.getErrorCauseDesc());
                    dto.setKoErrorGuideDesc(desc.getErrorGuideDesc());
                    dto.setKoHelpPageUrl(desc.getHelpPageUrl());
                    dto.setKoFaqPageUrl(desc.getEtcErrorGuideDesc());
                    dto.setKoMessage(desc.getPbErrorTitle());
                }
            }
        }

        // 핸들러 목록 조회
        List<ErrorHandleAppResponse> handleApps = getErrorHandleApps(errorCode);
        dto.setHandleApps(handleApps);

        return dto;
    }

    @Transactional
    @WorkListRecord(workId = "Errorcode", crudType = "C", pkExpression = "#dto.errorCode", workName = "에러코드")
    public ErrorResponse createError(ErrorCreateRequest dto) {
        if (errorMapper.countByErrorCode(dto.getErrorCode()) > 0) {
            throw new DuplicateException("errorCode: " + dto.getErrorCode());
        }

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        // FWK_ERROR 등록
        errorMapper.insert(dto, now, userId);

        // 다국어 설명 등록
        insertDesc(
                dto.getErrorCode(),
                dto.getKoErrorTitle(),
                dto.getKoErrorGuideDesc(),
                dto.getKoErrorCauseDesc(),
                dto.getKoHelpPageUrl(),
                dto.getKoFaqPageUrl(),
                dto.getKoMessage(),
                "KO",
                now,
                userId);
        insertDesc(
                dto.getErrorCode(),
                dto.getEnErrorTitle(),
                dto.getEnErrorGuideDesc(),
                dto.getEnErrorCauseDesc(),
                dto.getEnHelpPageUrl(),
                dto.getEnFaqPageUrl(),
                dto.getEnMessage(),
                "EN",
                now,
                userId);

        return errorMapper.selectResponseById(dto.getErrorCode());
    }

    @Transactional
    @WorkListRecord(workId = "Errorcode", crudType = "U", pkExpression = "#errorCode", workName = "에러코드")
    public ErrorResponse updateError(String errorCode, ErrorUpdateRequest dto) {
        if (errorMapper.countByErrorCode(errorCode) == 0) {
            throw new NotFoundException("errorCode: " + errorCode);
        }

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        // FWK_ERROR 수정
        errorMapper.update(errorCode, dto, now, userId);

        // 다국어 설명 수정/등록
        updateOrInsertDesc(
                errorCode,
                dto.getKoErrorTitle(),
                dto.getKoErrorGuideDesc(),
                dto.getKoErrorCauseDesc(),
                dto.getKoHelpPageUrl(),
                dto.getKoFaqPageUrl(),
                dto.getKoMessage(),
                "KO",
                now,
                userId);
        updateOrInsertDesc(
                errorCode,
                dto.getEnErrorTitle(),
                dto.getEnErrorGuideDesc(),
                dto.getEnErrorCauseDesc(),
                dto.getEnHelpPageUrl(),
                dto.getEnFaqPageUrl(),
                dto.getEnMessage(),
                "EN",
                now,
                userId);

        return errorMapper.selectResponseById(errorCode);
    }

    @Transactional
    @WorkListRecord(workId = "Errorcode", crudType = "D", pkExpression = "#errorCode", workName = "에러코드")
    public void deleteError(String errorCode) {
        if (errorMapper.countByErrorCode(errorCode) == 0) {
            throw new NotFoundException("errorCode: " + errorCode);
        }

        // 관련 데이터 삭제
        errorHandleAppService.deleteByErrorCode(errorCode);
        errorDescMapper.deleteByErrorCode(errorCode);
        errorMapper.deleteById(errorCode);
    }

    public List<ErrorHandleAppResponse> getErrorHandleApps(String errorCode) {
        // N+1 문제 해결: JOIN을 통해 핸들러 이름을 함께 조회
        return errorHandleAppService.getByErrorCodeWithHandleAppName(errorCode);
    }

    @Transactional
    public void saveErrorHandleApps(String errorCode, List<ErrorHandleAppRequest> handleApps) {
        // 기존 핸들러 삭제
        errorHandleAppService.deleteByErrorCode(errorCode);

        // 새 핸들러 등록
        for (ErrorHandleAppRequest dto : handleApps) {
            ErrorHandleAppCreateRequest createDTO = ErrorHandleAppCreateRequest.builder()
                    .errorCode(errorCode)
                    .handleAppId(dto.getHandleAppId())
                    .userParamValue(dto.getUserParamValue())
                    .build();
            errorHandleAppService.create(createDTO);
        }
    }

    // ===== Helper Methods =====

    /**
     * 다국어 설명 등록 (createError용 - SELECT 없이 바로 INSERT)
     * @param errorGuideDesc 오류조치방법 (ERROR_GUIDE_DESC)
     * @param errorCauseDesc 오류 발생원인 (ERROR_CAUSE_DESC)
     */
    private void insertDesc(
            String errorCode,
            String errorTitle,
            String errorGuideDesc,
            String errorCauseDesc,
            String helpPageUrl,
            String faqPageUrl,
            String message,
            String localeCode,
            String now,
            String userId) {
        if (errorTitle != null
                || errorCauseDesc != null
                || errorGuideDesc != null
                || helpPageUrl != null
                || faqPageUrl != null
                || message != null) {
            ErrorDescCreateRequest desc = ErrorDescCreateRequest.builder()
                    .errorCode(errorCode)
                    .localeCode(localeCode)
                    .errorTitle(errorTitle)
                    .errorCauseDesc(errorCauseDesc)
                    .errorGuideDesc(errorGuideDesc)
                    .helpPageUrl(helpPageUrl)
                    .etcErrorGuideDesc(faqPageUrl) // FAQ URL -> ETC_ERROR_GUIDE_DESC
                    .pbErrorTitle(message)
                    .build();
            errorDescMapper.insert(desc, now, userId);
        }
    }

    /**
     * 다국어 설명 수정/등록 (updateError용 - 기존 데이터 확인 후 처리)
     * @param errorGuideDesc 오류조치방법 (ERROR_GUIDE_DESC)
     * @param errorCauseDesc 오류 발생원인 (ERROR_CAUSE_DESC)
     */
    private void updateOrInsertDesc(
            String errorCode,
            String errorTitle,
            String errorGuideDesc,
            String errorCauseDesc,
            String helpPageUrl,
            String faqPageUrl,
            String message,
            String localeCode,
            String now,
            String userId) {
        int count = errorDescMapper.countById(errorCode, localeCode);

        if (count > 0) {
            ErrorDescUpdateRequest updateDto = ErrorDescUpdateRequest.builder()
                    .errorTitle(errorTitle)
                    .errorCauseDesc(errorCauseDesc)
                    .errorGuideDesc(errorGuideDesc)
                    .helpPageUrl(helpPageUrl)
                    .etcErrorGuideDesc(faqPageUrl) // FAQ URL -> ETC_ERROR_GUIDE_DESC
                    .pbErrorTitle(message)
                    .build();
            errorDescMapper.update(errorCode, localeCode, updateDto, now, userId);
        } else if (errorTitle != null
                || errorCauseDesc != null
                || errorGuideDesc != null
                || helpPageUrl != null
                || faqPageUrl != null
                || message != null) {
            ErrorDescCreateRequest createDto = ErrorDescCreateRequest.builder()
                    .errorCode(errorCode)
                    .localeCode(localeCode)
                    .errorTitle(errorTitle)
                    .errorCauseDesc(errorCauseDesc)
                    .errorGuideDesc(errorGuideDesc)
                    .helpPageUrl(helpPageUrl)
                    .etcErrorGuideDesc(faqPageUrl) // FAQ URL -> ETC_ERROR_GUIDE_DESC
                    .pbErrorTitle(message)
                    .build();
            errorDescMapper.insert(createDto, now, userId);
        }
    }
}
