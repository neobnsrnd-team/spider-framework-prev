package com.example.spider_admin.domain.trxmessage.service;

import com.example.spider_admin.domain.trxmessage.dto.MessageBrowseResponse;
import com.example.spider_admin.domain.trxmessage.dto.TrxMessageMappingUpdateRequest;
import com.example.spider_admin.domain.trxmessage.dto.TrxMessageSearchRequest;
import com.example.spider_admin.domain.trxmessage.dto.TrxMessageWithTrxResponse;
import com.example.spider_admin.domain.trxmessage.mapper.TrxMessageHistoryMapper;
import com.example.spider_admin.domain.trxmessage.mapper.TrxMessageMapper;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.AuditUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for TrxMessage management
 * Handles business logic for TrxMessage operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrxMessageService {

    private final TrxMessageMapper trxMessageMapper;
    private final TrxMessageHistoryMapper trxMessageHistoryMapper;

    // 허용할 정렬 기준 컬럼 목록
    private static final Set<String> ALLOWED_SORT_COLUMNS =
            Set.of("trxId", "orgId", "ioType", "messageId", "trxName", "lastUpdateDtime");

    /**
     * Get TrxMessages with pagination and search
     */
    public PageResponse<TrxMessageWithTrxResponse> getTrxMessagesWithSearch(
            PageRequest pageRequest, TrxMessageSearchRequest searchDTO) {

        String sortBy = pageRequest.getSortBy();
        if (sortBy != null && !sortBy.isBlank() && !ALLOWED_SORT_COLUMNS.contains(sortBy)) {
            throw new InvalidInputException("sortBy: " + sortBy);
        }

        String sortDirection = pageRequest.getSortDirection();
        if (sortDirection != null
                && !sortDirection.isBlank()
                && !("ASC".equalsIgnoreCase(sortDirection) || "DESC".equalsIgnoreCase(sortDirection))) {
            throw new InvalidInputException("sortDirection: " + sortDirection);
        }

        long total = trxMessageMapper.countAllWithTrxAndSearch(
                searchDTO.getSearchField(),
                searchDTO.getSearchValue(),
                searchDTO.getOrgIdFilter(),
                searchDTO.getIoTypeFilter(),
                searchDTO.getTrxIdFilter(),
                searchDTO.getAllowedOrgIds(),
                searchDTO.isDeduplicate());

        List<TrxMessageWithTrxResponse> list = trxMessageMapper.findAllWithTrxAndSearch(
                searchDTO.getSearchField(),
                searchDTO.getSearchValue(),
                searchDTO.getOrgIdFilter(),
                searchDTO.getIoTypeFilter(),
                searchDTO.getTrxIdFilter(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                searchDTO.getAllowedOrgIds(),
                searchDTO.isDeduplicate(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public List<TrxMessageWithTrxResponse> getAllTrxMessagesByTrxIdAndIoType(String trxId, String ioType) {
        log.info("Finding all TrxMessages by trxId: {}, ioType: {}", trxId, ioType);

        List<TrxMessageWithTrxResponse> trxMessages = trxMessageMapper.findAllByTrxIdAndIoTypeWithTrx(trxId, ioType);
        log.info("Found {} TrxMessage(s) for trxId={}, ioType={}", trxMessages.size(), trxId, ioType);

        return trxMessages;
    }

    public PageResponse<MessageBrowseResponse> browseMessagesWithPaging(
            PageRequest pageRequest, String searchField, String searchValue, String ioTypeFilter, String orgIdFilter) {

        long total = trxMessageMapper.countBrowseMessages(searchField, searchValue, ioTypeFilter, orgIdFilter);

        List<MessageBrowseResponse> messages = trxMessageMapper.browseMessagesWithPaging(
                searchField, searchValue, ioTypeFilter, orgIdFilter, pageRequest.getOffset(), pageRequest.getEndRow());

        return PageResponse.of(messages, total, pageRequest.getPage(), pageRequest.getSize());
    }

    @Transactional
    public void updateTrxMessageId(
            String trxId, String orgId, String ioType, TrxMessageMappingUpdateRequest requestDTO) {
        // 존재 확인
        int count = trxMessageMapper.countByCompositeKey(trxId, orgId, ioType);
        if (count == 0) {
            throw new NotFoundException(String.format("trxId: %s, orgId: %s, ioType: %s", trxId, orgId, ioType));
        }

        // 이력 생성 (INSERT ... SELECT)
        Integer nextVersion = trxMessageHistoryMapper.getNextVersion(trxId, orgId, ioType);
        trxMessageHistoryMapper.insertHistoryFromTrxMessage(trxId, orgId, ioType, nextVersion);

        // 부분 업데이트
        Map<String, Object> params = new HashMap<>();
        if (requestDTO.getMessageId() != null) {
            params.put("messageId", requestDTO.getMessageId());
        }
        if (requestDTO.getStdMessageId() != null) {
            params.put("stdMessageId", requestDTO.getStdMessageId());
        }
        if (requestDTO.getResMessageId() != null) {
            params.put("resMessageId", requestDTO.getResMessageId());
        }
        if (requestDTO.getStdResMessageId() != null) {
            params.put("stdResMessageId", requestDTO.getStdResMessageId());
        }
        if (requestDTO.getTimeoutSec() != null) {
            params.put("timeoutSec", requestDTO.getTimeoutSec());
        }
        if (requestDTO.getHexLogYn() != null) {
            params.put("hexLogYn", requestDTO.getHexLogYn());
        }
        if (requestDTO.getProxyResYn() != null) {
            params.put("proxyResYn", requestDTO.getProxyResYn());
        }
        if (requestDTO.getMultiResYn() != null) {
            params.put("multiResYn", requestDTO.getMultiResYn());
        }
        if (requestDTO.getMultiResType() != null) {
            params.put("multiResType", requestDTO.getMultiResType());
        }
        if (requestDTO.getResTypeFieldId() != null) {
            params.put("resTypeFieldId", requestDTO.getResTypeFieldId());
        }

        params.put("lastUpdateDtime", AuditUtil.now());
        params.put("lastUpdateUserId", AuditUtil.currentUserId());

        trxMessageMapper.updateTrxMessage(trxId, orgId, ioType, params);
    }
}
