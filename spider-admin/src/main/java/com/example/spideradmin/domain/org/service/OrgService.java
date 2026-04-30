package com.example.spideradmin.domain.org.service;

import com.example.spideradmin.domain.org.dto.OrgBatchRequest;
import com.example.spideradmin.domain.org.dto.OrgResponse;
import com.example.spideradmin.domain.org.dto.OrgUpsertRequest;
import com.example.spideradmin.domain.org.mapper.OrgMapper;
import com.example.spideradmin.global.dto.PageRequest;
import com.example.spideradmin.global.dto.PageResponse;
import com.example.spideradmin.global.exception.InternalException;
import com.example.spideradmin.global.exception.InvalidInputException;
import com.example.spideradmin.global.util.ExcelColumnDefinition;
import com.example.spideradmin.global.util.ExcelExportUtil;
import com.example.spideradmin.global.util.ValidationUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrgService {

    private final OrgMapper orgMapper;

    public List<OrgResponse> getAllOrgs() {
        return orgMapper.findAll();
    }

    public PageResponse<OrgResponse> searchOrgs(PageRequest pageRequest, String searchField, String keyword) {
        int offset = pageRequest.getPage() * pageRequest.getSize();
        int limit = pageRequest.getSize();

        List<OrgResponse> content = orgMapper.findBySearchPaging(
                searchField, keyword, offset, limit, pageRequest.getSortBy(), pageRequest.getSortDirection());
        long totalCount = orgMapper.countBySearch(searchField, keyword);

        int totalPages = (int) Math.ceil((double) totalCount / pageRequest.getSize());
        PageResponse<OrgResponse> response = new PageResponse<>();
        response.setContent(content);
        response.setTotalElements(totalCount);
        response.setTotalPages(totalPages);
        response.setCurrentPage(pageRequest.getPage() + 1);
        response.setSize(pageRequest.getSize());
        response.setHasPrevious(pageRequest.getPage() > 0);
        response.setHasNext(pageRequest.getPage() + 1 < totalPages);
        return response;
    }

    public byte[] exportOrgs(String searchField, String keyword, String sortBy, String sortDirection) {
        List<OrgResponse> data = orgMapper.findAllForExport(searchField, keyword, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("기관ID", 15, "orgId"),
                new ExcelColumnDefinition("기관명", 15, "orgName"),
                new ExcelColumnDefinition("기관설명", 15, "orgDesc"),
                new ExcelColumnDefinition("시작시간", 15, "startTime"),
                new ExcelColumnDefinition("종료시간", 15, "endTime"),
                new ExcelColumnDefinition("XMLROOT TAG", 15, "xmlRootTag"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (var item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orgId", item.getOrgId());
            row.put("orgName", item.getOrgName());
            row.put("orgDesc", item.getOrgDesc());
            row.put("startTime", item.getStartTime());
            row.put("endTime", item.getEndTime());
            row.put("xmlRootTag", item.getXmlRootTag());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("기관", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    @Transactional
    public void saveBatch(OrgBatchRequest request) {
        if (request == null) {
            throw new InvalidInputException("요청 정보가 없습니다.");
        }

        List<String> deleteOrgIds = request.getDeleteOrgIds();
        if (deleteOrgIds != null) {
            for (String orgId : deleteOrgIds) {
                if (orgId == null || orgId.isBlank()) {
                    continue;
                }
                orgMapper.deleteOrgById(orgId);
            }
        }

        List<OrgUpsertRequest> upserts = request.getUpserts();
        if (upserts == null) {
            upserts = Collections.emptyList();
        }

        for (OrgUpsertRequest dto : upserts) {
            validateOrg(dto);
            if (orgMapper.countByOrgId(dto.getOrgId()) == 0) {
                orgMapper.insertOrg(dto);
            } else {
                orgMapper.updateOrg(dto);
            }
        }
    }

    private void validateOrg(OrgUpsertRequest dto) {
        if (dto == null) {
            throw new InvalidInputException("기관 정보가 없습니다.");
        }
        if (dto.getOrgId() == null || dto.getOrgId().isBlank()) {
            throw new InvalidInputException("기관ID는 필수입니다.");
        }
        if (dto.getOrgId().chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidInputException("기관ID에는 공백을 사용할 수 없습니다.");
        }
        if (!ValidationUtils.isValidTimeHHmm(dto.getStartTime())) {
            throw new InvalidInputException("시작시간은 HHmm 형식이어야 합니다.");
        }
        if (!ValidationUtils.isValidTimeHHmm(dto.getEndTime())) {
            throw new InvalidInputException("종료시간은 HHmm 형식이어야 합니다.");
        }
        if (Objects.nonNull(dto.getXmlRootTag()) && dto.getXmlRootTag().length() > 20) {
            throw new InvalidInputException("XML ROOT TAG는 20자 이내여야 합니다.");
        }
    }
}
