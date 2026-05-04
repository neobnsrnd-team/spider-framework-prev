# Figma Plugin 사용 가이드

> React component-library와 Figma 캔버스를 동기화하는 플러그인의 구조, 빌드, 커맨드 사용법,  
> 컴포넌트 추가·수정·삭제 방법을 설명한다.

---

## 1. 개요

이 플러그인은 **React component-library의 컴포넌트를 Figma 캔버스에 자동 생성**한다.  
플러그인을 실행하면 `Core / Modules / Layout / Biz` 카테고리별 섹션이 그려진다.

```
React 컴포넌트 추가·수정
        ↓
figma-plugin 수정 (이 가이드 참고)
        ↓
npm run build:plugin
        ↓
Figma에서 플러그인 실행
        ↓
캔버스에 컴포넌트 자동 생성 → 디자이너가 활용
```

---

## 2. 파일 구조

```
figma-plugin/
├── main.ts                        ← 진입점 (커맨드 라우터만 담당)
├── createComponents.ts            ← ⭐ 컴포넌트 등록 진입점 (import + 배열 + 섹션 배치)
├── createIcons.ts                 ← 아이콘 생성 커맨드
├── createVariables.ts             ← 디자인 변수 등록 커맨드
├── icons.ts                       ← lucide-react 아이콘 SVG 데이터 (자동 생성)
├── tokens.ts                      ← 디자인 토큰 (색상·간격·폰트)  ※ 직접 수정 금지
├── helpers.ts                     ← Figma API 유틸 함수
├── manifest.json                  ← 플러그인 메타·커맨드 목록
├── tsconfig.json
└── components/
    ├── core/                      ← Button, Input, Badge, Typography, Select
    ├── layout/                    ← PageHeader, BottomNav, ModalSlideOver 등
    ├── modules/
    │   ├── common/                ← Card, Modal, DropdownMenu 등 범용 모듈
    │   └── banking/               ← AmountInput, OtpInput 등 뱅킹 전용
    └── biz/                       ← 도메인 비즈니스 컴포넌트
        ├── banking/
        ├── card/
        ├── common/
        └── insurance/
```

### 카테고리 선택 기준

| 위치 | 기준 |
|---|---|
| `core/` | 범용 원자 컴포넌트 (Button, Input 등) |
| `layout/` | 페이지 레이아웃·네비게이션 |
| `modules/common/` | 도메인 무관 모듈 컴포넌트 |
| `modules/banking/` | 뱅킹 전용 모듈 |
| `biz/` | 도메인 비즈니스 컴포넌트 |

---

## 3. 빌드 및 실행

### 빌드

```bash
# 루트(reactive-springware/)에서 실행
npm run build:plugin
```

빌드 결과물은 `figma-plugin/dist/main.js`에 생성된다.

### Figma에서 실행

Figma 데스크탑 앱 → 플러그인 메뉴 → 개발 → 플러그인 불러오기 → `figma-plugin/manifest.json` 선택

> 플러그인은 기존 노드를 업데이트하지 않고 새로 생성한다.  
> 재실행 전에 기존 컴포넌트를 캔버스에서 삭제한다.

---

## 4. 커맨드

플러그인은 `manifest.json`에 등록된 3개의 커맨드를 제공한다.  
`main.ts`는 커맨드에 따라 각 파일로 실행을 위임하는 라우터 역할만 한다.

| 메뉴 이름 | 커맨드 | 담당 파일 | 설명 |
|---|---|---|---|
| 컴포넌트 생성 | `createComponents` | `createComponents.ts` | 전체 컴포넌트를 섹션별로 캔버스에 생성 |
| 아이콘 생성 (Instance Swap용) | `createIcons` | `createIcons.ts` | `Icons/*` ComponentNode 일괄 생성 |
| 변수 등록 (Primitives + Semantic) | `createVariables` | `createVariables.ts` | Figma 색상 변수 등록 |

### 아이콘 생성 커맨드 상세

