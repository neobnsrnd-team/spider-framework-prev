package com.example.spideradmin.domain.errorcode.service;

import com.example.spideradmin.domain.errorcode.dto.ErrorDescCreateRequest;
import com.example.spideradmin.domain.errorcode.dto.ErrorDescResponse;
import com.example.spideradmin.domain.errorcode.dto.ErrorDescUpdateRequest;
import com.example.spideradmin.domain.errorcode.mapper.ErrorDescMapper;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.AuditUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 오류코드 다국어 설명 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ErrorDescService {

    private final ErrorDescMapper errorDescMapper;

    public ErrorDescResponse getById(String errorCode, String localeCode) {
        ErrorDescResponse response = errorDescMapper.selectResponseById(errorCode, localeCode);
        if (response == null) {
            throw new NotFoundException("errorCode: " + errorCode + ", localeCode: " + localeCode);
        }
        return response;
    }

    public List<ErrorDescResponse> getByErrorCode(String errorCode) {
        return errorDescMapper.selectByErrorCode(errorCode);
    }

    @Transactional
    public ErrorDescResponse create(ErrorDescCreateRequest requestDTO) {
        // 중복 체크
        int count = errorDescMapper.countById(requestDTO.getErrorCode(), requestDTO.getLocaleCode());
        if (count > 0) {
            throw new DuplicateException(
                    "errorCode: " + requestDTO.getErrorCode() + ", localeCode: " + requestDTO.getLocaleCode());
        }

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        errorDescMapper.insert(requestDTO, now, userId);

        return errorDescMapper.selectResponseById(requestDTO.getErrorCode(), requestDTO.getLocaleCode());
    }

    @Transactional
    public ErrorDescResponse update(String errorCode, String localeCode, ErrorDescUpdateRequest requestDTO) {
        int count = errorDescMapper.countById(errorCode, localeCode);
        if (count == 0) {
            throw new NotFoundException("errorCode: " + errorCode + ", localeCode: " + localeCode);
        }

        String now = AuditUtil.now();
        String userId = AuditUtil.currentUserId();

        errorDescMapper.update(errorCode, localeCode, requestDTO, now, userId);

        return errorDescMapper.selectResponseById(errorCode, localeCode);
    }

    @Transactional
    public void delete(String errorCode, String localeCode) {
        int count = errorDescMapper.countById(errorCode, localeCode);
        if (count == 0) {
            throw new NotFoundException("errorCode: " + errorCode + ", localeCode: " + localeCode);
        }

        errorDescMapper.deleteById(errorCode, localeCode);
    }

    @Transactional
    public void deleteByErrorCode(String errorCode) {
        errorDescMapper.deleteByErrorCode(errorCode);
    }
}
