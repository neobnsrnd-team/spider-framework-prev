package com.example.spideradmin.domain.messageinstance.service;

import com.example.spideradmin.domain.messageinstance.dto.MessageInstanceResponse;
import com.example.spideradmin.domain.messageinstance.dto.MessageInstanceSearchRequest;
import com.example.spideradmin.domain.messageinstance.mapper.MessageInstanceMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전문 내역(거래추적로그) 관리 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageInstanceService {

    private final MessageInstanceMapper messageInstanceMapper;

    public PageResponse<MessageInstanceResponse> search(
            PageRequest pageRequest, MessageInstanceSearchRequest searchDTO) {

        long total = messageInstanceMapper.countAllWithSearch(
                searchDTO.getUserId(),
                searchDTO.getTrxTrackingNo(),
                searchDTO.getGlobalId(),
                searchDTO.getOrgId(),
                searchDTO.getOrgMessageId(),
                searchDTO.getTrxDateFrom(),
                searchDTO.getTrxDateTo(),
                searchDTO.getTrxTimeFrom(),
                searchDTO.getTrxTimeTo());

        List<MessageInstanceResponse> instances = messageInstanceMapper.findAllWithSearch(
                searchDTO.getUserId(),
                searchDTO.getTrxTrackingNo(),
                searchDTO.getGlobalId(),
                searchDTO.getOrgId(),
                searchDTO.getOrgMessageId(),
                searchDTO.getTrxDateFrom(),
                searchDTO.getTrxDateTo(),
                searchDTO.getTrxTimeFrom(),
                searchDTO.getTrxTimeTo(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(instances, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public List<MessageInstanceResponse> getByTrxTrackingNo(String trxTrackingNo) {
        return messageInstanceMapper.findByTrxTrackingNo(trxTrackingNo);
    }
}
