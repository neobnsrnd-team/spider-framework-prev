package com.example.spideradmin;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

@AnalyzeClasses(packages = "com.example.spideradmin", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    // ── 1. 레이어 의존성: Controller → Service → Mapper 단방향 ──

    @ArchTest
    static final ArchRule controllers_should_not_depend_on_mappers = noClasses()
            .that()
            .resideInAnyPackage("..controller..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..mapper..")
            .as("Controller는 Mapper에 직접 의존하면 안 됩니다 (Service를 거쳐야 합니다)");

    @ArchTest
    static final ArchRule mappers_should_not_depend_on_services = noClasses()
            .that()
            .resideInAnyPackage("..mapper..")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Service")
            .as("Mapper는 Service에 의존하면 안 됩니다");

    @ArchTest
    static final ArchRule services_should_not_depend_on_controllers = noClasses()
            .that()
            .resideInAnyPackage("..service..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..controller..")
            .as("Service는 Controller에 의존하면 안 됩니다");

    // ── 2. DTO 네이밍 규칙: *Request 또는 *Response만 허용 ──

    private static final String[] ALLOWED_DTO_SUFFIXES = {"Request", "Response"};

    // 영구 면제 — 프레임워크/인프라 클래스
    private static final Set<String> EXEMPT =
            Set.of("ApiResponse", "PageRequest", "PageResponse", "AuthenticatedUser", "MenuPermission", "UserAuthInfo");

    @ArchTest
    static final ArchRule all_dtos_should_follow_naming = classes()
            .that()
            .resideInAnyPackage("..dto..")
            .should(beTopLevelClassWithAllowedSuffixOrExempt(EXEMPT, ALLOWED_DTO_SUFFIXES))
            .as("모든 dto 패키지의 클래스는 *Request 또는 *Response로 끝나야 합니다");

    // ── 4. ServiceImpl 금지 ──

    @ArchTest
    static final ArchRule no_service_impl = noClasses()
            .that()
            .resideInAnyPackage("..service..")
            .should()
            .haveSimpleNameEndingWith("ServiceImpl")
            .as("ServiceImpl 패턴은 금지입니다 (구체 Service 클래스를 사용하세요)");

    // ── 5. Entity 금지 ──

    @ArchTest
    static final ArchRule no_entity_classes = noClasses()
            .that()
            .resideInAnyPackage("..domain..")
            .should()
            .haveSimpleNameEndingWith("Entity")
            .as("Entity 클래스는 금지입니다 (DTO를 직접 사용하세요)");

    // ── 6. Converter 금지 ──

    @ArchTest
    static final ArchRule no_converter_classes = noClasses()
            .that()
            .resideInAnyPackage("..domain..")
            .should()
            .haveSimpleNameEndingWith("Converter")
            .as("Converter 클래스는 금지입니다 (DTO ↔ Mapper 직접 통신)");

    // ── 7. VO / Repository 금지 ──

    @ArchTest
    static final ArchRule no_vo_classes = noClasses()
            .that()
            .resideInAnyPackage("..domain..")
            .should()
            .haveSimpleNameEndingWith("VO")
            .as("VO 클래스는 금지입니다");

    @ArchTest
    static final ArchRule no_repository_classes = noClasses()
            .that()
            .resideInAnyPackage("..domain..")
            .should()
            .haveSimpleNameEndingWith("Repository")
            .as("Repository 클래스는 금지입니다 (MyBatis Mapper를 사용하세요)");

    // ── 8. DTO record 금지 (Lombok class 통일) ──

    @ArchTest
    static final ArchRule no_record_dtos = noClasses()
            .that()
            .resideInAnyPackage("..dto..", "..event..")
            .should(beRecord())
            .as("DTO/Event 클래스에 record 사용은 금지입니다 (Lombok class를 사용하세요)");

    // ── 9. @Autowired 필드 주입 금지 ──

    @ArchTest
    static final ArchRule no_field_injection =
            noFields().should().beAnnotatedWith(Autowired.class).as("@Autowired 필드 주입은 금지입니다 (생성자 주입을 사용하세요)");

    // ── Helper: record 여부 확인 ──

    private static ArchCondition<JavaClass> beRecord() {
        return new ArchCondition<>("be a record class") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                if (javaClass.isRecord()) {
                    events.add(SimpleConditionEvent.violated(
                            javaClass,
                            String.format("Class <%s> is a record (Lombok class를 사용하세요)", javaClass.getFullName())));
                }
            }
        };
    }

    // ── Helper: inner class(Builder 등)는 무시하고 top-level만 네이밍 검증 ──

    private static ArchCondition<JavaClass> beTopLevelClassWithAllowedSuffixOrExempt(
            Set<String> exempt, String... suffixes) {
        String description =
                "be a top-level class ending with one of: " + String.join(", ", suffixes) + " (or be exempt)";
        return new ArchCondition<>(description) {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                if (javaClass.getName().contains("$")) {
                    return;
                }
                String simpleName = javaClass.getSimpleName();
                if (exempt.contains(simpleName)) {
                    return;
                }
                for (String suffix : suffixes) {
                    if (simpleName.endsWith(suffix)) {
                        return;
                    }
                }
                events.add(SimpleConditionEvent.violated(
                        javaClass,
                        String.format(
                                "Class <%s> does not end with any of [%s] and is not exempt",
                                javaClass.getFullName(), String.join(", ", suffixes))));
            }
        };
    }
}
