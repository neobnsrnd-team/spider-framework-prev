# Figma Plugin 수정 가이드

> React component-library에 컴포넌트를 추가·수정·삭제했을 때,  
> 피그마와 동기화하기 위해 플러그인을 어떻게 수정하는지 설명한다.

---

## 1. 개요

이 플러그인은 **React component-library의 컴포넌트를 Figma 캔버스에 자동 생성**한다.  
플러그인을 실행하면 `core / modules / layout / biz` 카테고리별로 섹션이 그려진다.

```
React 컴포넌트 추가·수정
        ↓
figma-plugin 수정 (이 가이드)
        ↓
Figma에서 플러그인 실행
        ↓
캔버스에 컴포넌트 자동 생성 → 디자이너가 활용
```

---

## 2. 파일 구조

```
figma-plugin/
├── src/
│   ├── main.ts                        ← ⭐ 컴포넌트 등록 진입점 (import + 배열)
│   ├── tokens.ts                      ← 디자인 토큰 (색상·간격·폰트)  ※ 직접 수정 금지
│   ├── helpers.ts                     ← Figma API 유틸 함수
│   ├── icons.ts                       ← 아이콘 생성 함수
│   ├── createVariables.ts             ← Figma 변수 등록
│   └── components/
│       ├── core/                      ← Button, Input, Badge, Typography, Select
│       ├── layout/                    ← PageHeader, BottomNav, TabNav 등
│       ├── module/
│       │   ├── common/                ← Card, Modal, DropdownMenu 등 범용 모듈
│       │   └── banking/               ← AmountInput, OtpInput 등 뱅킹 전용
│       └── biz/                       ← AccountSummaryCard, UserProfile 등 도메인 컴포넌트
└── GUIDE.md                           ← 이 문서
```

### 카테고리 선택 기준

| 위치 | 기준 |
|---|---|
| `core/` | 범용 원자 컴포넌트 (Button, Input 등) |
| `layout/` | 페이지 레이아웃·네비게이션 |
| `module/common/` | 도메인 무관 모듈 컴포넌트 |
| `module/banking/` | 뱅킹 전용 모듈 |
| `biz/` | 도메인 비즈니스 컴포넌트 |

---

## 3. 컴포넌트 추가

### Step 1 — create 파일 생성

카테고리에 맞는 폴더에 `create[ComponentName].ts` 파일을 생성한다.

```
예: DropdownMenu → src/components/module/common/createDropdownMenu.ts
예: CardSummaryCard → src/components/biz/createCardSummaryCard.ts
```

**파일 기본 구조:**

```ts
import { COLOR, SPACING, FONT_SIZE } from '../../../tokens';
import { createComponent, setAutoLayout, setPadding, setFill, clearFill, addText } from '../../../helpers';

export async function createDropdownMenu(): Promise<ComponentNode | ComponentSetNode> {
  const comp = createComponent('ComponentName');
  // ... Figma API로 컴포넌트 구성
  figma.currentPage.appendChild(comp);
  return comp;
}
```

**Variant가 있는 경우 (Open/Closed, Hidden/Visible 등):**

```ts
import { combineVariants } from '../../../helpers';

async function createVariant(open: boolean): Promise<ComponentNode> {
  const comp = createComponent(`Open=${open ? 'True' : 'False'}`);
  // ...
  figma.currentPage.appendChild(comp);
  return comp;
}

export async function createDropdownMenu(): Promise<ComponentSetNode> {
  return combineVariants(
    await Promise.all([createVariant(false), createVariant(true)]),
    'DropdownMenu',
    2, // 한 행에 표시할 variant 수
  );
}
```

### Step 2 — main.ts에 등록

```ts
// ① import 추가 (카테고리별 import 블록에 추가)
import { createDropdownMenu } from './components/module/common/createDropdownMenu';

// ② 해당 배열에 추가
const moduleCommonNodes: SceneNode[] = [
  // ... 기존 항목들
  await createDropdownMenu(),  // ← 추가
];
```

