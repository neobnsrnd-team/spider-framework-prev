/**
 * @file EmergencyNoticeBanner.tsx
 * @description 긴급공지 팝업 모달 컴포넌트
 *
 * Admin에서 배포된 긴급공지를 화면 중앙 모달로 표시한다.
 * 언어 설정에 따라 한국어(EMERGENCY_KO) 또는 영어(EMERGENCY_EN) 공지를 표시하며,
 * 브라우저 언어(navigator.language)가 'en'으로 시작하면 영어를, 그 외에는 한국어를 기본으로 표시한다.
 *
 * 줄바꿈 마커(_$BR) 처리:
 *   DB에 저장된 _$BR 마커를 줄바꿈으로 변환하여 렌더링한다.
 *
 * 오늘 하루 보지 않기:
 *   localStorage에 당일 날짜 키를 저장하여 페이지 재방문 시 재표시를 억제한다.
 *   날짜가 바뀌면 키가 달라져 자동으로 무효화된다.
 *
 * @param {NoticePayload} data      - SSE로 수신한 공지 데이터
 * @param {() => void}   [onClose] - 모달 닫기 후 추가 처리가 필요할 때 사용하는 콜백 (선택)
 */

import { useState, useEffect }  from "react";
import { Building2, Clock }     from "lucide-react";
import { Modal }                from "@cl/modules/common/Modal";
import { Button }               from "@cl/core/Button";
import { Checkbox }             from "@cl/modules/common/Checkbox";
import type { NoticePayload, NoticeItem } from "@/hooks/useEmergencyNotice";

interface EmergencyNoticeBannerProps {
  data: NoticePayload;
  onClose?: () => void;
  /** 미리보기 모드에서 localStorage 오늘 하루 보지 않기 설정을 무시하고 강제 표시한다. */
  forceOpen?: boolean;
  /** 표시할 언어 코드 (예: 'EMERGENCY_KO' | 'EMERGENCY_EN'). 생략 시 브라우저 언어를 기반으로 자동 결정한다. */
  lang?: string;
}

/**
 * 오늘 날짜 기반 localStorage 키를 반환한다.
 * YYYY-MM-DD 형식이므로 날짜가 바뀌면 키가 달라져 자동 무효화된다.
 */
function getTodayHideKey(): string {
  // toLocaleDateString('en-CA')는 로컬 시간대 기준 YYYY-MM-DD를 반환한다.
  // toISOString()은 UTC 기준이므로 한국 시간 자정 전후로 날짜가 어긋날 수 있어 사용하지 않는다.
  const today = new Date().toLocaleDateString("en-CA");
  return `emergency-notice-hidden-${today}`;
}

/**
 * 언어 코드에 해당하는 공지 항목을 찾아 반환한다.
 * 해당 언어 공지가 없으면 첫 번째 항목을 fallback으로 사용한다.
 *
 * @param {NoticeItem[]} notices
 * @param {string}       lang    - 'EMERGENCY_KO' | 'EMERGENCY_EN'
 */
function findNoticeByLang(notices: NoticeItem[], lang: string): NoticeItem | undefined {
  return notices.find((n) => n.lang === lang) ?? notices[0];
}

/** _$BR 마커를 \n 으로 변환한다 (ASIS 호환). */
function parseContent(content: string): string {
  return content.replace(/_\$BR/g, "\n");
}

