# Reactive-Springware Claude Code 지침_dev version

## 코드 작성 규칙

### 주석

이 프로젝트의 실사용자는 프로젝트 내부 구조를 전혀 모르는 외부 개발자다.
코드만 봐도 의도를 파악할 수 있도록 주석을 충분히 작성한다.

**파일 상단 JSDoc**

- 새 파일을 생성할 때는 반드시 파일 상단에 JSDoc 주석을 작성한다.
- 주석에는 다음 내용을 포함한다:
  - 파일명 (`@file`)
  - 파일/함수의 역할 설명 (`@description`)
  - 주요 파라미터 (`@param`)
  - 반환값 (`@returns`)
  - 필요한 경우 사용 예시 (`@example`)

**인라인 주석**

- 다음 경우에는 반드시 인라인 주석을 추가한다:
  - 왜 이 값을 선택했는지 이유가 필요한 상수·기본값 (e.g. 기본 listStyle이 'card'인 이유)
  - 외부 개발자가 처음 봤을 때 의도를 오해할 수 있는 로직
  - 타입 단언(`as`)이나 예외 처리 등 방어 코드
  - 여러 분기 중 특정 분기가 존재하는 이유가 명확하지 않은 경우

---

# 🧪 Development Phase Rules

This project is currently in **development phase**.

During development phase, the following rules MUST be followed:

### Component Usage Rules

- Pages MUST be generated using components from `component-library/`
- Direct HTML elements are NOT allowed
- New UI must use existing component-library components

If additional components are required:

- Create reusable common components
- Add them to `component-library/`
- Generate Storybook file together

#### Mandatory When Creating New Components

When creating new components in `component-library/`:

Claude MUST also generate:

- Component file
- Types file (if needed)
- Storybook file

Example:

```
component-library/
  Button/
    index.tsx
    types.ts
    Button.stories.tsx
```

---

### File Generation Location Rules

All generated pages and files MUST be created under:

```
demo/react-demo-app/
```

Claude MUST NOT create files outside this directory.

---

### Folder Scope Restriction

Claude MUST only modify files inside:

```
figma-react-generator/
```

Rules:

- Only modify files inside `figma-react-generator/`
- Do NOT reference parent folders
- Do NOT modify upper-level directories
- Do NOT follow existing structure outside this folder

---

### Generation Scope Summary

Claude MUST:

✔ Follow rule files first
✔ Follow all rules inside rules/ folder
✔ Use component-library components only
✔ Create reusable components when needed
✔ Generate Storybook with new components
✔ Generate files under demo/react-demo-app
✔ Only modify figma-react-generator folder

Claude MUST NOT:

❌ Follow existing project structure blindly
❌ Ignore rules folder
❌ Modify parent folders
❌ Generate files outside defined directories
❌ Use raw HTML instead of component-library

---

### Final Rule

Rules defined in this document override:

- Existing project structure
- Existing folder layout
- Existing code patterns

Claude MUST strictly follow these rules during code generation.

---

# 🎯 목적

이 프로젝트는 Figma 디자인을 기반으로 React 코드를 자동 생성하는 플랫폼이다.
Claude가 생성하는 모든 코드는 **일관성**, **재사용성**, **유지보수성**을 최우선으로 한다.

---

# 🧠 기본 원칙

### 1. component-library가 유일한 UI 소스다

모든 UI는 반드시 `component-library`에서 가져온다.
직접 HTML 태그(`div`, `button` 등)를 사용하거나 외부 UI 라이브러리를 추가하지 않는다.
이 원칙을 지켜야 디자인 시스템과 코드가 항상 동기화된다.

### 2. 디자인 토큰이 유일한 스타일 소스다

색상·간격·폰트 크기는 반드시 `design-tokens`에서 가져온다.
임의 값(`#333`, `16px`)을 하드코딩하면 Figma 디자인 변경 시 코드를 일일이 수정해야 하는 기술 부채가 생긴다.

### 3. Figma 매핑 기준을 벗어나지 않는다

