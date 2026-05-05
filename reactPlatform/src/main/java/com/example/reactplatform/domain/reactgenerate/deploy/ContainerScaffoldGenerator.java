/**
 * @file ContainerScaffoldGenerator.java
 * @description 비즈니스 로직 주입용 Container scaffold 코드를 생성한다.
 *
 * <p>생성된 컴포넌트는 모든 동작을 props로 받는 순수 UI 구조이므로,
 * Container는 상태·API 호출·이벤트 핸들러를 구현하고 해당 props에 연결하는 역할을 한다.
 *
 * <p>컴포넌트명은 DB에 저장된 값을 직접 전달받으며, 생성 코드를 파싱하여 추출하지 않는다.
 *
 * @example
 * componentName={@code LoginPage} 를 전달하면 {@code LoginPageContainer.tsx} scaffold를 생성한다.
 */
package com.example.reactplatform.domain.reactgenerate.deploy;

import org.springframework.stereotype.Component;

@Component
public class ContainerScaffoldGenerator {

    /**
     * Container scaffold 코드를 생성한다.
     *
     * @param componentName UI 컴포넌트 함수명 (DB 저장값 — 예: {@code LoginPage})
     * @param reactCode     AI가 생성한 TSX 코드 (scaffold 본문의 주석 및 props 연결 힌트용)
     * @param importPrefix  UI 컴포넌트의 import 기준 경로 (예: {@code ../generated} 또는 {@code ./generated})
     * @return Container scaffold TSX 코드 문자열
     */
    public String generate(String componentName, String reactCode, String importPrefix) {
        String containerName = componentName + "Container";

        return String.format(
                """
                /**
                 * @file %s.tsx
                 * @description %s의 비즈니스 로직 컨테이너.
                 *   UI 컴포넌트(%s/%s.tsx)에 상태·API 호출·이벤트 핸들러를 주입한다.
                 * @returns {JSX.Element}
                 */
                import %s from '%s/%s';

                export default function %s() {
                  // TODO: 상태 관리 (useState, useReducer 등)

                  // TODO: API 호출 (useEffect, react-query 등)

                  // TODO: 이벤트 핸들러 구현

                  return (
                    <%s
                      // TODO: props 연결
                    />
                  );
                }
                """,
                containerName,
                componentName,
                importPrefix,
                componentName,
                componentName,
                importPrefix,
                componentName,
                containerName,
                componentName);
    }

    /**
     * Container scaffold 파일명을 반환한다.
     *
     * @param componentName UI 컴포넌트 함수명 (DB 저장값)
     * @return 예: {@code LoginPageContainer.tsx}
     */
    public String resolveFileName(String componentName) {
        return componentName + "Container.tsx";
    }
}
