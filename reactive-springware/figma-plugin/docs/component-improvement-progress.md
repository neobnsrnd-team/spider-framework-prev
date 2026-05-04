# Figma Plugin 컴포넌트 개선 작업 진행 현황

## 배경

피그마 플러그인(`figma-plugin/`)의 컴포넌트 생성 프로세스 개선 작업.  
AI(Claude)가 Figma 화면을 보고 React 페이지 컴포넌트를 자동 생성하는 것을 최종 목표로 한다.

---

## 개선 항목 전체 목록

| # | 항목 | 상태 |
|---|------|------|
| 1 | 컴포넌트 생성 순서 변경 | ✅ 완료 |
| 2 | 각 섹션 내 알파벳 순 정렬 | ✅ 완료 |
| 3 | React props 명 ↔ Figma variant 명 일치 | 🔲 미완료 |
| 4 | 텍스트를 variant(TEXT property)로 설정 | ✅ 완료 |
| 5 | 아이콘 영역 디폴트 설정 | ✅ 완료 |

---

## 완료된 작업 상세

### 1. 컴포넌트 생성 순서 (`createComponents.ts`)

**변경 전:** Core → Modules/Common → Modules/Banking → Layout → Biz/Banking → Biz/Common → Biz/Card → Biz/Insurance

**변경 후:** Core → Layout → Modules/Common → Modules/Banking → **Biz/Common** → **Biz/Banking** → Biz/Card → Biz/Insurance

### 2. 섹션 내 알파벳 순 정렬 (`createComponents.ts`)

각 섹션 배열을 컴포넌트 이름 기준 알파벳 순으로 재정렬.  
Layout 섹션은 의존 관계(PageLayouts → pageHeader/homeHeader/bottomNav 참조) 때문에 **생성 순서는 유지**하고, `layoutNodes` 배열만 알파벳 순으로 구성.

### 5. 아이콘 영역 디폴트 설정

#### `helpers.ts` — `addIconSlot` 시그니처 변경

선택적 6번째 파라미터 `parent?: FrameNode | ComponentNode` 추가.  
아이콘 인스턴스는 `parent`(지정 시) 또는 `comp`에 삽입하고, INSTANCE_SWAP 등록은 항상 최상위 `comp`에서 수행.

> **주의:** `componentPropertyReferences`는 ComponentNode 기준 2단계 이내 자식까지만 설정 가능.  
> 3단계 이상 중첩 시 "Can only set component property references on symbol sublayer" 오류 발생.

#### 수정된 컴포넌트

| 파일 | 변경 내용 | 디폴트 아이콘 |
|------|---------|------------|
| `components/core/createInput.ts` | `addRect()` → `addIconSlot()` | `Search` |
| `components/modules/common/createSelectableItem.ts` | `createEllipse()` → iconWrap Frame + `addIconSlot()` | `Home` |
| `components/modules/common/createNoticeItem.ts` | `createIcon('Bell')` 하드코딩 → `addIconSlot()` | `Bell` (swap 가능) |
| `components/modules/banking/createAccountSelectItem.ts` | `createEllipse()` → iconWrap Frame + `addIconSlot()` | `Building2` |
| `components/biz/banking/createAccountSelectorCard.ts` | `createEllipse()` → rightBtn Frame + `addIconSlot()` | `ChevronRight` |
| `components/biz/common/createQuickMenuGrid.ts` | `createRectangle()` → iconBg Frame + `createIcon()` | `LayoutGrid` (정적, 3단계 중첩 제한) |

---

## 미완료 작업 상세

### 3. React props 명 ↔ Figma variant 명 일치

#### 결정된 방향

**목적:** AI(Claude)가 Figma 화면 → React 코드 자동 생성 시 1:1 매핑 정확도 최대화  
**원칙:** Figma variant 키·값을 React prop 키·값과 완전 일치시킨다.

| 항목 | 현재 Figma | 변경 후 |
|------|-----------|--------|
| 키 케이스 | PascalCase (`Size`, `Variant`) | camelCase (`size`, `variant`) |
| size 값 | `Small\|Medium\|Large` | `sm\|md\|lg` |
| variant 값 | `Primary\|Outline\|Ghost\|Danger` | `primary\|outline\|ghost\|danger` |
| Input prop명 | `State` | `validationState` |
| state 값 | `Default\|Error\|Success\|Disabled` | `default\|error\|success\|disabled` |
| boolean 표현 | `Dot=True\|False` | `dot=true\|false` |
| State 구분 | `State=Loading\|Disabled` | `state=loading\|disabled` |

#### 영향 받는 파일 목록

- `components/core/createButton.ts` — `Variant`, `Size`, `State`, `Icon`, `Justify`
- `components/core/createBadge.ts` — `Variant`, `Dot`
- `components/core/createInput.ts` — `Size`, `State`, `Icon`, `Format`
- `components/core/createSelect.ts` — 확인 필요
- `components/core/createTypography.ts` — 확인 필요
- `components/modules/common/createCheckbox.ts` — `Shape` 확인 필요
- `components/modules/common/createTabNav.ts` — `Variant` 확인 필요
- `components/modules/common/createAlertBanner.ts` — `Intent` 확인 필요
- `components/modules/common/createModal.ts` — `Size`, `TitleAlign` 확인 필요
- `components/modules/common/createBottomSheet.ts` — `Snap` 확인 필요
- `components/biz/card/createCardVisual.ts` — `Mode`/`Compact` 확인 필요
- 기타 모든 컴포넌트 파일 전수 확인 필요

#### 구현 방법

