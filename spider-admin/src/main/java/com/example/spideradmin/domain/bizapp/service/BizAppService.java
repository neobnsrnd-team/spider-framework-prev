package com.example.spideradmin.domain.bizapp.service;

import com.example.spideradmin.domain.bizapp.dto.BizAppCreateRequest;
import com.example.spideradmin.domain.bizapp.dto.BizAppResponse;
import com.example.spideradmin.domain.bizapp.dto.BizAppSearchRequest;
import com.example.spideradmin.domain.bizapp.dto.BizAppUpdateRequest;
import com.example.spideradmin.domain.bizapp.mapper.BizAppMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.AuditUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Biz App 관리 Service */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BizAppService {

    private final BizAppMapper bizAppMapper;

    public PageResponse<BizAppResponse> getBizAppsWithSearch(BizAppSearchRequest searchDTO) {
        PageRequest pageRequest = searchDTO.toPageRequest();

        long total = bizAppMapper.countAllWithSearch(
                searchDTO.getBizAppId(), searchDTO.getBizAppName(), searchDTO.getDupCheckYn(), searchDTO.getLogYn());

        List<BizAppResponse> bizApps = bizAppMapper.findAllWithSearch(
                searchDTO.getBizAppId(),
                searchDTO.getBizAppName(),
                searchDTO.getDupCheckYn(),
                searchDTO.getLogYn(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(bizApps, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public BizAppResponse getById(String bizAppId) {
        BizAppResponse response = bizAppMapper.selectResponseById(bizAppId);
        if (response == null) {
            throw new NotFoundException("bizAppId: " + bizAppId);
        }
        return response;
    }

    @Transactional
    public BizAppResponse create(BizAppCreateRequest dto) {
        try {
            bizAppMapper.insert(dto, AuditUtil.now(), AuditUtil.currentUserId());
        } catch (DuplicateKeyException e) {
            throw new DuplicateException("bizAppId: " + dto.getBizAppId());
        }
        return bizAppMapper.selectResponseById(dto.getBizAppId());
    }

    @Transactional
    public BizAppResponse update(String bizAppId, BizAppUpdateRequest dto) {
        if (bizAppMapper.countById(bizAppId) == 0) {
            throw new NotFoundException("bizAppId: " + bizAppId);
        }
        bizAppMapper.update(bizAppId, dto, AuditUtil.now(), AuditUtil.currentUserId());
        return bizAppMapper.selectResponseById(bizAppId);
    }

    @Transactional
    public void delete(String bizAppId) {
        if (bizAppMapper.countById(bizAppId) == 0) {
            throw new NotFoundException("bizAppId: " + bizAppId);
        }
        bizAppMapper.deleteById(bizAppId);
    }
}