`docs/component-map.md`에 정의된 컴포넌트만 생성한다.
정의되지 않은 컴포넌트를 임의로 만들면 디자이너의 의도와 다른 UI가 생성된다.

### 4. 관심사를 명확히 분리한다

- **HTTP 호출·데이터 가공·모델 변환·에러 처리**는 `{entity}Repository.ts`에서만 한다.
- **데이터 패칭 로직**은 `use{Entity}.ts`에서만 한다.
- **컴포넌트**는 데이터를 표시하는 역할만 한다.

Repository 패턴을 사용하는 이유: API 응답 구조가 바뀌어도 Repository만 수정하면 되고, Hook과 Page는 변경하지 않아도 된다. 유지보수 비용이 크게 줄어든다.

### 5. 데이터 상태를 반드시 처리한다

모든 데이터 화면은 `loading`, `error`, `empty` 세 가지 상태를 빠짐없이 처리해야 한다.
처리하지 않으면 로딩 중 깨진 화면, 에러 시 빈 화면, 데이터 없을 때 의미 없는 빈 테이블이 사용자에게 노출된다.
반드시 이 순서로 처리한다: `isLoading` → `isError` → empty check → 정상 렌더링.

### 6. 타입 안전성을 유지한다

`any` 타입은 TypeScript를 사용하는 의미를 없앤다.
모든 props와 API 응답에는 명시적 타입을 정의한다.

---

# 🚫 하지 말아야 하는 이유

| 금지 항목                           | 하지 말아야 하는 이유                            |
| ----------------------------------- | ------------------------------------------------ |
| HTML 태그 직접 사용                 | 디자인 시스템 일관성 붕괴                        |
| Inline style                        | Figma 변경 시 코드 일괄 추적 불가                |
| 임의 색상·크기 하드코딩             | 테마 변경 시 전체 수정 필요                      |
| Hook·Component에서 직접 HTTP 호출   | Repository 계층 우회, API 변경 시 전체 수정 필요 |
| `any` 타입                          | 런타임 오류를 컴파일 타임에 잡지 못함            |
| 정의되지 않은 컴포넌트 생성         | Figma 디자인 의도 이탈                           |
| loading/error/empty 처리 생략       | 로딩 중·에러·빈 데이터 상황에서 UI 깨짐          |
| Page에서 `useState` 직접 사용       | 비즈니스 로직이 Page에 쌓여 재사용·테스트 불가   |
| Component에서 이벤트 로직 직접 처리 | 컴포넌트 재사용성 소멸, 라우팅·API 호출 분산     |

---

### 컴포넌트 조합 규칙

컴포넌트 계층은 Page → Layout → Section → Component 순서를 반드시 지킨다.
이 계층을 건너뛰거나 역방향으로 중첩하면 레이아웃 일관성이 무너진다.
특히 Component 내부에 Page를 다시 만드는 구조는 절대 허용하지 않는다.

### 네이밍 규칙

일관된 네이밍이 없으면 같은 역할의 파일이 다른 이름으로 생성되어 구조 파악이 어려워진다.

| 대상                | 규칙                     | 예시                           |
| ------------------- | ------------------------ | ------------------------------ |
| Page (목록)         | `{Entity}ListPage`       | `UserListPage`                 |
| Page (상세)         | `{Entity}DetailPage`     | `UserDetailPage`               |
| Page (등록)         | `{Entity}CreatePage`     | `UserCreatePage`               |
| Page (수정)         | `{Entity}EditPage`       | `UserEditPage`                 |
| Component (테이블)  | `{Entity}Table`          | `UserTable`                    |
| Component (폼)      | `{Entity}Form`           | `UserForm`                     |
| Component (모달)    | `{Entity}{Action}Modal`  | `UserDeleteModal`              |
| Hook (목록)         | `use{Entity}List`        | `useUserList`                  |
| Hook (폼)           | `use{Entity}Form`        | `useUserForm`                  |
| Repository          | camelCase + `Repository` | `userRepository.ts`            |
| Type                | camelCase + `Types`      | `userTypes.ts`                 |
| 이벤트 (Hook 정의)  | `handle{Action}`         | `handleDelete`, `handleSearch` |
| 이벤트 (props 전달) | `on{Action}`             | `onDelete`, `onSearch`         |

