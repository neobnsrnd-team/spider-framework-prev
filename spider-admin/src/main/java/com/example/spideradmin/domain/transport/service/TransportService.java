package com.example.spideradmin.domain.transport.service;

import com.example.spideradmin.domain.transport.dto.OptionResponse;
import com.example.spideradmin.domain.transport.dto.TransportBatchRequest;
import com.example.spideradmin.domain.transport.dto.TransportDeleteRequest;
import com.example.spideradmin.domain.transport.dto.TransportResponse;
import com.example.spideradmin.domain.transport.dto.TransportUpsertRequest;
import com.example.spideradmin.domain.transport.dto.TrxTypeOptionResponse;
import com.example.spideradmin.domain.transport.enums.ReqResType;
import com.example.spideradmin.domain.transport.mapper.TransportMapper;
import com.example.spideradmin.global.common.enums.OperModeType;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.util.ExcelColumnDefinition;
import com.example.spideradmin.global.util.ExcelExportUtil;
import com.example.spideradmin.global.util.ValidationUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransportService {

    private final TransportMapper transportMapper;

    public PageResponse<TransportResponse> searchTransports(
            PageRequest pageRequest, String orgId, String trxType, String ioType, String reqResType) {
        int offset = pageRequest.getPage() * pageRequest.getSize();
        int limit = pageRequest.getSize();

        List<TransportResponse> content = transportMapper.findBySearchPaging(
                orgId,
                trxType,
                ioType,
                reqResType,
                offset,
                limit,
                pageRequest.getSortBy(),
                pageRequest.getSortDirection());
        long totalCount = transportMapper.countBySearch(orgId, trxType, ioType, reqResType);

        int totalPages = (int) Math.ceil((double) totalCount / pageRequest.getSize());
        PageResponse<TransportResponse> response = new PageResponse<>();
        response.setContent(content);
        response.setTotalElements(totalCount);
        response.setTotalPages(totalPages);
        response.setCurrentPage(pageRequest.getPage() + 1);
        response.setSize(pageRequest.getSize());
        response.setHasPrevious(pageRequest.getPage() > 0);
        response.setHasNext(pageRequest.getPage() + 1 < totalPages);
        return response;
    }

    public byte[] exportTransports(
            String orgId, String trxType, String ioType, String reqResType, String sortBy, String sortDirection) {
        List<TransportResponse> data =
                transportMapper.findAllForExport(orgId, trxType, ioType, reqResType, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("기관 ID", 15, "orgId"),
                new ExcelColumnDefinition("어댑터/리스너", 15, "ioType"),
                new ExcelColumnDefinition("요청/응답", 12, "reqResType"),
                new ExcelColumnDefinition("거래유형", 15, "trxType"),
                new ExcelColumnDefinition("GATEWAY ID", 15, "gwId"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orgId", item.getOrgId());
            String ioTypeLabel = "I".equals(item.getIoType()) ? "리스너(수신)" : "어댑터(송신)";
            row.put("ioType", ioTypeLabel);
            String reqResLabel = "Q".equals(item.getReqResType()) ? "요청" : "응답";
            row.put("reqResType", reqResLabel);
            row.put("trxType", item.getTrxType());
            row.put("gwId", item.getGwId());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("기관통신", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    public List<TransportResponse> getByGatewayId(String gwId) {
        if (gwId == null || gwId.isBlank()) {
            return Collections.emptyList();
        }
        return transportMapper.findByGatewayId(gwId);
    }

    @Transactional
    public void saveBatch(TransportBatchRequest request) {
        if (request == null) {
            throw new InvalidInputException("요청 정보가 없습니다.");
        }

        List<TransportDeleteRequest> deletes = request.getDeletes();
        if (deletes != null) {
            for (TransportDeleteRequest dto : deletes) {
                if (dto == null) {
                    continue;
                }
                transportMapper.deleteTransport(dto.getOrgId(), dto.getTrxType(), dto.getIoType(), dto.getReqResType());
            }
        }

        List<TransportUpsertRequest> upserts = request.getUpserts();
        if (upserts == null) {
            upserts = Collections.emptyList();
        }

        for (TransportUpsertRequest dto : upserts) {
            validateTransport(dto);
            int count =
                    transportMapper.countByPk(dto.getOrgId(), dto.getTrxType(), dto.getIoType(), dto.getReqResType());
            if (count == 0) {
                transportMapper.insertTransport(dto);
            } else {
                transportMapper.updateTransport(dto);
            }
        }
    }

    public List<TrxTypeOptionResponse> getTrxTypeOptions() {
        return transportMapper.findTrxTypeOptions();
    }

    public List<OptionResponse> getOperModeOptions() {
        return Arrays.stream(OperModeType.values())
                .map(mode -> OptionResponse.builder()
                        .code(mode.getCode())
                        .description(mode.getDescription())
                        .build())
                .toList();
    }

    public boolean isValidTrxType(String trxType) {
        if (trxType == null || trxType.isBlank()) {
            return false;
        }
        return transportMapper.countByTrxType(trxType) > 0;
    }

    private void validateTransport(TransportUpsertRequest dto) {
        if (dto == null) {
            throw new InvalidInputException("기관통신 Gateway 맵핑 정보가 없습니다.");
        }
        if (!isValidTrxType(dto.getTrxType())) {
            throw new InvalidInputException("거래유형 값이 올바르지 않습니다.");
        }
        if (!ValidationUtils.isValidIoType(dto.getIoType())) {
            throw new InvalidInputException("어댑터/리스너 구분 값이 올바르지 않습니다.");
        }
        if (ReqResType.fromCode(dto.getReqResType()) == null) {
            throw new InvalidInputException("요청/응답 구분 값이 올바르지 않습니다.");
        }
    }
}
