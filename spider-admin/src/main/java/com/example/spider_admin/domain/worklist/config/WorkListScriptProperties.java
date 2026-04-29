package com.example.spider_admin.domain.worklist.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 이행스크립트 생성 설정.
 *
 * <p>WORK_ID별 대상 테이블·PK·자식 테이블 구성을 application.yml에서 관리한다.
 *
 * <pre>{@code
 * worklist.script.configs:
 *   Message:
 *     table-name: FWK_MESSAGE
 *     pk-columns: [ORG_ID, MESSAGE_ID]
 *     child-tables:
 *       - table-name: FWK_MESSAGE_FIELD
 *         fk-columns: [ORG_ID, MESSAGE_ID]
 * }</pre>
 */
@Component
@ConfigurationProperties(prefix = "worklist.script")
@Getter
@Setter
public class WorkListScriptProperties {

    /** 이행스크립트 파일 저장 경로. */
    private String path = "${user.dir}/worklist-scripts";

    /** WORK_ID → 테이블 설정 맵. */
    private Map<String, ScriptConfigProperties> configs = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class ScriptConfigProperties {
        private String tableName;
        private List<String> pkColumns = new ArrayList<>();
        private List<ChildTableConfigProperties> childTables = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class ChildTableConfigProperties {
        /** 자식 테이블명. */
        private String tableName;
        /** 부모 PK 컬럼과 순서가 일치하는 FK 컬럼 목록. */
        private List<String> fkColumns = new ArrayList<>();
    }
}