### Props 설계 규칙

Props는 최소한으로 설계한다. 불필요하게 많은 props는 컴포넌트의 역할이 불분명하다는 신호다.
`variant`, `size` 같은 열거형 props를 사용하고, boolean을 나열하는 방식은 피한다.
boolean props는 이름만으로 의미가 명확해야 한다. (`disabled`, `loading` 등)

### 상태관리 방향

상태의 출처(서버 vs 클라이언트)에 따라 관리 도구를 다르게 사용한다.
서버 데이터에 `useState`를 사용하면 캐싱·동기화·에러 처리를 직접 구현해야 해서 코드가 복잡해진다.
**Page에서 상태를 직접 생성하지 않는다.** 상태는 반드시 Hook을 통해 가져온다. Page에서 `useState`를 직접 쓰면 로직이 Page에 쌓이고 테스트와 재사용이 불가능해진다.
전역 상태는 정말 전역이 필요한 경우(인증, 테마)에만 사용한다. 과도한 전역 상태는 데이터 흐름을 추적하기 어렵게 만든다.

### 이벤트 처리 규칙

이벤트 핸들러는 **Hook에서 정의**하고, Page는 Hook에서 받은 핸들러를 Component에 전달만 한다. Component는 callback props로만 받는다.
Component 내부에서 직접 이벤트를 처리하면 같은 컴포넌트를 다른 동작으로 재사용할 수 없다.
핸들러 네이밍: Hook에서 정의할 때는 `handle~`, props로 전달할 때는 `on~` prefix를 사용한다.

### 컴포넌트 크기 규칙

컴포넌트는 200줄을 넘지 않도록 한다.
200줄을 초과한다면 역할이 두 개 이상이라는 신호다. 분리를 검토한다.

---

# 🙋 개발자 확인 원칙

판단할 수 없는 상황에서는 **절대 임의로 가정하고 진행하지 않는다.**
불확실한 내용을 추측하여 생성하면 잘못된 코드를 수정하는 비용이 처음부터 확인하는 비용보다 크다.

다음 상황에서는 반드시 개발자에게 확인 후 진행한다.

- 고객사 브랜드가 명시되지 않은 경우 (지원 브랜드: 하나은행·신한은행·KB국민은행·IBK기업은행·NH농협은행·우리은행 등)
- Figma 컴포넌트가 `docs/component-map.md`에 없는 경우
- 엔티티명을 Figma 디자인에서 판단할 수 없는 경우
- API 엔드포인트, 응답 구조, pagination 방식이 불명확한 경우
- Table vs Card vs Form 등 화면 타입이 애매한 경우
- Form의 create/update 용도, validation 방식이 불명확한 경우
- 생성하려는 파일이 이미 존재하거나 라우터 URL이 충돌하는 경우
- 비즈니스 로직이나 권한 처리 방식을 추론할 수 없는 경우

확인 질문은 한 번에 모아서 한다. 항목마다 개별로 질문하지 않는다.

---

# 🎯 라이브러리 최종 목표

Claude가 생성한 코드는 **Figma 디자인과 동일한 UI**를 보여주면서,
외부 개발자가 처음 보더라도 구조와 의도를 바로 이해할 수 있어야 한다.

---

# 🎨 디자인 토큰 규칙

## globals.css는 자동 생성 파일이다

`design-tokens/globals.css`는 **직접 수정하지 않는다.**
이 파일은 Figma Variables → Token Studio export → Claude 변환 과정을 통해서만 업데이트된다.

## temp.json을 받으면 반드시 아래 절차를 수행한다

누군가 `temp.json` 파일을 전달하거나 "globals.css 업데이트해줘"라고 요청하면,
아래 절차를 순서대로 수행한다. 절대 globals.css를 직접 수정하는 것으로 시작하지 않는다.

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

## 토큰 파일 구조 규칙

변환 시 아래 파일 구조와 경로를 반드시 유지한다.

