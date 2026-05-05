package com.example.reactplatform.domain.reactgenerate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.reactplatform.domain.reactgenerate.ai.client.ClaudeApiClient;
import com.example.reactplatform.domain.reactgenerate.ai.prompt.PromptBuilder;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateApprovalResponse;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateEntity;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateRequest;
import com.example.reactplatform.domain.reactgenerate.dto.ReactGenerateResponse;
import com.example.reactplatform.domain.reactgenerate.enums.BrandType;
import com.example.reactplatform.domain.reactgenerate.enums.DomainType;
import com.example.reactplatform.domain.reactgenerate.enums.ReactGenerateStatus;
import com.example.reactplatform.domain.reactgenerate.figma.FigmaDesignContext;
import com.example.reactplatform.domain.reactgenerate.figma.FigmaDesignExtractor;
import com.example.reactplatform.domain.reactgenerate.figma.client.FigmaApiClient;
import com.example.reactplatform.domain.reactgenerate.figma.client.FigmaNodeResponse;
import com.example.reactplatform.domain.reactgenerate.mapper.ReactGenerateMapper;
import com.example.reactplatform.domain.reactgenerate.validator.CodeValidationResult;
import com.example.reactplatform.domain.reactgenerate.validator.CodeValidator;
import com.example.reactplatform.global.exception.InternalException;
import com.example.reactplatform.global.exception.InvalidInputException;
import com.example.reactplatform.global.exception.NotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @file ReactGenerateServiceTest.java
 * @description ReactGenerateService 단위 테스트.
 *     React 코드 생성 파이프라인 전체 흐름, 실패 단계별 DB 저장 동작,
 *     requestApproval 권한 검증을 모킹 환경에서 검증한다.
 * @see ReactGenerateService
 */
@ExtendWith(MockitoExtension.class)
class ReactGenerateServiceTest {

    @Mock
    private ReactGenerateMapper reactGenerateMapper;

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private ClaudeApiClient claudeApiClient;

    @Mock
    private FigmaApiClient figmaApiClient;

    @Mock
    private FigmaDesignExtractor figmaDesignExtractor;

    @Mock
    private CodeValidator codeValidator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ReactGenerateService service;

    /** FigmaUrlParser.parse()가 정상 동작하는 유효한 Figma URL */
    private static final String VALID_FIGMA_URL = "https://www.figma.com/design/ABC123/test-design?node-id=1-2";

    private static final String USER_ID = "user001";
    private static final String SYSTEM_PROMPT = "system prompt";
    private static final String USER_PROMPT = "user prompt";
    private static final String GENERATED_CODE = "export default function App() { return <div/>; }";

    // ========== generate — 성공 경로 ==========

    @Test
    @DisplayName("정상 요청 시 DB에 GENERATED 상태로 저장하고 응답을 반환한다")
    void generate_success_savesGeneratedRecordAndReturnsResponse() throws Exception {
        stubGeneratePipeline(passedValidation());

        ReactGenerateResponse response = service.generate(generateRequest(DomainType.BANKING), USER_ID);

        // GENERATED 상태로 insert 호출 여부 확인
        ArgumentCaptor<ReactGenerateEntity> entityCaptor = ArgumentCaptor.forClass(ReactGenerateEntity.class);
        verify(reactGenerateMapper).insert(entityCaptor.capture());
        ReactGenerateEntity saved = entityCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ReactGenerateStatus.GENERATED.name());
        assertThat(saved.getFigmaUrl()).isEqualTo(VALID_FIGMA_URL);
        assertThat(saved.getBrand()).isEqualTo("HANA");
        assertThat(saved.getDomain()).isEqualTo("BANKING");
        assertThat(saved.getComponentName()).isEqualTo("TestComponent");
        assertThat(saved.getTitle()).isEqualTo("테스트 제목");
        assertThat(saved.getSystemPrompt()).isEqualTo(SYSTEM_PROMPT);
        assertThat(saved.getUserPrompt()).isEqualTo(USER_PROMPT);
        assertThat(saved.getReactCode()).isEqualTo(GENERATED_CODE);
        assertThat(saved.getFailReason()).isNull();
        assertThat(saved.getCreateUserId()).isEqualTo(USER_ID);

