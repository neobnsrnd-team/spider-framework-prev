package com.example.spider_admin.domain.gwsystem.service;

import com.example.spider_admin.domain.gwsystem.dto.SystemBatchRequest;
import com.example.spider_admin.domain.gwsystem.dto.SystemDeleteRequest;
import com.example.spider_admin.domain.gwsystem.dto.SystemUpsertRequest;
import com.example.spider_admin.domain.gwsystem.mapper.SystemMapper;
import com.example.spider_admin.global.common.enums.UseYnFlag;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.util.ValidationUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GwSystemService {

    private final SystemMapper systemMapper;

    @Transactional
    public void saveSystemBatch(String gwId, SystemBatchRequest request) {
        if (request == null) {
            throw new InvalidInputException("요청 정보가 없습니다.");
        }
        if (gwId == null || gwId.isBlank()) {
            throw new InvalidInputException("Gateway ID는 필수입니다.");
        }

        processDeletes(gwId, request.getDeletes());
        processUpserts(gwId, request.getUpserts());
    }

    private void processDeletes(String gwId, List<SystemDeleteRequest> deletes) {
        if (deletes == null) {
            return;
        }
        for (SystemDeleteRequest dto : deletes) {
            if (dto == null) {
                continue;
            }
            String targetGwId = dto.getGwId();
            if (targetGwId == null || targetGwId.isBlank()) {
                targetGwId = gwId;
            }
            systemMapper.deleteSystem(targetGwId, dto.getSystemId());
        }
    }

    private void processUpserts(String gwId, List<SystemUpsertRequest> upserts) {
        if (upserts == null) {
            return;
        }
        for (SystemUpsertRequest dto : upserts) {
            dto.setGwId(gwId);
            if (dto.getStopYn() == null || dto.getStopYn().isBlank()) {
                dto.setStopYn("N");
            }
            validateSystem(dto);
            boolean exists = systemMapper.countBySystem(gwId, dto.getSystemId()) > 0;
            if (!exists) {
                systemMapper.insertSystem(dto);
            } else {
                systemMapper.updateSystem(dto);
            }
        }
    }

    private void validateSystem(SystemUpsertRequest dto) {
        if (dto == null) {
            throw new InvalidInputException("Gateway System 정보가 없습니다.");
        }
        if (!ValidationUtils.isValidOperModeType(dto.getOperModeType())) {
            throw new InvalidInputException("운영모드 값이 올바르지 않습니다.");
        }
        if (!ValidationUtils.isValidIpv4(dto.getIp())) {
            throw new InvalidInputException("IP 형식이 올바르지 않습니다.");
        }
        if (!ValidationUtils.isValidPort(dto.getPort())) {
            throw new InvalidInputException("PORT 값이 올바르지 않습니다.");
        }
        if (dto.getStopYn() != null && !dto.getStopYn().isBlank() && UseYnFlag.fromCode(dto.getStopYn()) == null) {
            throw new InvalidInputException("상태 값이 올바르지 않습니다.");
        }
    }
}
