package com.example.spideradmin.domain.transdata.service;

import com.example.spideradmin.domain.transdata.dto.TransDataDetailResponse;
import com.example.spideradmin.domain.transdata.dto.TransDataHisFailResponse;
import com.example.spideradmin.domain.transdata.dto.TransDataHisResponse;
import com.example.spideradmin.domain.transdata.dto.TransDataHisSearchRequest;
import com.example.spideradmin.domain.transdata.dto.TransDataTimesResponse;
import com.example.spideradmin.domain.transdata.dto.TransDataTimesSearchRequest;
import com.example.spideradmin.domain.transdata.enums.TranResult;
import com.example.spideradmin.domain.transdata.enums.TranType;
import com.example.spideradmin.domain.transdata.mapper.TransDataHisMapper;
import com.example.spideradmin.domain.transdata.mapper.TransDataTimesMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransDataService {

    private final TransDataTimesMapper transDataTimesMapper;
    private final TransDataHisMapper transDataHisMapper;

    public PageResponse<TransDataTimesResponse> searchTransDataTimes(
            PageRequest pageRequest, TransDataTimesSearchRequest searchDTO) {
        long total = transDataTimesMapper.countAllWithSearch(searchDTO.getUserId(), searchDTO.getTranResult());

        List<TransDataTimesResponse> list = transDataTimesMapper.findAllWithSearch(
                searchDTO.getUserId(),
                searchDTO.getTranResult(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        list.forEach(this::enrichTimesDTO);

        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public List<TransDataHisResponse> getTransDataHisDetails(Long tranSeq) {
        List<TransDataHisResponse> list = transDataHisMapper.findByTranSeq(tranSeq);
        list.forEach(this::enrichHisDTO);
        return list;
    }

    public TransDataDetailResponse getTransDataDetail(Long tranSeq) {
        TransDataDetailResponse detail = transDataTimesMapper.findDetailByTranSeq(tranSeq);
        if (detail == null) {
            throw new NotFoundException("tranSeq: " + tranSeq);
        }

        enrichDetailDTO(detail);

        List<TransDataHisResponse> list = transDataHisMapper.findByTranSeq(tranSeq);
        list.forEach(this::enrichHisDTO);

        detail.setDetails(list);

        return detail;
    }

    public PageResponse<TransDataHisResponse> searchTransDataHis(
            Long tranSeq, PageRequest pageRequest, TransDataHisSearchRequest searchDTO) {
        long total = transDataHisMapper.countByTranSeqWithSearch(
                tranSeq, searchDTO.getTranResult(), searchDTO.getTranType());

        List<TransDataHisResponse> list = transDataHisMapper.findByTranSeqWithSearch(
                tranSeq,
                searchDTO.getTranResult(),
                searchDTO.getTranType(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        list.forEach(this::enrichHisDTO);

        return PageResponse.of(list, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public TransDataHisFailResponse getTransDataHisFailDetail(Long tranSeq, String tranId, String tranType) {
        TransDataHisFailResponse dto = transDataHisMapper.findFailDetail(tranSeq, tranId, tranType);
        if (dto == null) {
            throw new NotFoundException("tranSeq: " + tranSeq);
        }

        enrichHisFailDTO(dto);

        return dto;
    }

    public byte[] exportTransDataTimes(TransDataTimesSearchRequest searchDTO, String sortBy, String sortDirection) {
        List<TransDataTimesResponse> data = transDataTimesMapper.findAllForExport(
                searchDTO.getUserId(), searchDTO.getTranResult(), sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        data.forEach(this::enrichTimesDTO);

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("이행요청자", 15, "userId"),
                new ExcelColumnDefinition("이행사유", 30, "tranReason"),
                new ExcelColumnDefinition("이행시각", 20, "tranTime"),
                new ExcelColumnDefinition("이행결과", 10, "tranResultName"),
                new ExcelColumnDefinition("총이행건수", 12, "totalCount"),
                new ExcelColumnDefinition("실패", 10, "failCount"),
                new ExcelColumnDefinition("성공", 10, "successCount"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", item.getUserId());
            row.put("tranReason", item.getTranReason());
            row.put("tranTime", item.getTranTime());
            row.put("tranResultName", item.getTranResultName());
            row.put("totalCount", item.getTotalCount());
            row.put("failCount", item.getFailCount());
            row.put("successCount", item.getSuccessCount());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("이행데이터반영", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    private void enrichTimesDTO(TransDataTimesResponse dto) {
        if (dto == null) return;
        dto.setTranResultName(resolveTranResultName(dto.getTranResult(), dto.getTranResultName()));
    }

    private void enrichHisDTO(TransDataHisResponse dto) {
        if (dto == null) return;
        if (dto.getTranType() != null && dto.getTranTypeName() == null) {
            TranType type = TranType.fromCode(dto.getTranType());
            if (type != null) {
                dto.setTranTypeName(type.getDescription());
            }
        }
        dto.setTranResultName(resolveTranResultName(dto.getTranResult(), dto.getTranResultName()));
    }

    private void enrichDetailDTO(TransDataDetailResponse dto) {
        if (dto == null) return;
        dto.setTranResultName(resolveTranResultName(dto.getTranResult(), dto.getTranResultName()));
    }

    private void enrichHisFailDTO(TransDataHisFailResponse dto) {
        if (dto == null) return;
        if (dto.getTranType() != null && dto.getTranTypeName() == null) {
            TranType type = TranType.fromCode(dto.getTranType());
            if (type != null) {
                dto.setTranTypeName(type.getDescription());
            }
        }
        dto.setTranResultName(resolveTranResultName(dto.getTranResult(), dto.getTranResultName()));
    }

    private String resolveTranResultName(String tranResult, String tranResultName) {
        if (tranResult == null || tranResultName != null) {
            return tranResultName;
        }
        TranResult result = TranResult.fromCode(tranResult);
        return result != null ? result.getDescription() : null;
    }
}
