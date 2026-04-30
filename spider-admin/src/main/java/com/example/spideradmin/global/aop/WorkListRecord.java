package com.example.spideradmin.global.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 도메인 서비스 CRUD 메서드에 선언하면 {@link WorkListAspect}가 FWK_WORK_LIST에 변경 이력을 자동 적재한다.
 *
 * <p>적재 조건: 메서드가 예외 없이 정상 반환된 경우에만 실행 (AfterReturning).
 *
 * <pre>{@code
 * // 단순 PK
 * @WorkListRecord(workId = "SQL_QUERY", crudType = "C", pkExpression = "#dto.queryId", workName = "SQL쿼리관리")
 * public SqlQueryResponse create(SqlQueryCreateRequest dto) { ... }
 *
 * // 복합 PK (@ 구분자)
 * @WorkListRecord(workId = "Message", crudType = "C", pkExpression = "#requestDTO.orgId + '@' + #requestDTO.messageId", workName = "전문")
 * public MessageResponse createMessage(MessageCreateRequest requestDTO) { ... }
 *
 * // workId를 SpEL로 동적 결정
 * @WorkListRecord(workIdExpression = "#dto.serviceType", crudType = "C", pkExpression = "#dto.serviceId", workName = "서비스")
 * public FwkServiceDetailResponse create(FwkServiceCreateRequest dto) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WorkListRecord {

    /**
     * 고정 WORK_ID 값 (예: "SQL_QUERY", "Message").
     * {@link #workIdExpression()}이 비어 있지 않으면 이 값은 무시된다.
     */
    String workId() default "";

    /**
     * WORK_ID를 동적으로 결정하는 SpEL 표현식.
     * 비어 있으면 {@link #workId()}를 사용한다.
     * 예: {@code "#dto.serviceType"}
     */
    String workIdExpression() default "";

    /** CRUD 유형: "C"(생성), "U"(수정), "D"(삭제) */
    String crudType();

    /**
     * 항목 식별자(PK)를 추출하는 SpEL 표현식.
     * 메서드 파라미터는 파라미터명으로 참조한다 (예: {@code "#dto.queryId"}, {@code "#queryId"}).
     * 복합 PK는 '@' 구분자로 연결한다 (예: {@code "#orgId + '@' + #messageId"}).
     */
    String pkExpression();

    /** WORK_NAME 컬럼에 저장될 항목 레이블 (예: "SQL쿼리관리", "전문") */
    String workName();
}
