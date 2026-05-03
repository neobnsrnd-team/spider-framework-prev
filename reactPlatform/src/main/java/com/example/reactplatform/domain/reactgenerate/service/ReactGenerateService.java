package com.example.reactplatform.domain.reactgenerate.service;

import com.example.reactplatform.domain.reactgenerate.ai.client.ClaudeApiClient;
import com.example.reactplatform.domain.reactgenerate.ai.prompt.PromptBuilder;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateApprovalResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateEntity;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateHistoryResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateRequest;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateSearchRequest;
import com.example.reactplatform.domain.reactgenerate.dto.ReactRegenerateRequest;
import com.example.reactplatform.domain.reactgenerate.enums.BrandType;
import com.example.reactplatform.domain.reactgenerate.enums.DomainType;
import com.example.reactplatform.domain.reactgenerate.enums.ReactGenerateStatus;
import com.example.reactplatform.domain.reactgenerate.figma.FigmaDesignContext;
import com.example.reactplatform.domain.reactgenerate.figma.FigmaDesignExtractor;
import com.example.reactplatform.domain.reactgenerate.figma.FigmaUrlParser;
import com.example.reactplatform.domain.reactgenerate.figma.client.FigmaApiClient;
import com.example.reactplatform.domain.reactgenerate.figma.client.FigmaNodeResponse;
import com.example.reactplatform.domain.reactgenerate.mapper.ReactGenerateMapper;
import com.example.reactplatform.domain.reactgenerate.validator.CodeValidationResult;
import com.example.reactplatform.domain.reactgenerate.validator.CodeValidator;
import com.example.reactplatform.global.exception.InternalException;
import com.example.reactplatform.global.exception.InvalidInputException;
import com.example.reactplatform.global.exception.NotFoundException;
import com.example.reactplatform.global.exception.base.BaseException;
import com.example.reactplatform.global.log.event.ErrorLogEvent;
import com.example.reactplatform.global.util.ExcelColumnDefinition;
import com.example.reactplatform.global.util.ExcelExportUtil;
import com.example.reactplatform.global.util.TraceIdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactGenerateService {

    private final ReactGenerateMapper reactGenerateMapper;
    private final PromptBuilder promptBuilder;
    private final ClaudeApiClient claudeApiClient;
    private final FigmaApiClient figmaApiClient;
    private final FigmaDesignExtractor figmaDesignExtractor;
    private final CodeValidator codeValidator;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Figma URL과 brand·domain 등을 받아 Claude API로 React 코드를 생성하고 DB에 저장한다.
     *
     * @param request   title, category, description, figmaUrl, brand, domain, componentName, requirements
     * @param createdBy 생성 요청자 ID (로그인 사용자)
     * @return 생성된 코드와 메타 정보
     */
    public ReactGenerateResponse generate(ReactGenerateRequest request, String createdBy) {
        // domain 미입력 시 BANKING 기본값 적용
        DomainType effectiveDomain = request.getDomain() != null ? request.getDomain() : DomainType.BANKING;

        String categoryStr = request.getCategory() != null ? request.getCategory().name() : null;

        log.info(
                "React 코드 생성 요청 — figmaUrl: {}, brand: {}, domain: {}, category: {}, userId: {}",
                request.getFigmaUrl(),
                request.getBrand(),
                effectiveDomain,
                categoryStr,
                createdBy);

        // 실패 이력 저장에 필요하므로 try 바깥에서 미리 생성
        String id = UUID.randomUUID().toString();
        String now = LocalDateTime.now().format(FORMATTER);

        // componentName 확정값: Figma에서 가져오기 전 실패 시 "Unknown" 폴백
        // 실제 값은 Figma 디자인 컨텍스트 추출 후 갱신된다
        String effectiveComponentName =
                (request.getComponentName() != null && !request.getComponentName().isBlank())
                        ? request.getComponentName()
                        : "Unknown";

        // 어느 단계에서 실패해도 그 시점까지 수집된 값을 실패 이력에 기록하기 위해 바깥에 선언
        String systemPrompt = null;
        String userPrompt = null;
        String reactCode = null;
        String figmaJson = null;

        try {
            // 1. Figma URL에서 fileKey, nodeId 파싱
            FigmaUrlParser.ParsedFigmaUrl parsed = FigmaUrlParser.parse(request.getFigmaUrl());
            log.info("Figma URL 파싱 완료 — fileKey: {}, nodeId: {}", parsed.getFileKey(), parsed.getNodeId());

            // 2. Figma API 호출로 디자인 노드 데이터 수신
            FigmaNodeResponse figmaNodeResponse = figmaApiClient.getNode(parsed.getFileKey(), parsed.getNodeId());

            // Figma 원시 응답을 JSON 문자열로 직렬화하여 감사 이력 저장
            figmaJson = toJson(figmaNodeResponse);

            // 3. 원시 Figma 응답에서 Claude 프롬프트용 디자인 컨텍스트 추출
            FigmaDesignContext designContext =
                    figmaDesignExtractor.extract(figmaNodeResponse, parsed.getNodeId(), request.getFigmaUrl());
            log.info(
                    "Figma 디자인 추출 완료 — component: {} ({}), size: {}×{}",
                    designContext.getComponentName(),
                    designContext.getComponentType(),
                    designContext.getWidth(),
                    designContext.getHeight());

            // componentName 미입력 시 Figma 컴포넌트명으로 대체
            if (request.getComponentName() == null || request.getComponentName().isBlank()) {
                effectiveComponentName = designContext.getComponentName();
            }

            // 4. system / user prompt 조립
            systemPrompt = promptBuilder.buildSystemPrompt();
            userPrompt = promptBuilder.buildUserPrompt(
                    designContext,
                    request.getBrand(),
                    effectiveDomain,
                    effectiveComponentName,
                    request.getTitle(),
                    categoryStr,
                    request.getDescription(),
                    request.getRequirements());

            // 5. Claude API 호출하여 React 코드 생성
            reactCode = claudeApiClient.generate(systemPrompt, userPrompt);

            // 6. 보안 패턴 검증 (Java 정규표현식 기반)
            CodeValidationResult validation = codeValidator.validate(reactCode);
            if (!validation.isPassed()) {
                log.warn("React 코드 보안 검증 실패 — errors: {}", String.join(" | ", validation.getErrors()));
                throw new InvalidInputException("보안 검증 실패: " + String.join(", ", validation.getErrors()));
            }
            if (!validation.getWarnings().isEmpty()) {
                log.warn("React 코드 보안 경고 — warnings: {}", String.join(" | ", validation.getWarnings()));
            }

            // 7. DB 저장 (초기 상태: GENERATED) — 구조화 필드를 전용 컬럼에 저장
            reactGenerateMapper.insert(buildEntity(
                    id, request.getFigmaUrl(), request.getBrand(), effectiveDomain,
                    effectiveComponentName, categoryStr, request.getTitle(), request.getDescription(),
                    request.getRequirements(), figmaJson,
                    systemPrompt, userPrompt, reactCode, null, ReactGenerateStatus.GENERATED.name(),
                    createdBy, now, null, null));

            log.info("React 코드 생성 완료 — codeId: {}", id);

            return ReactGenerateResponse.builder()
                    .codeId(id)
                    .title(request.getTitle())
                    .category(categoryStr)
                    .description(request.getDescription())
                    .figmaUrl(request.getFigmaUrl())
                    .brand(request.getBrand().name())
                    .domain(effectiveDomain.name())
                    .componentName(effectiveComponentName)
                    .reactCode(reactCode)
                    .status(ReactGenerateStatus.GENERATED.name())
                    .createDtime(now)
                    // WARN 경고가 없으면 null을 반환해 프론트엔드에서 불필요한 렌더링 방지
                    .validationWarnings(validation.getWarnings().isEmpty() ? null : validation.getWarnings())
                    .build();

        } catch (Exception e) {
            // 생성 파이프라인 어느 단계에서든 실패 시 이력 저장 후 예외 재전파
            // null 필드는 해당 단계에 도달하기 전에 실패했음을 의미

            // BaseException은 getMessage()가 ErrorType 고정 문구를 반환하므로
            // detailMessage(실제 상세 오류)가 있으면 우선 사용한다
            String failReason = (e instanceof BaseException be && be.getDetailMessage() != null)
                    ? be.getDetailMessage()
                    : e.getMessage();

            log.error(
                    "React 코드 생성 실패 — codeId: {}, brand: {}, domain: {}, error: {}",
                    id,
                    request.getBrand(),
                    effectiveDomain,
                    failReason);
            reactGenerateMapper.insert(buildEntity(
                    id, request.getFigmaUrl(), request.getBrand(), effectiveDomain,
                    effectiveComponentName, categoryStr, request.getTitle(), request.getDescription(),
                    request.getRequirements(), figmaJson,
                    systemPrompt, userPrompt, reactCode, failReason, ReactGenerateStatus.FAILED.name(),
                    createdBy, now, null, null));
            throw e; // 원래 예외를 그대로 재전파 → GlobalExceptionHandler에서 처리
        }
    }

    /**
     * 기존 생성 이력을 기반으로 변경 요청사항을 반영하여 React 코드를 재생성한다.
     *
     * <p>Figma API를 재호출하지 않고 원본 레코드의 figmaJson을 재사용한다.
     * figmaJson이 null인 경우(직렬화 실패 이력)에는 Figma API를 재호출하여 보완한다.
     *
     * <p>재생성 이력 체인:
     * <ul>
     *   <li>refCodeId = 직계 부모 codeId</li>
     *   <li>rootCodeId = 부모의 rootCodeId가 있으면 그 값, 없으면 부모의 codeId (체인 최상위)</li>
     * </ul>
     *
     * @param refCodeId  재생성 기준이 되는 원본 코드 ID
     * @param request    변경 요청사항
     * @param createdBy  재생성 요청자 ID
     * @return 재생성된 코드와 메타 정보
     * @throws NotFoundException 원본 codeId가 존재하지 않을 때
     */
    public ReactGenerateResponse regenerate(String refCodeId, ReactRegenerateRequest request, String createdBy) {
        ReactGenerateResponse original = reactGenerateMapper.selectById(refCodeId);
        if (original == null) {
            throw new NotFoundException("원본 생성 이력을 찾을 수 없습니다. codeId=" + refCodeId);
        }

        // rootCodeId 체인 계산: 원본이 이미 재생성본이면 그 rootCodeId를 승계, 최초면 원본 자신이 root
        String rootCodeId = original.getRootCodeId() != null ? original.getRootCodeId() : refCodeId;

        BrandType brand = BrandType.valueOf(original.getBrand());
        DomainType effectiveDomain = original.getDomain() != null
                ? DomainType.valueOf(original.getDomain())
                : DomainType.BANKING;
        String effectiveComponentName = original.getComponentName() != null
                ? original.getComponentName()
                : "Unknown";
        String categoryStr = original.getCategory();

        log.info(
                "React 코드 재생성 요청 — refCodeId: {}, brand: {}, domain: {}, userId: {}",
                refCodeId, brand, effectiveDomain, createdBy);

        String id = UUID.randomUUID().toString();
        String now = LocalDateTime.now().format(FORMATTER);

        String systemPrompt = null;
        String userPrompt = null;
        String reactCode = null;
        String figmaJson = original.getFigmaJson();

        try {
            // figmaJson 역직렬화 시도 — 실패 시 null로 처리하여 Figma API 재호출 폴백
            FigmaNodeResponse cachedNodeResponse = null;
            if (figmaJson != null) {
                try {
                    cachedNodeResponse = objectMapper.readValue(figmaJson, FigmaNodeResponse.class);
                } catch (JsonProcessingException parseEx) {
                    // 역직렬화 실패(스키마 변경 등): null로 처리하고 API 재호출로 복구
                    log.warn("재생성: figmaJson 역직렬화 실패 — Figma API 재호출로 복구, refCodeId: {}", refCodeId, parseEx);
                    figmaJson = null;
                }
            }

            FigmaUrlParser.ParsedFigmaUrl parsed = FigmaUrlParser.parse(original.getFigmaUrl());
            FigmaNodeResponse figmaNodeResponse;
            if (cachedNodeResponse != null) {
                figmaNodeResponse = cachedNodeResponse;
            } else {
                // figmaJson 미저장 또는 역직렬화 실패: Figma API 재호출
                log.warn("재생성: Figma API 재호출 진행 — refCodeId: {}", refCodeId);
                figmaNodeResponse = figmaApiClient.getNode(parsed.getFileKey(), parsed.getNodeId());
                figmaJson = toJson(figmaNodeResponse);
            }

            FigmaDesignContext designContext =
                    figmaDesignExtractor.extract(figmaNodeResponse, parsed.getNodeId(), original.getFigmaUrl());

            log.info("재생성: 디자인 컨텍스트 추출 완료 — component: {}", designContext.getComponentName());

            systemPrompt = promptBuilder.buildSystemPrompt();
            userPrompt = promptBuilder.buildRegenerateUserPrompt(
                    designContext,
                    brand,
                    effectiveDomain,
                    effectiveComponentName,
                    original.getTitle(),
                    categoryStr,
                    original.getDescription(),
                    original.getReactCode(),
                    request.getRequirements());

            reactCode = claudeApiClient.generate(systemPrompt, userPrompt);

            CodeValidationResult validation = codeValidator.validate(reactCode);
            if (!validation.isPassed()) {
                log.warn("재생성 코드 보안 검증 실패 — errors: {}", String.join(" | ", validation.getErrors()));
                throw new InvalidInputException("보안 검증 실패: " + String.join(", ", validation.getErrors()));
            }
            if (!validation.getWarnings().isEmpty()) {
                log.warn("재생성 코드 보안 경고 — warnings: {}", String.join(" | ", validation.getWarnings()));
            }

            reactGenerateMapper.insert(buildEntity(
                    id, original.getFigmaUrl(), brand, effectiveDomain, effectiveComponentName,
                    categoryStr, original.getTitle(), original.getDescription(), request.getRequirements(),
                    figmaJson, systemPrompt, userPrompt, reactCode,
                    null, ReactGenerateStatus.GENERATED.name(), createdBy, now, refCodeId, rootCodeId));

            log.info("React 코드 재생성 완료 — codeId: {}, refCodeId: {}", id, refCodeId);

            return ReactGenerateResponse.builder()
                    .codeId(id)
                    .title(original.getTitle())
                    .category(categoryStr)
                    .description(original.getDescription())
                    .figmaUrl(original.getFigmaUrl())
                    .brand(brand.name())
                    .domain(effectiveDomain.name())
                    .componentName(effectiveComponentName)
                    .requirements(request.getRequirements())
                    .reactCode(reactCode)
                    .status(ReactGenerateStatus.GENERATED.name())
                    .createDtime(now)
                    .refCodeId(refCodeId)
                    .rootCodeId(rootCodeId)
                    .validationWarnings(validation.getWarnings().isEmpty() ? null : validation.getWarnings())
                    .build();

        } catch (Exception e) {
            // generate()와 동일하게 Exception 범위로 잡아 checked 예외 발생 시에도 FAILED 이력을 보장한다
            String failReason = (e instanceof BaseException be && be.getDetailMessage() != null)
                    ? be.getDetailMessage()
                    : e.getMessage();
            log.error("React 코드 재생성 실패 — codeId: {}, refCodeId: {}, error: {}", id, refCodeId, failReason);
            reactGenerateMapper.insert(buildEntity(
                    id, original.getFigmaUrl(), brand, effectiveDomain, effectiveComponentName,
                    categoryStr, original.getTitle(), original.getDescription(), request.getRequirements(),
                    figmaJson, systemPrompt, userPrompt, reactCode,
                    failReason, ReactGenerateStatus.FAILED.name(), createdBy, now, refCodeId, rootCodeId));
            throw e;
        }
    }

    /**
     * 생성된 코드를 승인 요청 상태로 변경한다.
     *
     * <p>클라이언트 측 버튼 노출 조건(작성자 여부)과 별개로,
     * 서버에서도 요청자가 코드 작성자인지 검증하여 API 직접 호출 우회를 방지한다.
     *
     * @param id            승인 요청할 코드 ID
     * @param requestUserId 요청자 ID (로그인 사용자)
     * @throws InvalidInputException 요청자가 코드 작성자가 아닐 때
     */
    public ReactGenerateApprovalResponse requestApproval(String id, String requestUserId) {
        ReactGenerateResponse existing = reactGenerateMapper.selectById(id);
        if (existing == null) {
            throw new NotFoundException("생성 결과를 찾을 수 없습니다. codeId=" + id);
        }
        // 작성자 본인만 승인 요청 가능 — 클라이언트 우회 방지
        if (!requestUserId.equals(existing.getCreateUserId())) {
            throw new InvalidInputException("코드 작성자만 승인 요청할 수 있습니다.");
        }

        reactGenerateMapper.updateStatus(id, ReactGenerateStatus.PENDING_APPROVAL.name(), null, null, null);
        log.info("승인 요청 — codeId: {}, requestUserId: {}", id, requestUserId);

        return ReactGenerateApprovalResponse.builder()
                .codeId(id)
                .status(ReactGenerateStatus.PENDING_APPROVAL.name())
                .build();
    }

    /**
     * Preview App에서 발생한 렌더링 오류를 기록한다.
     *
     * <p>브라우저 side에서 catch된 오류는 서버까지 전달되지 않으므로,
     * 클라이언트가 명시적으로 이 메서드를 호출해야 한다.
     *
     * <ul>
     *   <li>FWK_ERROR_HIS: ErrorLogEvent 발행으로 시스템 공통 오류 이력 저장</li>
     *   <li>FWK_REACT_CODE_HIS: codeId가 있으면 해당 코드 레코드를 FAILED 처리</li>
     * </ul>
     *
     * @param codeId       렌더링 실패한 코드의 CODE_ID (없으면 null)
     * @param errorMessage 브라우저에서 전달한 오류 메시지
     * @param userId       요청자 ID
     * @param requestUri   요청 URI
     */
    public void logRenderError(String codeId, String errorMessage, String userId, String requestUri) {
        String now = LocalDateTime.now().format(FORMATTER);

        // FWK_ERROR_HIS: 시스템 공통 오류 이력 — 이벤트 발행 실패가 비즈니스 로직(updateToFailed)을 막으면 안 됨
        try {
            eventPublisher.publishEvent(new ErrorLogEvent(
                    TraceIdUtil.get(),
                    "RENDER_ERROR",
                    errorMessage,
                    null, // 클라이언트 오류이므로 서버 스택 트레이스 없음
                    userId,
                    requestUri,
                    "POST",
                    null,
                    now,
                    LogLevel.WARN)); // 렌더링 실패는 서버 장애가 아니므로 WARN
        } catch (Exception e) {
            log.warn("렌더링 오류 이벤트 발행 실패 — 상태 업데이트는 계속 진행됩니다", e);
        }

        // FWK_REACT_CODE_HIS: 해당 코드 레코드를 FAILED로 업데이트
        if (codeId != null && !codeId.isBlank()) {
            log.warn("렌더링 오류로 코드 실패 처리 — codeId: {}, error: {}", codeId, errorMessage);
            reactGenerateMapper.updateToFailed(codeId, errorMessage);
        }
    }

    /**
     * 검색 조건에 맞는 이력 목록과 전체 건수를 조회한다.
     *
     * @param req 검색 조건 (상태·생성자·날짜 범위·페이지)
     * @return list(목록), totalCount(전체 건수), page, size
     */
    public Map<String, Object> getHistory(ReactGenerateSearchRequest req) {
        List<ReactGenerateHistoryResponse> list = reactGenerateMapper.selectList(req);
        int totalCount = reactGenerateMapper.selectCount(req);
        return Map.of("list", list, "totalCount", totalCount, "page", req.getPage(), "size", req.getSize());
    }

    /**
     * CODE_ID로 생성 이력 상세를 조회한다.
     *
     * @param codeId 조회할 코드 ID
     * @return 코드·메타 정보 (reactCode, approvalUserId 등 포함)
     * @throws NotFoundException 해당 codeId 레코드가 없을 때
     */
    public ReactGenerateResponse getById(String codeId) {
        ReactGenerateResponse response = reactGenerateMapper.selectById(codeId);
        if (response == null) {
            throw new NotFoundException("이력을 찾을 수 없습니다. codeId=" + codeId);
        }
        return response;
    }

    /**
     * 검색 조건에 맞는 전체 이력을 엑셀 파일로 변환하여 반환한다.
     *
     * @param req 검색 조건 (페이지네이션 무시, 전체 조회)
     * @return xlsx 파일의 byte 배열
     * @throws InvalidInputException 결과 건수가 최대 행 수를 초과할 때
     */
    public byte[] exportHistory(ReactGenerateSearchRequest req) {
        // 전체 데이터 로드 전 건수 먼저 확인 → 초과 시 즉시 예외로 OOM 방지
        int totalCount = reactGenerateMapper.selectCount(req);
        if (!ExcelExportUtil.isWithinLimit(totalCount)) {
            throw new InvalidInputException(
                    "엑셀 다운로드 최대 행 수(" + ExcelExportUtil.MAX_ROW_LIMIT + ")를 초과했습니다: " + totalCount);
        }
        List<ReactGenerateHistoryResponse> data = reactGenerateMapper.selectAllForExport(req);

        List<ExcelColumnDefinition> columns = List.of(
                new ExcelColumnDefinition("화면 제목", 30, "title"),
                new ExcelColumnDefinition("브랜드", 12, "brand"),
                new ExcelColumnDefinition("도메인", 12, "domain"),
                new ExcelColumnDefinition("분류", 12, "category"),
                new ExcelColumnDefinition("컴포넌트명", 25, "componentName"),
                new ExcelColumnDefinition("Figma URL", 50, "figmaUrl"),
                new ExcelColumnDefinition("상태", 15, "status"),
                new ExcelColumnDefinition("생성자", 15, "createUserId"),
                new ExcelColumnDefinition("생성일시", 20, "createDtime"),
                new ExcelColumnDefinition("승인자", 15, "approvalUserId"),
                new ExcelColumnDefinition("승인일시", 20, "approvalDtime"));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (ReactGenerateHistoryResponse item : data) {
            Map<String, Object> row = new HashMap<>();
            row.put("title", item.getTitle());
            row.put("brand", item.getBrand());
            row.put("domain", item.getDomain());
            row.put("category", item.getCategory());
            row.put("componentName", item.getComponentName());
            row.put("figmaUrl", item.getFigmaUrl());
            row.put("status", translateStatus(item.getStatus()));
            row.put("createUserId", item.getCreateUserId());
            row.put("createDtime", formatDtimeForExcel(item.getCreateDtime()));
            row.put("approvalUserId", item.getApprovalUserId());
            row.put("approvalDtime", formatDtimeForExcel(item.getApprovalDtime()));
            rows.add(row);
        }

        try {
            return ExcelExportUtil.createWorkbook("React 코드 생성 이력", columns, rows);
        } catch (IOException e) {
            throw new InternalException("엑셀 파일 생성 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 생성·재생성 실행 결과를 조합해 INSERT용 엔티티를 생성한다.
     *
     * @param id                  생성된 CODE_ID
     * @param figmaUrl            Figma URL
     * @param brand               브랜드
     * @param effectiveDomain     null 대체 처리 후 확정된 도메인
     * @param effectiveComponentName Figma에서 확정된 컴포넌트명
     * @param categoryStr         카테고리 enum name (null 가능)
     * @param title               화면 제목
     * @param description         화면 설명 (null 가능)
     * @param requirements        추가 요구사항 / 변경 요청사항 (null 가능)
     * @param figmaJson           Figma API 원시 응답 JSON (실패 시 null 가능)
     * @param systemPrompt        Claude에게 전달한 시스템 프롬프트 (실패 시 null 가능)
     * @param userPrompt          Claude에게 전달한 유저 프롬프트 (실패 시 null 가능)
     * @param reactCode           생성된 React 코드 (실패 시 null 가능)
     * @param failReason          실패 사유 (성공 시 null)
     * @param status              저장할 상태 (GENERATED / FAILED)
     * @param createdBy           생성 요청자 ID
     * @param now                 생성 일시 (yyyyMMddHHmmss)
     * @param refCodeId           재생성 직계 부모 CODE_ID (최초 생성 시 null)
     * @param rootCodeId          재생성 체인 최상위 CODE_ID (최초 생성 시 null)
     * @return INSERT 전용 엔티티
     */
    private ReactGenerateEntity buildEntity(
            String id,
            String figmaUrl,
            BrandType brand,
            DomainType effectiveDomain,
            String effectiveComponentName,
            String categoryStr,
            String title,
            String description,
            String requirements,
            String figmaJson,
            String systemPrompt,
            String userPrompt,
            String reactCode,
            String failReason,
            String status,
            String createdBy,
            String now,
            String refCodeId,
            String rootCodeId) {

        return ReactGenerateEntity.builder()
                .codeId(id)
                .figmaUrl(figmaUrl)
                .figmaJson(figmaJson)
                .brand(brand != null ? brand.name() : null)
                .domain(effectiveDomain.name())
                .componentName(effectiveComponentName)
                .title(title)
                .category(categoryStr)
                .description(description)
                .requirements(requirements)
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .reactCode(reactCode)
                .failReason(failReason)
                .status(status)
                .createUserId(createdBy)
                .createDtime(now)
                .refCodeId(refCodeId)
                .rootCodeId(rootCodeId)
                .build();
    }

    /**
     * 객체를 JSON 문자열로 직렬화한다.
     * 직렬화 실패 시 null을 반환하여 JSON 저장 실패가 코드 생성 전체를 중단하지 않도록 한다.
     */
    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Figma 응답 JSON 직렬화 실패 — FIGMA_JSON 컬럼 null로 저장됩니다", e);
            return null;
        }
    }

    /** DB 저장 형식(yyyyMMddHHmmss)을 사람이 읽기 쉬운 형식(yyyy-MM-dd HH:mm)으로 변환한다. */
    private String formatDtimeForExcel(String dtime) {
        if (dtime == null) return "";
        if (dtime.length() != 14) return dtime;
        try {
            return LocalDateTime.parse(dtime, FORMATTER).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            log.warn("날짜 형식 변환 실패 — dtime: {}", dtime);
            return dtime;
        }
    }

    /** 영문 상태 코드를 한글 레이블로 변환한다. */
    private String translateStatus(String status) {
        if (status == null) return "";
        return switch (status) {
            case "GENERATED" -> "생성 완료";
            case "PENDING_APPROVAL" -> "승인 대기";
            case "APPROVED" -> "승인 완료";
            case "FAILED" -> "실패";
            default -> status;
        };
    }

}