export function EmergencyNoticeBanner({ data, onClose, forceOpen = false, lang = (typeof window !== 'undefined' && navigator.language.startsWith("en")) ? "EMERGENCY_EN" : "EMERGENCY_KO" }: EmergencyNoticeBannerProps) {
  const [hideToday, setHideToday] = useState(false);

  /* 닫기 버튼 허용 여부 — N이면 사용자가 모달을 닫을 수 없음 (critical 장애 시) */
  const isCloseable  = data.closeableYn  !== "N";
  /* 오늘 하루 보지 않기 체크박스 표시 여부 — 닫기 불가이면 체크박스도 의미 없으므로 숨김 */
  const showHideToday = isCloseable && data.hideTodayYn !== "N";

  /* 초기 표시 여부: forceOpen이면 무조건 열기, displayType이 N이거나 오늘 하루 보지 않기가 설정된 경우 닫힌 상태로 시작 */
  const [open, setOpen] = useState(() => {
    if (forceOpen) return true;
    if (data.displayType === "N") return false;
    return !localStorage.getItem(getTodayHideKey());
  });

  /* 배포 종료(displayType → N) 시 모달 자동 닫기 — forceOpen이면 무시 */
  useEffect(() => {
    if (!forceOpen && data.displayType === "N") setOpen(false);
  }, [data.displayType, forceOpen]);

  /*
   * Admin이 설정 변경 또는 재배포 시 모달 재표시.
   * data 레퍼런스는 SSE 이벤트가 올 때만 변경되므로 불필요한 재실행 없음.
   *
   * - forceOpen=true (미리보기): localStorage 무관하게 항상 열기
   * - closeableYn=N (강제 노출): localStorage 무관하게 항상 열기
   *   → 이미 닫은 사용자도 critical 장애 공지를 다시 보게 됨
   * - closeableYn=Y (일반): 오늘 하루 보지 않기 미설정 시에만 열기
   *   → 사용자가 오늘 닫기로 선택한 경우 재오픈하지 않음
   */
  useEffect(() => {
    if (data.displayType === "N" && !forceOpen) return;
    if (forceOpen || data.closeableYn === "N") {
      setOpen(true);
      return;
    }
    if (!localStorage.getItem(getTodayHideKey())) {
      setOpen(true);
    }
  }, [data, forceOpen]);

  const notice = findNoticeByLang(data.notices, lang);

  /* 표시할 내용이 없으면 DOM에 마운트하지 않음 */
  if (!notice || (!notice.title && !notice.content)) return null;

  const contentLines = parseContent(notice.content || "").split("\n");

  /* X 버튼: hideToday가 체크된 경우 localStorage에 저장 후 닫기
   * 확인 버튼과 동일한 저장 동작을 수행하여 사용자가 X를 눌러도 설정이 유실되지 않는다. */
  const handleClose = () => {
    if (hideToday) {
      localStorage.setItem(getTodayHideKey(), "1");
    }
    setOpen(false);
    onClose?.();
  };

  /* 확인 버튼: hideToday 체크 시 localStorage에 저장 후 닫기 */
  const handleConfirm = () => {
    if (hideToday) {
      localStorage.setItem(getTodayHideKey(), "1");
    }
    setOpen(false);
    onClose?.();
  };

  return (
    <Modal
      open={open}
      onClose={handleClose}
      /* 배경 클릭으로는 닫히지 않도록: 긴급공지는 사용자가 명시적으로 확인해야 함 */
      disableBackdropClose
      /* closeable=false이면 X 버튼 숨김 + ESC 비활성화 */
      closeable={isCloseable}
      /* Figma 디자인 기준 24px 라운드 (Modal 기본 rounded-2xl=16px 보다 큰 값) */
      className="rounded-[24px]"
    >
      <div className="flex flex-col items-center gap-lg text-center pb-md">

        {/* ── 공지 유형 아이콘 ─────────────────────────────────────────────
            은행 빌딩 아이콘을 원형 배경 위에 표시하고,
            우측 하단에 시계 뱃지를 올려 '시간 제한 안내'임을 시각화한다. */}
        <div className="relative mt-sm">
          <div className="flex items-center justify-center size-[72px] rounded-full bg-brand-5">
            <Building2 className="size-8 text-brand" aria-hidden="true" />
          </div>
          {/* 시계 뱃지 — aria-hidden: 장식 요소이므로 스크린리더에서 무시 */}
          <span
            aria-hidden="true"
            className="absolute -bottom-1 -right-1 flex items-center justify-center size-6 rounded-full bg-[var(--color-status-warning,#f59e0b)]"
          >
            <Clock className="size-3.5 text-white" />
          </span>
        </div>

        {/* ── 공지 제목 ──────────────────────────────────────────────────── */}
        {notice.title && (
          <h2 className="text-2xl font-bold text-text-heading leading-tight">
            {notice.title}
          </h2>
        )}

        {/* ── 공지 본문 ──────────────────────────────────────────────────── */}
        {notice.content && (
          <p className="text-sm text-text-secondary leading-relaxed">
            {contentLines.map((line, i) => (
              <span key={i}>
                {line}
                {i < contentLines.length - 1 && <br />}
              </span>
            ))}
          </p>
        )}

        {/* ── 확인 버튼 — closeable=N(강제 노출)이면 숨김 ─────────────── */}
        {isCloseable && (
          <Button
            variant="primary"
            size="lg"
            fullWidth
            onClick={handleConfirm}
          >
            확인
          </Button>
        )}

        {/* ── 오늘 하루 보지 않기 — showHideToday=false이면 숨김 ──────── */}
        {showHideToday && (
          <Checkbox
            id="emergency-hide-today"
            checked={hideToday}
            onChange={setHideToday}
            label="오늘 하루 보지 않기"
          />
        )}
      </div>
    </Modal>
  );
}
