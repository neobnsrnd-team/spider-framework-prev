package com.example.spideradmin.global.aop;

import com.example.spideradmin.domain.worklist.mapper.WorkListMapper;
import com.example.spideradmin.global.util.AuditUtil;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

/**
 * {@link WorkListRecord} 애노테이션이 선언된 서비스 메서드 성공 후
 * FWK_WORK_LIST에 변경 이력을 자동 적재하는 AOP Aspect.
 *
 * <p>적재 조건: 메서드가 예외 없이 정상 반환된 경우에만 실행.
 * 로그인 사용자 ID가 없으면(Anonymous) 기록을 생략한다.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkListAspect {

    private final WorkListMapper workListMapper;

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    @AfterReturning("@annotation(record)")
    public void afterReturning(JoinPoint jp, WorkListRecord record) {
        String userId = AuditUtil.currentUserId();
        if (userId == null || userId.isBlank()) {
            return;
        }

        try {
            String workId = resolveWorkId(jp, record);
            String pk = evaluateExpression(jp, record.pkExpression());

            Map<String, Object> params = new HashMap<>();
            params.put("workId", workId);
            params.put("workDataPk", pk);
            params.put("workName", record.workName());
            params.put("crudType", record.crudType());
            params.put("userId", userId);

            workListMapper.upsertHistory(params);
        } catch (Exception e) {
            // 이력 적재 실패는 원래 비즈니스 로직에 영향을 주지 않는다.
            log.warn(
                    "WorkList 이력 적재 실패 — method={}, error={}", jp.getSignature().toShortString(), e.getMessage());
        }
    }

    private String resolveWorkId(JoinPoint jp, WorkListRecord record) {
        if (!record.workIdExpression().isBlank()) {
            return evaluateExpression(jp, record.workIdExpression());
        }
        return record.workId();
    }

    private String evaluateExpression(JoinPoint jp, String expression) {
        MethodSignature sig = (MethodSignature) jp.getSignature();
        MethodBasedEvaluationContext ctx =
                new MethodBasedEvaluationContext(jp.getTarget(), sig.getMethod(), jp.getArgs(), NAME_DISCOVERER);
        Object result = PARSER.parseExpression(expression).getValue(ctx);
        return result != null ? result.toString() : "";
    }
}
