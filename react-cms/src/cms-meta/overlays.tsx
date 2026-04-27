/**
 * @file overlays.tsx
 * @description CMS OverlayTemplate 정의.
 * CMS 빌더 캔버스에서 바텀시트·모달 오버레이를 미리보기·렌더링하기 위한 템플릿 목록.
 *
 * 지원 오버레이:
 * - "BottomSheet" : modules/BottomSheet — 내부 블록을 children으로 렌더링
 * - "Modal"       : modules/Modal      — 내부 블록을 children으로 렌더링
 *
 * renderer 주의사항:
 *   BottomSheet / Modal은 내부적으로 `createPortal(content, document.body)`를 사용한다.
 *   CMS 캔버스에서는 `container` prop으로 전달받은 요소를 portal 타깃으로 대체해
 *   CMS 미리보기 영역 안에서 오버레이가 올바르게 렌더링되도록 한다.
 *
 * @example
 * // your-cms-app/src/main.tsx
 * import { overlays } from "@reactivespringware/component-library/cms/overlays";
 * <CMSApp blocks={blocks} overlays={overlays} layoutRenderer={layoutRenderer} />
 */
import type { OverlayTemplate, OverlayRendererProps } from "@cms-core";
import {
  BottomSheet,
  Modal,
  UsageHistoryFilterSheet,
  PinConfirmSheet,
  ModalSlideOver,
} from "@cl";
// ─────────────────────────────────────────────────────────────────────────────
// BottomSheet 렌더러
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CMS 미리보기용 BottomSheet 렌더러.
 * modules/BottomSheet의 시각 구조를 유지하면서,
 * portal 타깃을 `container`(CMS 미리보기 요소)로 교체한다.
 *
 * props 필드:
 *   - title          : 시트 상단 제목 (string)
 *   - snap           : 높이 프리셋 'auto' | 'half' | 'full'
 *   - hideCloseButton: X 버튼 숨김 여부 (boolean)
 */
