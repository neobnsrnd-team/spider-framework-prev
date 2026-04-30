package com.example.spideradmin.domain.errorhandle.service;

import com.example.spideradmin.domain.errorhandle.dto.ErrorHandleAppCreateRequest;
import com.example.spideradmin.domain.errorhandle.dto.ErrorHandleAppResponse;
import com.example.spideradmin.domain.errorhandle.mapper.ErrorHandleAppMapper;
import com.example.spideradmin.global.exception.DuplicateException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 오류코드-처리APP 매핑 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ErrorHandleAppService {

    private final ErrorHandleAppMapper errorHandleAppMapper;

    public List<ErrorHandleAppResponse> getByErrorCodeWithHandleAppName(String errorCode) {
        return errorHandleAppMapper.selectByErrorCodeWithHandleAppName(errorCode);
    }

    @Transactional
    public ErrorHandleAppResponse create(ErrorHandleAppCreateRequest requestDTO) {
        // 중복 체크
        if (errorHandleAppMapper.countById(requestDTO.getErrorCode(), requestDTO.getHandleAppId()) > 0) {
            throw new DuplicateException(
                    "errorCode: " + requestDTO.getErrorCode() + ", handleAppId: " + requestDTO.getHandleAppId());
        }

        errorHandleAppMapper.insert(requestDTO);

        return errorHandleAppMapper.selectResponseById(requestDTO.getErrorCode(), requestDTO.getHandleAppId());
    }

    @Transactional
    public void deleteByErrorCode(String errorCode) {
        errorHandleAppMapper.deleteByErrorCode(errorCode);
    }
}