"아이콘 생성" 커맨드는 `icons.ts`의 lucide-react 아이콘 데이터를 기반으로  
`Icons/ChevronRight` 형식의 ComponentNode를 캔버스에 만든다.

컴포넌트 생성(`createComponents`) 실행 시 아이콘 슬롯(INSTANCE_SWAP)이 `Icons/*` 컴포넌트를 참조한다.  
**컴포넌트 생성 전에 아이콘을 먼저 생성해두는 것을 권장한다.**

```
① 아이콘 생성 커맨드 실행  →  Icons/* ComponentNode 3,894개 생성
② 컴포넌트 생성 커맨드 실행  →  각 컴포넌트의 아이콘 슬롯이 Icons/*를 참조
③ 디자이너가 오른쪽 패널에서 아이콘 교체 가능
```

#### 아이콘 데이터 갱신

`icons.ts`는 `npm run generate:icons`로 자동 생성된다. lucide-react 버전 업 시 재실행한다.

```bash
npm run generate:icons   # icons.ts 재생성
npm run build:plugin     # 플러그인 번들 재빌드
```

---

## 5. 컴포넌트 추가

### Step 1 — create 파일 생성

카테고리에 맞는 폴더에 `create[ComponentName].ts` 파일을 생성한다.

```
예: DropdownMenu  →  components/modules/common/createDropdownMenu.ts
예: CardSummaryCard  →  components/biz/card/createCardSummaryCard.ts
```

**파일 기본 구조:**

```ts
/// <reference types="@figma/plugin-typings" />
/**
 * @file create[ComponentName].ts
 * @description [컴포넌트 설명]
 */

import { COLOR, SPACING, FONT_SIZE } from '../../tokens';
import { createComponent, setAutoLayout, setPadding, setFill, addTextWithVar } from '../../helpers';

export async function create[ComponentName](): Promise<ComponentSetNode> {
  const comp = createComponent('Variant=Default');
  // ... Figma API로 컴포넌트 구성
  return combineVariants([comp], '[ComponentName]', 1);
}
```

**Variant가 있는 경우:**

```ts
import { combineVariants } from '../../helpers';

async function createVariant(open: boolean): Promise<ComponentNode> {
  const comp = createComponent(`Open=${open ? 'True' : 'False'}`);
  // ...
  return comp;
}

export async function createDropdownMenu(): Promise<ComponentSetNode> {
  const components = await Promise.all([
    createVariant(false),
    createVariant(true),
  ]);
  return combineVariants(components, 'DropdownMenu', 2);
}
```

**아이콘 슬롯 추가:**

```ts
import { addIconSlot } from '../../helpers';

// INSTANCE_SWAP 슬롯 — Icons/* 컴포넌트가 없으면 rect fallback
addIconSlot(comp, 'ChevronRight', 16, COLOR.textMuted);
```

### Step 2 — createComponents.ts에 등록

```ts
// ① import 추가 (카테고리별 import 블록에 추가)
import { createDropdownMenu } from './components/modules/common/createDropdownMenu';

// ② 해당 배열에 추가
const moduleCommonNodes: SceneNode[] = [
  // ... 기존 항목들
  await s('createDropdownMenu', createDropdownMenu),  // ← 추가
];
```

**배열 이름 기준:**

| 배열 | 대응 카테고리 |
|---|---|
| `coreNodes` | `core/` |
| `moduleCommonNodes` | `modules/common/` |
| `moduleBankingNodes` | `modules/banking/` |
| `layoutNodes` | `layout/` |
| `bizBankingNodes` | `biz/banking/` |
| `bizCommonNodes` | `biz/common/` |
| `bizCardNodes` | `biz/card/` |
| `bizInsuranceNodes` | `biz/insurance/` |

---

## 6. 컴포넌트 수정

React 컴포넌트의 props·레이아웃·색상이 바뀌었을 때 해당 `create*.ts` 파일을 수정한다.

