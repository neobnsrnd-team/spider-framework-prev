package com.example.spider_admin.global.log.listener;

import com.example.spider_admin.domain.errorhistory.dto.ErrorHisCreateRequest;
import com.example.spider_admin.domain.errorhistory.mapper.ErrorHisMapper;
import com.example.spider_admin.global.log.event.ErrorLogEvent;
import com.example.spider_admin.global.util.StringUtil;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "log.dest.rdb.error", havingValue = "true", matchIfMissing = true)
public class RdbErrorLogListener {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;
    private static final int MAX_ERROR_TRACE_LENGTH = 4000;

    private final ErrorHisMapper errorHisMapper;

    @Async("logExecutor")
    @EventListener
    public void onErrorLog(ErrorLogEvent event) {
        try {
            String errorSerNo = event.getErrorOccurDtime()
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 6);

            ErrorHisCreateRequest dto = ErrorHisCreateRequest.builder()
                    .errorCode(event.getErrorCode())
                    .errorSerNo(errorSerNo)
                    .custUserId(StringUtil.truncateBytes(event.getUserId(), 20))
                    .errorMessage(StringUtil.truncateBytes(event.getErrorMessage(), MAX_ERROR_MESSAGE_LENGTH))
                    .errorOccurDtime(event.getErrorOccurDtime())
                    .errorUrl(StringUtil.truncateBytes(event.getAccessUrl(), 300))
                    .errorTrace(StringUtil.truncateBytes(event.getErrorTrace(), MAX_ERROR_TRACE_LENGTH))
                    .build();

            errorHisMapper.insert(dto);
        } catch (DataIntegrityViolationException e) {
            log.warn("Error log FK violation, skipping: errorCode={}", event.getErrorCode(), e);
        } catch (Exception e) {
            log.error("Failed to save error log: errorCode={}", event.getErrorCode(), e);
        }
    }
}
