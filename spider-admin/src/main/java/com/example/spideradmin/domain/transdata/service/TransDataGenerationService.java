package com.example.spideradmin.domain.transdata.service;

import com.example.spideradmin.domain.transdata.dto.TransDataGenerationRequest;
import com.example.spideradmin.domain.transdata.dto.TransDataSourceResponse;
import com.example.spideradmin.domain.transdata.dto.TransDataTimesResponse;
import com.example.spideradmin.domain.transdata.enums.TranResult;
import com.example.spideradmin.domain.transdata.mapper.TransDataGenerationMapper;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.util.SecurityUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransDataGenerationService {

    private final TransDataGenerationMapper transDataGenerationMapper;

    public List<TransDataSourceResponse> getSourceList(
            String tabType, String searchField, String searchValue, String orgId) {
        return switch (tabType.toUpperCase()) {
            case "TRX" -> transDataGenerationMapper.findTrxList(searchField, searchValue, orgId);
            case "MESSAGE" -> transDataGenerationMapper.findMessageList(searchField, searchValue, orgId);
            case "CODE" -> transDataGenerationMapper.findCodeGroupList(searchField, searchValue);
            case "WEBAPP" -> transDataGenerationMapper.findWebappList(searchField, searchValue);
            case "SERVICE" -> transDataGenerationMapper.findServiceList(searchField, searchValue);
            case "ERROR" -> transDataGenerationMapper.findErrorList(searchField, searchValue);
            case "COMPONENT" -> transDataGenerationMapper.findComponentList(searchField, searchValue);
            case "PROPERTY" -> transDataGenerationMapper.findPropertyList(searchField, searchValue);
            default -> throw new InvalidInputException("Unknown tab type: " + tabType);
        };
    }

    @Transactional
    public TransDataTimesResponse executeTransfer(TransDataGenerationRequest dto) {
        long tranSeqId = Long.parseLong(transDataGenerationMapper.selectNextTranSeq());
        String tranTime = String.valueOf(tranSeqId);
        String userId = SecurityUtil.getCurrentUserIdOrSystem();

        // 1. FWK_TRANS_DATA_TIMES INSERT
        transDataGenerationMapper.insertTransDataTimes(
                tranSeqId, userId, tranTime, TranResult.SUCCESS.getCode(), dto.getTranReason());

        // 2. FWK_TRANS_DATA_HIS BATCH INSERT
        List<Map<String, Object>> hisList = dto.getItems().stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("tranSeq", tranSeqId);
                    map.put("tranId", item.getTranId());
                    map.put("tranType", item.getTranType());
                    map.put("tranName", item.getTranName());
                    map.put("tranResult", TranResult.SUCCESS.getCode());
                    map.put("tranTime", tranTime);
                    return map;
                })
                .toList();

        if (!hisList.isEmpty()) {
            transDataGenerationMapper.insertTransDataHisBatch(hisList);
        }

        log.info("이행 데이터 생성 완료 - tranSeq: {}, items: {}, userId: {}", tranSeqId, hisList.size(), userId);

        return TransDataTimesResponse.builder()
                .tranSeq(tranSeqId)
                .userId(userId)
                .tranTime(tranTime)
                .tranResult(TranResult.SUCCESS.getCode())
                .tranResultName(TranResult.SUCCESS.getDescription())
                .tranReason(dto.getTranReason())
                .totalCount((long) hisList.size())
                .successCount((long) hisList.size())
                .failCount(0L)
                .build();
    }
}
