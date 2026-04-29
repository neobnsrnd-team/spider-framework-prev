package com.example.spider_admin.domain.codetemplate.service;

import com.example.spider_admin.domain.codetemplate.dto.CodeTemplateCreateRequest;
import com.example.spider_admin.domain.codetemplate.dto.CodeTemplateResponse;
import com.example.spider_admin.domain.codetemplate.dto.CodeTemplateUpdateRequest;
import com.example.spider_admin.domain.codetemplate.mapper.CodeTemplateMapper;
import com.example.spider_admin.domain.transaction.mapper.TrxMapper;
import com.example.spider_admin.domain.trxmessage.dto.TrxMessageResponse;
import com.example.spider_admin.global.dto.PageRequest;
import com.example.spider_admin.global.dto.PageResponse;
import com.example.spider_admin.global.exception.DuplicateException;
import com.example.spider_admin.global.exception.InternalException;
import com.example.spider_admin.global.exception.InvalidInputException;
import com.example.spider_admin.global.exception.NotFoundException;
import com.example.spider_admin.global.util.AuditUtil;
import com.example.spider_admin.global.util.ExcelColumnDefinition;
import com.example.spider_admin.global.util.ExcelExportUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeTemplateService {

    private final CodeTemplateMapper codeTemplateMapper;
    private final TrxMapper trxMapper;

    public PageResponse<CodeTemplateResponse> getCodeTemplates(PageRequest pageRequest) {
        long total = codeTemplateMapper.countAllWithSearch(pageRequest.getSearchField(), pageRequest.getSearchValue());
        List<CodeTemplateResponse> templates = codeTemplateMapper.findAllWithSearch(
                pageRequest.getSearchField(),
                pageRequest.getSearchValue(),
                pageRequest.getSortBy(),
                pageRequest.getSortDirection(),
                pageRequest.getOffset(),
                pageRequest.getEndRow());
        return PageResponse.of(templates, total, pageRequest.getPage(), pageRequest.getSize());
    }

    public CodeTemplateResponse getCodeTemplate(String templateId) {
        CodeTemplateResponse template = codeTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new NotFoundException("templateId: " + templateId);
        }
        return template;
    }

    @Transactional
    public CodeTemplateResponse createCodeTemplate(CodeTemplateCreateRequest dto) {
        if (codeTemplateMapper.countByTemplateId(dto.getTemplateId()) > 0) {
            throw new DuplicateException("templateId: " + dto.getTemplateId());
        }
        if (dto.getUseYn() == null) {
            dto.setUseYn("Y");
        }
        if (dto.getSortOrder() == null) {
            dto.setSortOrder(0);
        }
        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();
        codeTemplateMapper.insert(dto, now, currentUserId);
        return codeTemplateMapper.selectById(dto.getTemplateId());
    }

    @Transactional
    public CodeTemplateResponse updateCodeTemplate(String templateId, CodeTemplateUpdateRequest dto) {
        if (codeTemplateMapper.countByTemplateId(templateId) == 0) {
            throw new NotFoundException("templateId: " + templateId);
        }
        if (dto.getUseYn() == null) {
            dto.setUseYn("Y");
        }
        if (dto.getSortOrder() == null) {
            dto.setSortOrder(0);
        }
        String now = AuditUtil.now();
        String currentUserId = AuditUtil.currentUserId();
        codeTemplateMapper.update(templateId, dto, now, currentUserId);
        return codeTemplateMapper.selectById(templateId);
    }

    @Transactional
    public void deleteCodeTemplate(String templateId) {
        if (codeTemplateMapper.countByTemplateId(templateId) == 0) {
            throw new NotFoundException("templateId: " + templateId);
        }
        codeTemplateMapper.deleteById(templateId);
    }

    public byte[] exportCodeTemplates(String searchField, String searchValue, String sortBy, String sortDirection) {
        List<CodeTemplateResponse> data =
                codeTemplateMapper.findAllForExport(searchField, searchValue, sortBy, sortDirection);

        if (!ExcelExportUtil.isWithinLimit(data.size())) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + data.size());
        }

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("템플릿ID", 20, "templateId"),
                new ExcelColumnDefinition("템플릿명", 25, "templateName"),
                new ExcelColumnDefinition("타입", 10, "templateType"),
                new ExcelColumnDefinition("설명", 40, "description"),
                new ExcelColumnDefinition("사용여부", 10, "useYn"),
                new ExcelColumnDefinition("정렬순서", 10, "sortOrder"),
                new ExcelColumnDefinition("최종수정일시", 20, "lastUpdateDtime"),
                new ExcelColumnDefinition("최종수정자", 15, "lastUpdateUserId"));

        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (CodeTemplateResponse item : data) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("templateId", item.getTemplateId());
            row.put("templateName", item.getTemplateName());
            row.put("templateType", item.getTemplateType());
            row.put("description", item.getDescription());
            row.put("useYn", item.getUseYn());
            row.put("sortOrder", item.getSortOrder());
            row.put("lastUpdateDtime", item.getLastUpdateDtime());
            row.put("lastUpdateUserId", item.getLastUpdateUserId());
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("코드 템플릿", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    public byte[] generateSourceZip(String trxId, String orgId) {
        var trx = trxMapper.findTrxDetailById(trxId);
        if (trx == null) {
            throw new NotFoundException("trxId: " + trxId);
        }

        List<TrxMessageResponse> messages = trxMapper.findMessagesByTrxId(trxId);

        // IO_TYPE='O' → 요청전문, IO_TYPE='I' → 응답전문
        TrxMessageResponse reqMsg = messages.stream()
                .filter(m ->
                        "O".equals(m.getIoType()) && (orgId == null || orgId.isBlank() || orgId.equals(m.getOrgId())))
                .findFirst()
                .orElse(messages.stream()
                        .filter(m -> "O".equals(m.getIoType()))
                        .findFirst()
                        .orElse(null));

        TrxMessageResponse resMsg = messages.stream()
                .filter(m ->
                        "I".equals(m.getIoType()) && (orgId == null || orgId.isBlank() || orgId.equals(m.getOrgId())))
                .findFirst()
                .orElse(messages.stream()
                        .filter(m -> "I".equals(m.getIoType()))
                        .findFirst()
                        .orElse(null));

        List<CodeTemplateResponse> templates = codeTemplateMapper.findAllActive();
        if (templates.isEmpty()) {
            throw new NotFoundException("사용 가능한 템플릿이 없습니다");
        }

        Map<String, Object> model = new HashMap<>();
        model.put("trxId", trx.getTrxId());
        model.put("trxName", trx.getTrxName() != null ? trx.getTrxName() : trx.getTrxId());
        model.put("className", toPascalCase(trx.getTrxId()));
        model.put("packageName", "com.example");
        model.put("orgId", orgId != null ? orgId : (reqMsg != null ? reqMsg.getOrgId() : ""));
        model.put("reqMessageId", reqMsg != null ? reqMsg.getMessageId() : "");
        model.put("reqMessageName", reqMsg != null ? nvl(reqMsg.getMessageName(), reqMsg.getMessageId()) : "");
        model.put("resMessageId", resMsg != null ? resMsg.getMessageId() : "");
        model.put("resMessageName", resMsg != null ? nvl(resMsg.getMessageName(), resMsg.getMessageId()) : "");
        // 하위 호환 변수 (기존 템플릿에서 ${messageId} 사용 시)
        model.put("messageId", model.get("reqMessageId"));
        model.put("messageName", model.get("reqMessageName"));

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (CodeTemplateResponse template : templates) {
                    String rendered = renderTemplate(template.getTemplateBody(), model);
                    String ext = "XML".equalsIgnoreCase(template.getTemplateType()) ? ".xml" : ".java";
                    String fileName = toPascalCase(trx.getTrxId()) + template.getTemplateName() + ext;
                    ZipEntry entry = new ZipEntry(fileName);
                    zos.putNextEntry(entry);
                    zos.write(rendered.getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new InternalException("ZIP 생성 중 오류가 발생했습니다", e);
        }
    }

    private String renderTemplate(String templateBody, Map<String, Object> model) {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
        cfg.setDefaultEncoding("UTF-8");
        try {
            Template template = new Template("t", new StringReader(templateBody), cfg);
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            throw new InternalException("템플릿 렌더링 중 오류: " + e.getMessage(), e);
        }
    }

    private String toPascalCase(String input) {
        if (input == null || input.isEmpty()) return input;
        String[] parts = input.split("[_\\-]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    private String nvl(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