**배열 이름 기준:**

| 배열 | 대응 카테고리 |
|---|---|
| `coreNodes` | `core/` |
| `moduleCommonNodes` | `module/common/` |
| `moduleBankingNodes` | `module/banking/` |
| `layoutNodes` | `layout/` |
| `bizNodes` | `biz/` |

---

## 4. 컴포넌트 수정

React 컴포넌트의 **props·레이아웃·색상**이 바뀌었을 때 해당 `create*.ts` 파일을 수정한다.

**주요 수정 패턴:**

| 변경 사항 | 수정 위치 |
|---|---|
| 새 prop 추가 (예: `hidden`) | `create*.ts` — variant 분기 추가 |
| 레이아웃 방향 변경 | `setAutoLayout(comp, 'HORIZONTAL' \| 'VERTICAL', ...)` |
| 색상 변경 | `setFill(node, COLOR.xxx)` 또는 `setFillWithVar(node, COLOR_VAR.xxx, fallback)` |
| 텍스트 변경 | `addText(parent, '텍스트', FONT_SIZE.sm, COLOR.xxx)` |
| 아이콘 변경 | `createIcon('IconName', size, COLOR.xxx)` — 아이콘명은 lucide-react 기준 |

---

## 5. 컴포넌트 삭제

1. `src/components/[카테고리]/create[ComponentName].ts` 파일 삭제
2. `main.ts`에서 import 제거
3. `main.ts`의 배열에서 `await create[ComponentName]()` 제거
4. `main.ts` 상단 JSDoc 주석의 컴포넌트 목록에서 제거

---

## 6. Claude로 작업하기

Claude에게 작업을 맡기면 **파일 생성부터 main.ts 등록까지 한 번에** 처리할 수 있다.

### Claude에게 넘겨야 할 정보

```
1. React 컴포넌트 코드 (index.tsx, types.ts)
2. 참고할 기존 create*.ts 파일 1~2개 (비슷한 구조의 컴포넌트)
3. 배치할 카테고리 (core / module/common / biz 등)
```

### 프롬프트 템플릿

```
아래 React 컴포넌트를 Figma 플러그인에 반영해줘.

[React 컴포넌트 코드]
// index.tsx 내용 붙여넣기

[참고 파일]
// 비슷한 구조의 기존 create*.ts 내용 붙여넣기

요청:
- 카테고리: module/common
- create[ComponentName].ts 파일 생성
- main.ts에 import 및 moduleCommonNodes 배열 등록
```

### 실제 예시 — DropdownMenu 추가

```
아래 React 컴포넌트를 Figma 플러그인에 반영해줘.

[React 컴포넌트 코드]
(DropdownMenu/index.tsx 내용)

[참고 파일]
(createBalanceToggle.ts 내용 — variant 패턴 참고용)

요청:
- 카테고리: module/common
- Open=False / Open=True 2종 variant로 구성
- main.ts moduleCommonNodes에 등록
```

### 주의사항

> ⚠️ **tokens.ts는 직접 수정하지 않는다.**  
> 새 토큰이 필요하다고 판단되더라도 임의로 추가하지 않고 개발자에게 확인한다.  
> tokens.ts는 `design-tokens/globals.css`와 동기화 절차를 통해서만 업데이트된다.

> ⚠️ **아이콘명은 lucide-react 기준이다.**  
> `createIcon('Settings', 16, COLOR.textMuted)` — 첫 번째 인자는 lucide-react의 컴포넌트명과 동일해야 한다.

---

## 7. Figma에서 확인하기

1. `figma-plugin/` 디렉토리에서 빌드 실행
   ```bash
   npm run build
   ```
2. Figma 데스크탑 앱 → 플러그인 메뉴 → **"Create Components"** 실행
3. 캔버스에 새 컴포넌트가 생성되었는지 확인

> 기존 컴포넌트가 이미 캔버스에 있다면 삭제 후 다시 실행한다.  
> 플러그인은 기존 노드를 업데이트하지 않고 새로 생성한다.