```
design-tokens/
├── globals.css              ← 자동 생성 (직접 수정 금지)
└── figma-tokens/
    ├── primitives.json      ← spacing / radius / text / font / shadow 등 고정값
    ├── semantic.json        ← color.* 시맨틱 토큰 (brand·domain 토큰 참조)
    ├── brand.hana.json      ← 하나은행
    ├── brand.ibk.json       ← IBK기업은행
    ├── brand.kb.json        ← KB국민은행
    ├── brand.nh.json        ← NH농협은행
    ├── brand.shinhan.json   ← 신한은행
    ├── brand.woori.json     ← 우리은행
    ├── domain.banking.json  ← 뱅킹 도메인
    ├── domain.card.json     ← 카드 도메인
    ├── domain.giro.json     ← 지로 도메인
    └── domain.insurance.json← 보험 도메인
```

신규 브랜드·도메인이 temp.json에 포함되어 있으면 대응하는 파일을 새로 생성한다.
기존에 없는 키가 추가된 경우 개발자에게 확인 후 적절한 파일에 배치한다.

## figma-plugin/src/tokens.ts와의 관계

`tokens.ts`는 **Figma 플러그인 전용** 파일이다.
globals.css와 완전히 동기화할 필요 없으며, 아래 토큰만 관리한다.

- Figma 컴포넌트 생성 시 사용하는 **변수 경로 상수** (Figma Variables 바인딩용 경로) 와 **값 상수**(Figma Plugin API용 런타임 값) 조합으로 이루어져있다.
- 브랜드별·도메인별 색상은 tokens.ts에 포함하지 않는다.
- Figma 컴포넌트를 생성할 때 필요한 변수 경로 상수가 tokens.ts에 없는 경우, 임의로 추가하지 않고 **반드시 개발자에게 확인** 후 진행한다.
- temp.json 업데이트 작업에서 tokens.ts는 수정하지 않는다.
- tokens.ts 수정이 필요한 경우 개발자에게 별도로 확인한다.

---

# 🔌 Figma 플러그인 컴포넌트 생성 규칙

`figma-plugin/src/components/` 하위에 컴포넌트 생성 파일을 작성할 때 반드시 아래 규칙을 따른다.

## 변수 바인딩 원칙

모든 속성은 **Figma Variables가 존재하면 바인딩하고, 없으면 fallback 값으로 폴백**한다.
속성 종류에 따라 사용하는 헬퍼가 다르다.

### 색상 속성 → `COLOR_VAR + setFillWithVar / addTextWithVar`

```ts
// fill 색상 바인딩
await setFillWithVar(node, COLOR_VAR.surface, COLOR.surface);

// 텍스트 생성 + 색상 바인딩 (동시에 처리)
await addTextWithVar(parent, '텍스트', FONT_SIZE.base, COLOR_VAR.textHeading, COLOR.textHeading, bold);
```

### 수치 속성(spacing, radius, fontSize) → `SIZE_VAR + setFloatVar`

`setAutoLayout`, `setPadding`, `node.cornerRadius =` 등으로 fallback 값을 먼저 설정한 뒤,
반드시 곧바로 `setFloatVar`로 동일 필드를 Variable에 바인딩한다.

```ts
// itemSpacing
setAutoLayout(node, 'VERTICAL', SPACING.md);
await setFloatVar(node, 'itemSpacing', SIZE_VAR.spacingMd, SPACING.md);

// padding (setPadding으로 fallback 설정 후 각 필드를 개별 바인딩)
setPadding(node, SPACING.xl, SPACING.xl);
await setFloatVar(node, 'paddingTop',    SIZE_VAR.spacingXl, SPACING.xl);
await setFloatVar(node, 'paddingRight',  SIZE_VAR.spacingXl, SPACING.xl);
await setFloatVar(node, 'paddingBottom', SIZE_VAR.spacingXl, SPACING.xl);
await setFloatVar(node, 'paddingLeft',   SIZE_VAR.spacingXl, SPACING.xl);

// cornerRadius
await setFloatVar(node, 'cornerRadius', SIZE_VAR.radiusFull, RADIUS.full);

// 상단만 radius 적용하는 경우 (예: BottomSheet rounded-t-2xl)
await setFloatVar(node, 'topLeftRadius',  SIZE_VAR.radiusLg, RADIUS.lg);
await setFloatVar(node, 'topRightRadius', SIZE_VAR.radiusLg, RADIUS.lg);
node.bottomLeftRadius  = 0;
node.bottomRightRadius = 0;

// fontSize — addTextWithVar의 7번째 인자로 전달
await addTextWithVar(parent, '텍스트', FONT_SIZE.sm, COLOR_VAR.textBase, COLOR.textBase, bold, SIZE_VAR.fontSizeSm);
```