        assertThat(response.getReactCode()).isEqualTo(GENERATED_CODE);
        assertThat(response.getStatus()).isEqualTo(ReactGenerateStatus.GENERATED.name());
        assertThat(response.getFigmaUrl()).isEqualTo(VALID_FIGMA_URL);
    }

    @Test
    @DisplayName("보안 경고만 있는 경우 통과하고 응답에 경고 목록이 포함된다")
    void generate_withWarnings_passesAndIncludesWarnings() throws Exception {
        CodeValidationResult withWarnings = CodeValidationResult.builder()
                .passed(true)
                .errors(List.of())
                .warnings(List.of("eval 사용 감지"))
                .build();
        stubGeneratePipeline(withWarnings);

        ReactGenerateResponse response = service.generate(generateRequest(DomainType.BANKING), USER_ID);

        assertThat(response.getValidationWarnings()).containsExactly("eval 사용 감지");
    }

    @Test
    @DisplayName("보안 경고가 없으면 응답의 validationWarnings는 null이다")
    void generate_noWarnings_validationWarningsIsNull() throws Exception {
        stubGeneratePipeline(passedValidation());

        ReactGenerateResponse response = service.generate(generateRequest(DomainType.BANKING), USER_ID);

        assertThat(response.getValidationWarnings()).isNull();
    }

    // ========== generate — 도메인 기본값 ==========

    @Test
    @DisplayName("domain이 null이면 BANKING이 기본값으로 적용된다")
    void generate_nullDomain_defaultsToBanking() throws Exception {
        stubGeneratePipeline(passedValidation());

        // domain null로 요청
        service.generate(generateRequest(null), USER_ID);

        // promptBuilder.buildUserPrompt에 effectiveDomain=BANKING이 전달되는지 확인
        verify(promptBuilder)
                .buildUserPrompt(
                        any(),
                        eq(BrandType.HANA),
                        eq(DomainType.BANKING),
                        eq("TestComponent"),
                        eq("테스트 제목"),
                        isNull(),
                        isNull(),
                        isNull());
    }

    // ========== generate — Figma 단계 실패 ==========

    @Test
    @DisplayName("Figma API 호출 실패 시 systemPrompt·userPrompt·reactCode 모두 null로 FAILED 저장 후 예외 전파")
    void generate_figmaApiThrows_savesFailedRecordWithNullPrompts() throws Exception {
        when(figmaApiClient.getNode(anyString(), anyString())).thenThrow(new InternalException("Figma 연결 실패"));

        assertThatThrownBy(() -> service.generate(generateRequest(DomainType.BANKING), USER_ID))
                .isInstanceOf(InternalException.class);

        ArgumentCaptor<ReactGenerateEntity> entityCaptor = ArgumentCaptor.forClass(ReactGenerateEntity.class);
        verify(reactGenerateMapper).insert(entityCaptor.capture());
        ReactGenerateEntity saved = entityCaptor.getValue();
        assertThat(saved.getFigmaUrl()).isEqualTo(VALID_FIGMA_URL);
        assertThat(saved.getFigmaJson()).isNull(); // Figma 호출 실패로 미설정
        assertThat(saved.getBrand()).isEqualTo("HANA");
        assertThat(saved.getDomain()).isEqualTo("BANKING");
        assertThat(saved.getComponentName()).isEqualTo("Unknown"); // Figma 도달 전 폴백값
        assertThat(saved.getTitle()).isEqualTo("테스트 제목");
        assertThat(saved.getSystemPrompt()).isNull(); // 미설정
        assertThat(saved.getUserPrompt()).isNull(); // 미설정
        assertThat(saved.getReactCode()).isNull(); // 미설정
        assertThat(saved.getFailReason()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ReactGenerateStatus.FAILED.name());
        assertThat(saved.getCreateUserId()).isEqualTo(USER_ID);
    }

    // ========== generate — Claude 단계 실패 ==========

    @Test
    @DisplayName("Claude API 실패 시 systemPrompt·userPrompt는 기록되고 reactCode는 null로 FAILED 저장")
    void generate_claudeApiThrows_savesFailedRecordWithPromptsButNullCode() throws Exception {
        when(figmaApiClient.getNode(anyString(), anyString())).thenReturn(new FigmaNodeResponse());
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"nodes\":{}}");
        when(figmaDesignExtractor.extract(any(), anyString(), anyString())).thenReturn(minimalDesignContext());
        when(promptBuilder.buildSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(promptBuilder.buildUserPrompt(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(USER_PROMPT);
        when(claudeApiClient.generate(anyString(), anyString())).thenThrow(new InternalException("Claude 타임아웃"));

        assertThatThrownBy(() -> service.generate(generateRequest(DomainType.BANKING), USER_ID))
                .isInstanceOf(InternalException.class);

        ArgumentCaptor<ReactGenerateEntity> entityCaptor = ArgumentCaptor.forClass(ReactGenerateEntity.class);
        verify(reactGenerateMapper).insert(entityCaptor.capture());
        ReactGenerateEntity saved = entityCaptor.getValue();
        assertThat(saved.getFigmaUrl()).isEqualTo(VALID_FIGMA_URL);
        assertThat(saved.getFigmaJson()).isNotNull(); // Figma 성공 후 직렬화됨
        assertThat(saved.getBrand()).isEqualTo("HANA");
        assertThat(saved.getDomain()).isEqualTo("BANKING");
        assertThat(saved.getComponentName()).isEqualTo("TestComponent");
        assertThat(saved.getTitle()).isEqualTo("테스트 제목");
        assertThat(saved.getSystemPrompt()).isEqualTo(SYSTEM_PROMPT); // 이미 설정됨
        assertThat(saved.getUserPrompt()).isEqualTo(USER_PROMPT); // 이미 설정됨
        assertThat(saved.getReactCode()).isNull(); // Claude 실패로 미설정
        assertThat(saved.getFailReason()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ReactGenerateStatus.FAILED.name());
        assertThat(saved.getCreateUserId()).isEqualTo(USER_ID);
    }

    // ========== generate — 코드 검증 실패 ==========

    @Test
    @DisplayName("보안 검증 실패 시 InvalidInputException이 전파되고 FAILED 상태로 저장된다")
    void generate_codeValidationFails_throwsAndSavesFailedRecord() throws Exception {
        CodeValidationResult failed = CodeValidationResult.builder()
                .passed(false)
                .errors(List.of("eval() 사용 금지"))
                .warnings(List.of())
                .build();
        stubGeneratePipeline(failed);

        assertThatThrownBy(() -> service.generate(generateRequest(DomainType.BANKING), USER_ID))
                .isInstanceOf(InvalidInputException.class);

        // 검증 실패 시 reactCode는 Claude가 반환한 코드가 포함된 채로 FAILED 저장
        ArgumentCaptor<ReactGenerateEntity> entityCaptor = ArgumentCaptor.forClass(ReactGenerateEntity.class);
        verify(reactGenerateMapper).insert(entityCaptor.capture());
        ReactGenerateEntity saved = entityCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ReactGenerateStatus.FAILED.name());
        assertThat(saved.getCreateUserId()).isEqualTo(USER_ID);
    }

    // ========== requestApproval ==========

    @Test
    @DisplayName("코드 작성자가 승인 요청하면 PENDING_APPROVAL 상태로 변경된다")
    void requestApproval_byOwner_updatesStatusToPendingApproval() {
        ReactGenerateResponse existing = ReactGenerateResponse.builder()
                .codeId("code-id-1")
                .createUserId(USER_ID)
                .build();
        when(reactGenerateMapper.selectById("code-id-1")).thenReturn(existing);

        ReactGenerateApprovalResponse response = service.requestApproval("code-id-1", USER_ID);

        verify(reactGenerateMapper)
                .updateStatus(
                        eq("code-id-1"), eq(ReactGenerateStatus.PENDING_APPROVAL.name()), eq(null), eq(null), eq(null));
        assertThat(response.getStatus()).isEqualTo(ReactGenerateStatus.PENDING_APPROVAL.name());
        assertThat(response.getCodeId()).isEqualTo("code-id-1");
    }

    @Test
    @DisplayName("코드 작성자가 아닌 사용자가 승인 요청하면 InvalidInputException이 발생한다")
    void requestApproval_byNonOwner_throwsInvalidInputException() {
        ReactGenerateResponse existing = ReactGenerateResponse.builder()
                .codeId("code-id-1")
                .createUserId("owner-user")
                .build();
        when(reactGenerateMapper.selectById("code-id-1")).thenReturn(existing);

        assertThatThrownBy(() -> service.requestApproval("code-id-1", "another-user"))
                .isInstanceOf(InvalidInputException.class);

        verify(reactGenerateMapper, never()).updateStatus(anyString(), anyString(), any(), any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 codeId로 승인 요청하면 NotFoundException이 발생한다")
    void requestApproval_nonExistentCode_throwsNotFoundException() {
        when(reactGenerateMapper.selectById("unknown-id")).thenReturn(null);

        assertThatThrownBy(() -> service.requestApproval("unknown-id", USER_ID)).isInstanceOf(NotFoundException.class);
    }

    // ========== getById ==========

    @Test
    @DisplayName("존재하는 codeId로 조회하면 해당 이력을 반환한다")
    void getById_existingCode_returnsResponse() {
        ReactGenerateResponse expected = ReactGenerateResponse.builder()
                .codeId("code-id-1")
                .reactCode(GENERATED_CODE)
                .build();
        when(reactGenerateMapper.selectById("code-id-1")).thenReturn(expected);

        ReactGenerateResponse result = service.getById("code-id-1");

        assertThat(result.getCodeId()).isEqualTo("code-id-1");
        assertThat(result.getReactCode()).isEqualTo(GENERATED_CODE);
    }

    @Test
    @DisplayName("존재하지 않는 codeId로 조회하면 NotFoundException이 발생한다")
    void getById_nonExistentCode_throwsNotFoundException() {
        when(reactGenerateMapper.selectById("unknown-id")).thenReturn(null);

        assertThatThrownBy(() -> service.getById("unknown-id")).isInstanceOf(NotFoundException.class);
    }

    // ========== helpers ==========

    /** 성공 파이프라인 전체를 스텁한다. */
    private void stubGeneratePipeline(CodeValidationResult validationResult) throws Exception {
        when(figmaApiClient.getNode(anyString(), anyString())).thenReturn(new FigmaNodeResponse());
        // ObjectMapper 직렬화: Figma 응답 → JSON 문자열 (임의 문자열 반환)
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"nodes\":{}}");
        when(figmaDesignExtractor.extract(any(), anyString(), anyString())).thenReturn(minimalDesignContext());
        when(promptBuilder.buildSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(promptBuilder.buildUserPrompt(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(USER_PROMPT);
        when(claudeApiClient.generate(anyString(), anyString())).thenReturn(GENERATED_CODE);
        when(codeValidator.validate(anyString())).thenReturn(validationResult);
    }

    /** 검증 통과, 경고 없음 CodeValidationResult 헬퍼 */
    private CodeValidationResult passedValidation() {
        return CodeValidationResult.builder()
                .passed(true)
                .errors(List.of())
                .warnings(List.of())
                .build();
    }

    /** 최소 필드만 채운 ReactGenerateRequest 생성 헬퍼 (title 필수) */
    private ReactGenerateRequest generateRequest(DomainType domain) {
        return ReactGenerateRequest.builder()
                .title("테스트 제목")
                .figmaUrl(VALID_FIGMA_URL)
                .brand(BrandType.HANA)
                .domain(domain)
                .build();
    }

    /** 최소 필드만 채운 FigmaDesignContext 생성 헬퍼 */
    private FigmaDesignContext minimalDesignContext() {
        return FigmaDesignContext.builder()
                .figmaUrl(VALID_FIGMA_URL)
                .componentName("TestComponent")
                .componentType("FRAME")
                .width(360)
                .height(240)
                .children(List.of())
                .build();
    }
}