function BottomSheetRenderer({ open, onClose, children, container, props }: OverlayRendererProps) {
  if (!open) return null;

  const title           = props?.title            as string | undefined;
  const hideCloseButton = (props?.hideCloseButton as boolean | undefined) ?? false;
  const content         = (props?.children)       as string | undefined;
  const bottomBtnCnt    = (props?.bottomBtnCnt    as "0" | "1" | "2" | undefined) ?? "0";
  const bottomBtn1Label = (props?.bottomBtn1Label as string | undefined) ?? "확인";
  const bottomBtn2Label = (props?.bottomBtn2Label as string | undefined) ?? "취소";


  return (
    <BottomSheet
      open={open}
      onClose={onClose}
      title={title}
      hideCloseButton={hideCloseButton}
      container={container ?? undefined}
      bottomBtnCnt={bottomBtnCnt}
      bottomBtn1Label={bottomBtn1Label}
      bottomBtn2Label={bottomBtn2Label}
    >
      { content ?? children }
    </BottomSheet>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// Modal 렌더러
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CMS 미리보기용 Modal 렌더러.
 * modules/Modal의 시각 구조를 유지하면서,
 * portal 타깃을 `container`(CMS 미리보기 요소)로 교체한다.
 *
 * 반응형 레이아웃:
 *   - 모바일 (md 미만): 화면 하단 Bottom Sheet
 *   - 데스크톱 (md 이상): 화면 중앙 다이얼로그
 *
 * props 필드:
 *   - title              : 모달 제목 (string)
 *   - size               : 최대 너비 프리셋 'sm' | 'md' | 'lg' | 'fullscreen'
 *   - disableBackdropClose: 백드롭 클릭 닫기 비활성화 (boolean)
 */
function ModalRenderer({ open, onClose, children, container, props }: OverlayRendererProps) {
  if (!open) return null;

  const title               = props?.title            as string | undefined;
  const content             = props?.children         as string | undefined;
  const bottomBtnCnt        = (props?.bottomBtnCnt    as "0" | "1" | "2" | undefined) ?? "0";
  const bottomBtn1Label     = (props?.bottomBtn1Label as string | undefined) ?? "확인";
  const bottomBtn2Label     = (props?.bottomBtn2Label as string | undefined) ?? "취소";

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={title}
      container={container ?? undefined}
      bottomBtnCnt={bottomBtnCnt}
      bottomBtn1Label={bottomBtn1Label}
      bottomBtn2Label={bottomBtn2Label}
    >
      { content ?? children }
    </Modal>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// UsageHistoryFilterSheet 렌더러
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CMS 미리보기용 UsageHistoryFilterSheet 렌더러.
 * 카드 선택 목록은 CMS 빌더에서 편집할 수 없으므로 대표 샘플을 고정 주입한다.
 */
const DEFAULT_CARD_OPTIONS = [
  { value: 'card1', label: '하나 머니 체크카드 (1234)' },
  { value: 'card2', label: '하나 1Q 신용카드 (5678)' },
];

function UsageHistoryFilterSheetRenderer({ open, onClose, container }: OverlayRendererProps) {
  if (!open) return null;
  return (
    <UsageHistoryFilterSheet
      open={open}
      onClose={onClose}
      cardOptions={DEFAULT_CARD_OPTIONS}
      onApply={() => { onClose(); }}
      container={container ?? undefined}
    />
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// PinConfirmSheet 렌더러
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CMS 미리보기용 PinConfirmSheet 렌더러.
 * PIN 입력 완료 시 onClose를 호출해 시트를 닫는다.
 *
 * props 필드:
 *   - title     : 시트 타이틀 (string)
 *   - pinLength : PIN 자릿수 4 | 6
 */
function PinConfirmSheetRenderer({ open, onClose, container, props }: OverlayRendererProps) {
  if (!open) return null;
  const title     = typeof props?.title === 'string' ? props.title : '비밀번호 입력';
  const pinLength = typeof props?.pinLength === 'number' ? props.pinLength : 4;
  return (
    <PinConfirmSheet
      open={open}
      onClose={onClose}
      onConfirm={() => { onClose(); }}
      title={title}
      pinLength={pinLength}
      container={container ?? undefined}
    />
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// ModalSlideOver 렌더러
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CMS 미리보기용 ModalSlideOver 렌더러.
 * children(내부 블록)을 슬라이드 오버 패널 안에 렌더링한다.
 *
 * props 필드:
 *   - direction : 슬라이드 방향 'right' | 'bottom'
 */
function ModalSlideOverRenderer({ open, onClose, children, container, props }: OverlayRendererProps) {
  if (!open) return null;
  const direction = props?.direction === 'bottom' ? 'bottom' : 'right';
  return (
    <ModalSlideOver direction={direction} onClose={onClose} container={container ?? undefined}>
      {children}
    </ModalSlideOver>
  );
}

// ─────────────────────────────────────────────────────────────────────────────
// OverlayTemplate 목록
// ─────────────────────────────────────────────────────────────────────────────

/**
 * CMS 빌더에 제공하는 오버레이 템플릿 목록.
 * CMSApp의 `overlays` prop에 그대로 전달한다.
 *
 * | id                    | type         | 설명                         |
 * |-----------------------|--------------|------------------------------|
 * | tpl_bottomsheet       | BottomSheet  | 내부 블록 자유 구성 시트        |
 * | tpl_bottomsheet_content | BottomSheet  | 텍스트 구성 시트             |
 * | tpl_modal             | Modal        | 내부 블록 자유 구성 모달        |
 * | tpl_modal_content     | Modal        | 텍스트 구성 모달               |
 */
export const overlays: OverlayTemplate[] = [
  // ── 바텀시트 ─────────────────────────────────
  {
    id:          "tpl_bottomsheet",
    label:       "바텀시트",
    description: "블록을 자유롭게 추가할 수 있는 바텀 시트",
    type:        "BottomSheet",
    defaultId:   "bottomSheetWithBlocks",
    componentName: "BottomSheet",
    blocks:      [],
    props: {
      title:     "바텀시트 제목",
      hideCloseButton: false,
      bottomBtnCnt: "0",
      bottomBtn1Label: "확인",
      bottomBtn2Label: "취소"
    },
    propSchema: {
      title:     { type: "string", label: "제목", default: "바텀시트 제목" },
      hideCloseButton: { type: "boolean", label: "닫기 버튼 숨김 여부",   default: false },
      bottomBtnCnt:    { type: "select",  label: "하단 버튼 수",        options: ["0", "1", "2"],  default: "0" },
      bottomBtn1Label: { type: "string",  label: "버튼1 텍스트",        default: "확인" },
      bottomBtn2Label: { type: "string",  label: "버튼2 텍스트",        default: "취소" },
    },
    renderer: BottomSheetRenderer,
  },
  {
    id:          "tpl_bottomsheet_content",
    label:       "바텀시트 (텍스트 구성)",
    description: "텍스트로 구성하는 바텀 시트",
    type:        "BottomSheet",
    defaultId:   "bottomSheetWithContent",
    componentName: "BottomSheet",
    blocks: [],
    props: {
      title:    "바텀시트 제목",
      hideCloseButton: false,
      children: "바텀시트 내용",
      bottomBtnCnt: "0",
      bottomBtn1Label: "확인",
      bottomBtn2Label: "취소"
    },
    propSchema: {
      title:           { type: "string",  label: "제목",              default: "바텀시트 제목" },
      hideCloseButton: { type: "boolean", label: "닫기 버튼 숨김 여부", default: false },
      children:        { type: "string",  label: "바텀시트 내용 입니다.", default: "바텀시트 내용" },
      bottomBtnCnt:    { type: "select",  label: "하단 버튼 수",        options: ["0", "1", "2"],   default: "0" },
      bottomBtn1Label: { type: "string",  label: "버튼1 텍스트",        default: "확인" },
      bottomBtn2Label: { type: "string",  label: "버튼2 텍스트",        default: "취소" },
    },
    renderer: BottomSheetRenderer,
  },
  
  // ── 모달 ─────────────────────────────────────────────────
  {
    id:          "tpl_modal",
    label:       "모달",
    description: "블록을 자유롭게 추가할 수 있는 모달",
    type:        "Modal",
    defaultId:   "modalWithBlocks",
    componentName: "Modal",
    blocks:      [],
    props: {
      title:     "모달 제목",
      bottomBtnCnt: "0",
      bottomBtn1Label: "확인",
      bottomBtn2Label: "취소"
    },
    propSchema: {
      title:           { type: "string",  label: "제목",         default: "모달 제목" },
      bottomBtnCnt:    { type: "select",  label: "하단 버튼 수",  options: ["0", "1", "2"],  default: "0" },
      bottomBtn1Label: { type: "string",  label: "버튼1 텍스트",  default: "확인" },
      bottomBtn2Label: { type: "string",  label: "버튼2 텍스트",  default: "취소" },
    },
    renderer: ModalRenderer,
  },
  {
    id:          "tpl_modal_content",
    label:       "모달 (텍스트 구성)",
    description: "텍스트로 구성하는 모달",
    type:        "Modal",
    defaultId:   "modalWithContent",
    componentName: "Modal",
    blocks:      [],
    props: {
      title:     "모달 제목",
      children:  "모달 내용 입니다.",
      bottomBtnCnt: "0",
      bottomBtn1Label: "확인",
      bottomBtn2Label: "취소"
    },
    propSchema: {
      title:           { type: "string",  label: "제목",          default: "모달 제목" },
      children:        { type: "string",  label: "모달 내용",      default: "모달 내용 입니다." },
      bottomBtnCnt:    { type: "select",  label: "하단 버튼 수",    options: ["0", "1", "2"],  default: "0" },
      bottomBtn1Label: { type: "string",  label: "버튼1 텍스트",    default: "확인" },
      bottomBtn2Label: { type: "string",  label: "버튼2 텍스트",    default: "취소" },
    },
    renderer: ModalRenderer,
  },
  
  // ── 이용내역 검색 필터 ────────────────────────────────────
  {
    id:            "tpl_usage_history_filter",
    label:         "이용내역 검색 필터",
    description:   "카드 이용내역 검색 조건(승인구분·카드구분·기간 등)을 편집하는 필터 시트",
    type:          "UsageHistoryFilterSheet",
    defaultId:   "usageHistoryFilterSheet",
    componentName: "UsageHistoryFilterSheet",
    blocks:        [],
    props:         {},
    propSchema:    {},
    renderer:      UsageHistoryFilterSheetRenderer,
  },

  // ── PIN 입력 ──────────────────────────────────────────────
  {
    id:            "tpl_pin_confirm",
    label:         "PIN 입력",
    description:   "즉시결제·계좌 비밀번호 등 PIN 확인이 필요한 화면에서 사용하는 하단 시트",
    type:          "PinConfirmSheet",
    defaultId:   "pinConfirmSheet",
    componentName: "PinConfirmSheet",
    blocks:        [],
    props: {
      title:     "비밀번호 입력",
      pinLength: 4,
    },
    propSchema: {
      title:     { type: "string", label: "타이틀",    default: "비밀번호 입력" },
      pinLength: { type: "select", label: "PIN 자릿수", options: ["4", "6"],         default: 4 },
    },
    renderer: PinConfirmSheetRenderer,
  },

  // ── 슬라이드 오버 ─────────────────────────────────────────
  {
    id:            "tpl_modal_slide_over",
    label:         "슬라이드 오버",
    description:   "라우트 기반 모달 패턴에서 사용하는 슬라이드 오버 패널",
    type:          "ModalSlideOver",
    defaultId:   "modalSlideOver",
    componentName: "ModalSlideOver",
    blocks:        [],
    props: {
      direction: "right",
    },
    propSchema: {
      direction: { type: "select", label: "슬라이드 방향", options: ["right", "bottom"], default: "right" },
    },
    renderer: ModalSlideOverRenderer,
  },
];
