package com.example.spideradmin.domain.accessuser.service;

import com.example.spideradmin.domain.accessuser.dto.AccessUserCreateRequest;
import com.example.spideradmin.domain.accessuser.dto.AccessUserResponse;
import com.example.spideradmin.domain.accessuser.dto.AccessUserSearchRequest;
import com.example.spideradmin.domain.accessuser.dto.AccessUserUpdateRequest;
import com.example.spideradmin.domain.accessuser.mapper.AccessUserMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.DuplicateException;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.exception.NotFoundException;
import com.example.spideradmin.global.util.AuditUtil;
import com.example.spideradmin.global.util.ExcelColumnDefinition;
import com.example.spideradmin.global.util.ExcelExportUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h3>중지거래 접근허용자 서비스 구현체</h3>
 * <p>중지거래 접근허용자 관리 비즈니스 로직을 구현합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessUserService {

    private final AccessUserMapper accessUserMapper;

    @Transactional
    public AccessUserResponse createAccessUser(AccessUserCreateRequest dto) {
        log.info(
                "Creating AccessUser: gubunType={}, trxId={}, custUserId={}",
                dto.getGubunType(),
                dto.getTrxId(),
                dto.getCustUserId());

        // 복합 PK 중복 체크
        if (accessUserMapper.existsByPk(dto.getGubunType(), dto.getTrxId(), dto.getCustUserId()) > 0) {
            throw new DuplicateException("gubunType: " + dto.getGubunType() + ", trxId: " + dto.getTrxId()
                    + ", custUserId: " + dto.getCustUserId());
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        accessUserMapper.insertAccessUser(dto, now, currentUserId);

        log.info(
                "AccessUser created successfully: gubunType={}, trxId={}, custUserId={}",
                dto.getGubunType(),
                dto.getTrxId(),
                dto.getCustUserId());

        return accessUserMapper.selectResponseByPk(dto.getGubunType(), dto.getTrxId(), dto.getCustUserId());
    }

    @Transactional
    public AccessUserResponse updateAccessUser(AccessUserUpdateRequest dto) {
        log.info(
                "Updating AccessUser: gubunType={}, trxId={}, custUserId={}",
                dto.getGubunType(),
                dto.getTrxId(),
                dto.getCustUserId());

        // 존재 확인 (복합 PK)
        if (accessUserMapper.existsByPk(dto.getGubunType(), dto.getTrxId(), dto.getCustUserId()) == 0) {
            throw new NotFoundException("gubunType: " + dto.getGubunType() + ", trxId: " + dto.getTrxId()
                    + ", custUserId: " + dto.getCustUserId());
        }

        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();

        accessUserMapper.updateAccessUser(dto, now, currentUserId);

        log.info(
                "AccessUser updated successfully: gubunType={}, trxId={}, custUserId={}",
                dto.getGubunType(),
                dto.getTrxId(),
                dto.getCustUserId());

        return accessUserMapper.selectResponseByPk(dto.getGubunType(), dto.getTrxId(), dto.getCustUserId());
    }

    @Transactional
    public void deleteAccessUser(String gubunType, String trxId, String custUserId) {
        log.info("Deleting AccessUser: gubunType={}, trxId={}, custUserId={}", gubunType, trxId, custUserId);

        // 삭제 실행 후 영향받은 행 수 확인
        int deletedRows = accessUserMapper.deleteAccessUser(gubunType, trxId, custUserId);

        if (deletedRows == 0) {
            throw new NotFoundException(
                    "gubunType: " + gubunType + ", trxId: " + trxId + ", custUserId: " + custUserId);
        }

        log.info(
                "AccessUser deleted successfully: gubunType={}, trxId={}, custUserId={}", gubunType, trxId, custUserId);
    }

    public AccessUserResponse getAccessUser(String gubunType, String trxId, String custUserId) {
        log.info("Fetching AccessUser: gubunType={}, trxId={}, custUserId={}", gubunType, trxId, custUserId);

        AccessUserResponse response = accessUserMapper.selectResponseByPk(gubunType, trxId, custUserId);
        if (response == null) {
            throw new NotFoundException(
                    "gubunType: " + gubunType + ", trxId: " + trxId + ", custUserId: " + custUserId);
        }

        return response;
    }

    public List<AccessUserResponse> getAllAccessUsers() {
        log.info("Fetching all AccessUsers");

        return accessUserMapper.findAll();
    }

    public byte[] exportAccessUsers(String trxId, String gubunType, String custUserId) {
        List<AccessUserResponse> data =
                accessUserMapper.findAllWithSearch(trxId, gubunType, custUserId, null, null, 0, Integer.MAX_VALUE);
        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }
        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("구분유형", 12, "gubunType"),
                new ExcelColumnDefinition("거래/서비스ID", 25, "trxId"),
                new ExcelColumnDefinition("접근허용 사용자ID", 25, "custUserId"),
                new ExcelColumnDefinition("사용여부", 10, "useYn"),
                new ExcelColumnDefinition("최종수정일시", 18, "lastUpdateDtime"),
                new ExcelColumnDefinition("최종수정자", 15, "lastUpdateUserId"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AccessUserResponse item : data) {
            Map<String, Object> row = new HashMap<>();
            row.put("gubunType", item.getGubunType());
            row.put("trxId", item.getTrxId());
            row.put("custUserId", item.getCustUserId());
            row.put("useYn", item.getUseYn());
            row.put("lastUpdateDtime", item.getLastUpdateDtime());
            row.put("lastUpdateUserId", item.getLastUpdateUserId());
            rows.add(row);
        }
        try {
            return ExcelExportUtil.createWorkbook("중지거래 접근허용자", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    public List<AccessUserResponse> searchAccessUsers(AccessUserSearchRequest searchDTO) {
        log.info(
                "Searching AccessUsers: trxId={}, gubunType={}, custUserId={}",
                searchDTO.getTrxId(),
                searchDTO.getGubunType(),
                searchDTO.getCustUserId());

        return accessUserMapper.findAllWithSearch(
                searchDTO.getTrxId(),
                searchDTO.getGubunType(),
                searchDTO.getCustUserId(),
                null,
                null,
                0,
                Integer.MAX_VALUE);
    }

    public PageResponse<AccessUserResponse> searchAccessUsersWithPagination(
            PageRequest pageRequest, AccessUserSearchRequest searchDTO) {
        log.info(
                "Searching AccessUsers with pagination: page={}, size={}, trxId={}, gubunType={}, custUserId={}, sortBy={}, sortDirection={}",
                pageRequest.getPage(),
                pageRequest.getSize(),
                searchDTO.getTrxId(),
                searchDTO.getGubunType(),
                searchDTO.getCustUserId(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection());

        // 검색 조건 카운트 조회
        long total = accessUserMapper.countAllWithSearch(
                searchDTO.getTrxId(), searchDTO.getGubunType(), searchDTO.getCustUserId());

        // 네이티브 ROWNUM 페이징 조회
        List<AccessUserResponse> dtos = accessUserMapper.findAllWithSearch(
                searchDTO.getTrxId(),
                searchDTO.getGubunType(),
                searchDTO.getCustUserId(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());

        return PageResponse.of(dtos, total, pageRequest.getPage(), pageRequest.getSize());
    }
}
