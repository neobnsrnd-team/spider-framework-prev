/**
 * @file useEmergencyNotice.ts
 * @description 긴급공지 SSE 구독 커스텀 훅
 *
 * Demo Backend의 GET /api/notices/sse 엔드포인트에 연결하여
 * 긴급공지 변경 이벤트를 실시간으로 수신한다.
 *
 * 동작:
 *   - 컴포넌트 마운트 시 SSE 연결을 시작한다.
 *   - Admin이 배포하면 'notice' 이벤트로 공지 데이터를 수신한다.
 *   - Admin이 배포 종료하면 'notice-end' 이벤트(빈 data)를 수신한다.
 *   - 컴포넌트 언마운트 시 SSE 연결을 종료한다.
 *
 * @returns {{ notice: NoticePayload | null }} 현재 공지 데이터 (null이면 공지 없음)
 *
 * @example
 *   const { notice } = useEmergencyNotice();
 *   if (notice) return <EmergencyNoticeBanner data={notice} />;
 */

import { useEffect, useState } from "react";

/** 단일 언어 공지 항목 */
export interface NoticeItem {
  /** PROPERTY_ID — 'EMERGENCY_KO' | 'EMERGENCY_EN' */
  lang: string;
  /** 공지 제목 */
  title: string;
  /** 공지 내용 (_$BR 마커 포함 가능) */
  content: string;
}

/** SSE 'notice' 이벤트 페이로드 */
export interface NoticePayload {
  notices: NoticeItem[];
  /** 노출 타입: A(전체) | B(기업) | C(개인) | N(사용안함) */
  displayType: string;
  /**
   * 닫기 버튼 노출 여부.
   * N이면 모달을 닫을 수 없어 화면 접근이 차단된다 (critical 장애 시 사용).
   */
  closeableYn: string;
  /** 오늘 하루 보지 않기 체크박스 노출 여부. N이면 체크박스를 숨긴다. */
  hideTodayYn: string;
}

/** SSE 연결 URL — vite proxy를 통해 Demo Backend로 전달 */
const SSE_URL = "/api/notices/sse";

export function useEmergencyNotice(): { notice: NoticePayload | null } {
  const [notice, setNotice] = useState<NoticePayload | null>(null);

  // 마운트 시 현재 공지 상태를 즉시 조회한다.
  // SSE 연결과 별개로 페이지 진입 시점의 배포 상태를 반영하여
  // SSE 초기 이벤트 미수신으로 공지가 표시되지 않는 것을 방지한다.
  useEffect(() => {
    let cancelled = false;
    fetch("/api/notices/preview")
      .then((r) => r.json() as Promise<NoticePayload>)
      .then((data) => {
        if (!cancelled && data?.notices?.length > 0 && data.displayType !== "N") {
          setNotice(data);
        }
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const es = new EventSource(SSE_URL);

    es.addEventListener("notice", (event) => {
      try {
        const data = JSON.parse(event.data) as NoticePayload | null;
        setNotice(data);
      } catch {
        // JSON 파싱 실패 시 무시 (연결 초기 ping 등)
      }
    });

    // 배포 종료 시 서버가 "notice-end" 이벤트를 전송한다 (빈 data)
    es.addEventListener("notice-end", () => {
      setNotice(null);
    });

    es.onerror = () => {
      // 네트워크 오류 시 브라우저가 자동 재연결하므로 별도 처리 불필요
      console.warn("[useEmergencyNotice] SSE 연결 오류 — 재연결 대기 중");
    };

    return () => {
      // 컴포넌트 언마운트 시 연결 종료
      es.close();
    };
  }, []);

  return { notice };
}