| 변경 사항 | 수정 위치 |
|---|---|
| 새 prop 추가 (예: `disabled`) | `create*.ts` — variant 분기 추가 |
| 레이아웃 방향 변경 | `setAutoLayout(comp, 'HORIZONTAL' \| 'VERTICAL', gap)` |
| 색상 변경 | `setFillWithVar(node, COLOR_VAR.xxx, fallback)` |
| 텍스트 변경 | `addTextWithVar(parent, '텍스트', FONT_SIZE.sm, COLOR_VAR.xxx, fallback)` |
| 아이콘 변경 | `addIconSlot(comp, 'IconName', size, fallbackColor)` — 아이콘명은 lucide-react 기준 |

---

## 7. 컴포넌트 삭제

1. `components/[카테고리]/create[ComponentName].ts` 파일 삭제
2. `createComponents.ts`에서 import 제거
3. `createComponents.ts`의 배열에서 `await s('create[ComponentName]', create[ComponentName])` 제거
4. `createComponents.ts` 상단 JSDoc 주석의 컴포넌트 목록에서 제거

---

## 8. helpers.ts 주요 유틸 함수

| 함수 | 설명 |
|---|---|
| `createComponent(name)` | ComponentNode 생성 |
| `combineVariants(nodes, name, cols)` | ComponentNode 배열 → ComponentSetNode |
| `setAutoLayout(node, direction, gap)` | Auto Layout 설정 |
| `setPadding(node, vertical, horizontal)` | 패딩 설정 |
| `setFill(node, color)` | 단색 배경 적용 |
| `setFillWithVar(node, varName, fallback)` | Figma 변수 바인딩 배경 적용 |
| `setStroke(node, color)` | 테두리 적용 |
| `addTextWithVar(parent, text, size, varName, fallback)` | 텍스트 노드 생성 및 변수 바인딩 |
| `addRect(parent, w, h, color, radius?)` | 사각형 노드 생성 |
| `addIconSlot(comp, name, size, fallbackColor)` | INSTANCE_SWAP 아이콘 슬롯 추가 |
| `setFloatVar(node, prop, varName, fallback)` | Float 속성에 Figma 변수 바인딩 |

---

## 9. 주의사항

> **tokens.ts는 직접 수정하지 않는다.**  
> 새 토큰이 필요하다고 판단되더라도 임의로 추가하지 않고 개발자에게 확인한다.  
> `tokens.ts`는 `design-tokens/globals.css`와 동기화 절차를 통해서만 업데이트된다.

> **아이콘명은 lucide-react 기준이다.**  
> `addIconSlot(comp, 'Settings', 16, COLOR.textMuted)` — 두 번째 인자는 lucide-react의 컴포넌트명과 동일해야 한다.

> **`s()` 헬퍼로 컴포넌트를 등록한다.**  
> `await s('createDropdownMenu', createDropdownMenu)` 형식을 사용해야 오류 발생 시 어느 컴포넌트에서 실패했는지 메시지에 표시된다.

---

## 10. Claude로 작업하기

Claude에게 작업을 맡기면 파일 생성부터 `createComponents.ts` 등록까지 한 번에 처리할 수 있다.

### Claude에게 넘겨야 할 정보

```
1. React 컴포넌트 코드 (index.tsx, types.ts)
2. 참고할 기존 create*.ts 파일 1~2개 (비슷한 구조의 컴포넌트)
3. 배치할 카테고리 (core / modules/common / biz/card 등)
```

### 프롬프트 템플릿

```
아래 React 컴포넌트를 Figma 플러그인에 반영해줘.

[React 컴포넌트 코드]
// index.tsx 내용 붙여넣기

[참고 파일]
// 비슷한 구조의 기존 create*.ts 내용 붙여넣기

요청:
- 카테고리: modules/common
- create[ComponentName].ts 파일 생성
- createComponents.ts에 import 및 moduleCommonNodes 배열 등록
```
