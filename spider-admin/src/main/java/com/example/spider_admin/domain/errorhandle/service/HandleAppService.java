package com.example.spider_admin.domain.errorhandle.service;

import com.example.spider_admin.domain.errorhandle.dto.HandleAppResponse;
import com.example.spider_admin.domain.errorhandle.mapper.HandleAppMapper;
import com.example.spider_admin.global.exception.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 핸들러 APP 관리 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HandleAppService {

    private final HandleAppMapper handleAppMapper;

    public List<HandleAppResponse> getAllHandleApps() {
        return handleAppMapper.selectAllResponse();
    }

    public HandleAppResponse getHandleApp(String handleAppId) {
        HandleAppResponse response = handleAppMapper.selectResponseById(handleAppId);
        if (response == null) {
            throw new NotFoundException("handleAppId: " + handleAppId);
        }
        return response;
    }
}