각 컴포넌트 파일에서 `createComponent('Variant=Primary, Size=Small, ...')` 형식의 문자열을  
`createComponent('variant=primary, size=sm, ...')` 형식으로 변경.

---

### 4. 텍스트를 TEXT component property로 설정 ✅

#### 구현 내용

`helpers.ts`의 `addTextWithVar()` 함수에 선택적 8·9번째 파라미터 추가:

```typescript
export async function addTextWithVar(
  parent, characters, fontSize, colorVar, fallback, bold?, fontSizeVar?,
  textPropertyName?: string,  // TEXT property 이름
  comp?: ComponentNode,        // parent가 FrameNode일 때 명시적으로 전달
): Promise<TextNode>
// textPropertyName이 있으면:
// const root = comp ?? (parent.type === 'COMPONENT' ? parent : null);
// if (root) { text.componentPropertyReferences = { characters: root.addComponentProperty(...) }; }
```

> **주의:** `componentPropertyReferences`는 텍스트 노드가 ComponentNode 서브트리에 속해 있어야 바인딩 가능.  
> `addTextWithVar()` 호출 전에 반드시 `comp.appendChild(frame)` 또는 `parent.appendChild(child)`로 선 삽입 필요.

#### 적용된 컴포넌트 목록

**Core / Layout**
- `createInput.ts` — `placeholder`, `label`
- `createAppBrandHeader.ts` — `brandInitial`, `brandName`

**Modules/Common**
- `createBalanceToggle.ts` — `toggleLabel`
- `createBankSelectGrid.ts` — `bankName`
- `createBottomSheet.ts` — `title`
- `createCheckbox.ts` — `label`
- `createErrorState.ts` — `title`, `description`, `retryLabel`
- `createModal.ts` — `title`, `content`
- `createRecentRecipientItem.ts` — `name`, `accountNumber`
- `createSectionHeader.ts` — `title`, `badgeCount`, `actionLabel`
- `createSelectableListItem.ts` — `label`
- `createTransactionList.ts` — `emptyMessage`
- `createTransferLimitInfo.ts` — `sectionTitle`

**Biz/Card**
- `createAccountSelectCard.ts` — `bankName`, `accountNumber`
- `createBillingPeriodLabel.ts` — `billingPeriodText`
- `createCardBenefitSummary.ts` — (스킵: 중첩 헬퍼 함수 내부, comp 접근 불가)
- `createCardChipItem.ts` — `cardName`, `cardNumber`
- `createCardInfoPanel.ts` — (스킵: 중첩 헬퍼 함수)
- `createCardLinkedBalance.ts` — `limitLabel`, `balanceAmount`, `badgeLabel`
- `createCardManagementPanel.ts` — `sectionTitle`
- `createCardPaymentActions.ts` — `actionLabel`
- `createCardPaymentItem.ts` — `cardNetworkLabel`, `cardName`, `transactionAmount`, `detailLabel`
- `createCardPaymentSummary.ts` — `billingMonth`, `paymentSchedule`, `totalAmount`
- `createCardPerformanceBar.ts` — `cardName`, `detailLabel`, `usageAmount`, `targetAmount`, `statusMessage`
- `createCardPillTab.ts` — `tabLabel`
- `createCardSummaryCard.ts` — `cardName`, `badgeLabel`, `maskedCardNumber`, `amountLabel`, `amountValue`, `creditLimit`
- `createCardVisual.ts` — `brandLabel`, `cardName`
- `createLoanMenuBar.ts` — `menuLabel`
- `createPaymentAccountCard.ts` — `accountLabel`, `operatingHours`
- `createQuickShortcutCard.ts` — `cardTitle`, `couponCount`
- `createStatementHeroCard.ts` — `cardLabel`, `amountValue`, `amountUnit`, `paymentDateLabel`
- `createStatementTotalCard.ts` — `sectionLabel`, `statusBadge`, `totalAmount`
- `createSummaryCard.ts` — `cardTitle`, `cardAmount`
- `createUsageHistoryFilterSheet.ts` — `filterSummary`, `sheetTitle`, `submitLabel`
- `createUsageTransactionItem.ts` — `merchantName`, `transactionMeta`, `transactionAmount`

**Biz/Banking**
- `createAccountSelectorCard.ts` — (스킵: `addText()` 사용)

**Biz/Insurance**
- `createInsuranceSummaryCard.ts` — `insuranceName`, `statusLabel`, `policyNumber`

---

## 참고: React component-library props 매핑 요약

| 컴포넌트 | React prop | 값 |
|---------|-----------|---|
| Button | `variant` | `primary\|outline\|ghost\|danger` |
| Button | `size` | `sm\|md\|lg` |
| Button | `loading` | boolean |
| Button | `disabled` | boolean |
| Badge | `variant` | `primary\|brand\|success\|danger\|warning\|neutral` |
| Badge | `dot` | boolean |
| Input | `validationState` | `default\|error\|success` |
| Input | `size` | `md\|lg` |
| Typography | `variant` | `heading\|subheading\|body-lg\|body\|body-sm\|caption` |
| Typography | `weight` | `normal\|medium\|bold` |
| Typography | `color` | `heading\|base\|label\|secondary\|muted\|brand\|danger\|success` |
| Checkbox | `shape` | `square\|circle` |
| TabNav | `variant` | `underline\|pill` |
| AlertBanner | `intent` | `warning\|danger\|success\|info` |
| Modal | `size` | `sm\|md\|lg\|fullscreen` |
| Modal | `titleAlign` | `left\|center` |
| BottomSheet | `snap` | `auto\|half\|full` |
| CardVisual | `compact` | boolean |
