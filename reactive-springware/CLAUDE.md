# Reactive-Springware Claude Code 지침

## 컴포넌트 신규 추가 / 수정 시

`component-library/`에 새 컴포넌트를 추가하거나 기존 컴포넌트를 수정할 때는 반드시 아래 파일을 먼저 읽고 체크리스트를 따른다.

```
docs/component-checklist.md
```

---

## 코드 작성 규칙

### 주석

**파일 상단 JSDoc**

새 파일을 생성할 때는 반드시 파일 상단에 JSDoc 주석을 작성한다.

- `@file` — 파일명
- `@description` — 역할 설명
- `@param` / `@returns` — 주요 파라미터 및 반환값
- `@example` — 필요한 경우 사용 예시

**인라인 주석**

다음 경우에는 반드시 인라인 주석을 추가한다.

- 왜 이 값을 선택했는지 이유가 필요한 상수·기본값
- 외부 개발자가 처음 봤을 때 의도를 오해할 수 있는 로직
- 타입 단언(`as`)이나 예외 처리 등 방어 코드
- 여러 분기 중 특정 분기가 존재하는 이유가 명확하지 않은 경우

---

## 디자인 토큰 규칙

### globals.css는 자동 생성 파일이다

`design-tokens/globals.css`는 직접 수정하지 않는다.
Figma Variables → Token Studio export → 변환 과정을 통해서만 업데이트된다.

### temp.json을 받으면 반드시 아래 절차를 수행한다

```
1. temp.json 파싱
   └─ 최상위 키(primitives / semantic / brand.hana 등 / domain.*)를 기준으로 분류

2. figma-tokens/*.json 업데이트 (카테고리별 분배)
   ├─ primitives.json  — spacing, radius, text, font, shadow, transition, breakpoint, nav, z
   ├─ semantic.json    — color.* (brand·domain 참조 포함)
   ├─ brand.{키}.json  — brand.* 토큰 (하나은행·신한은행 등 브랜드별)
   └─ domain.{키}.json — domain.* 토큰 (banking·card·giro·insurance)

3. figma-tokens/*.json → globals.css 변환
   ├─ [data-brand="hana"] 등 브랜드 블록 — brand.*.json에서 생성
   ├─ [data-domain="card"] 등 도메인 블록 — domain.*.json에서 생성
   ├─ @theme 블록 — semantic.json에서 생성
   └─ @layer base 블록 — 전역 기본 스타일 유지

4. temp.json 삭제 안내
   └─ "temp.json은 삭제해도 됩니다" 메시지 출력
```