값이 `0`인 padding도 `SIZE_VAR.spacing0`가 tokens.ts에 존재하므로 동일하게 바인딩한다.

```ts
// 예: paddingTop/Bottom = 0인 경우
setPadding(node, 0, SPACING.md);
await setFloatVar(node, 'paddingTop',    SIZE_VAR.spacing0,  SPACING['0']);
await setFloatVar(node, 'paddingBottom', SIZE_VAR.spacing0,  SPACING['0']);
await setFloatVar(node, 'paddingRight',  SIZE_VAR.spacingMd, SPACING.md);
await setFloatVar(node, 'paddingLeft',   SIZE_VAR.spacingMd, SPACING.md);
```

### stroke 속성 → `COLOR_VAR + setStrokeWithVar`

```ts
// stroke에 COLOR 변수 바인딩 (setStroke는 RGB만 받으므로 반드시 이 헬퍼 사용)
await setStrokeWithVar(node, COLOR_VAR.border, COLOR.border);
await setStrokeWithVar(node, COLOR_VAR.brandPrimary, BRAND.primary, 2); // weight 지정 가능
```

### lineHeight 속성 → `SIZE_VAR + setLineHeightVar`

```ts
// lineHeight는 { value, unit } 객체 구조라 setFloatVar로 처리 불가 — 전용 헬퍼 사용
const text = await addTextWithVar(parent, '텍스트', FONT_SIZE.base, COLOR_VAR.textHeading, COLOR.textHeading, bold, SIZE_VAR.fontSizeBase);
await setLineHeightVar(text, SIZE_VAR.lineHeightBase, LINE_HEIGHT.base);
```

## `layoutSizingHorizontal = 'FILL'` / `layoutGrow = 1` 설정 순서

반드시 `parent.appendChild(child)` **이후에** 설정한다. 이전에 설정하면 Figma가 조용히 무시한다.

```ts
// ❌ 잘못된 예 — Figma가 무시함
node.layoutSizingHorizontal = 'FILL';
parent.appendChild(node);

// ✅ 올바른 예 — appendChild 이후에 설정
parent.appendChild(node);
node.layoutSizingHorizontal = 'FILL';
```

## 헬퍼 함수 (`figma-plugin/src/helpers.ts`)

| 함수 | 용도 |
|------|------|
| `setFillWithVar(node, colorVar, fallback)` | fill에 COLOR 변수 바인딩 |
| `setStrokeWithVar(node, colorVar, fallback, weight?)` | stroke에 COLOR 변수 바인딩 (setStroke 대체) |
| `setFloatVar(node, field, sizeVar, fallback)` | 수치 필드(padding/radius/itemSpacing 등)에 FLOAT 변수 바인딩 |
| `setLineHeightVar(node, sizeVar, fallback)` | TextNode lineHeight에 FLOAT 변수 바인딩 (setFloatVar 대체) |
| `addTextWithVar(parent, text, fontSize, colorVar, fallback, bold?, fontSizeVar?)` | 텍스트 생성 + 색상·fontSize 변수 바인딩 |
| `setAutoLayout(node, direction, gap, align?)` | Auto Layout 설정 (gap은 fallback용 — 이후 setFloatVar로 바인딩) |
| `setPadding(node, top, right, bottom?, left?)` | padding 설정 (fallback용 — 이후 setFloatVar로 바인딩) |
| `combineVariants(components, name, cols?)` | variant 배열을 그리드 배치 후 ComponentSet으로 묶음 |

---