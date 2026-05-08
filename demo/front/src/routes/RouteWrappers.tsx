/**
 * @file RouteWrappers.tsx
 * @description 각 페이지 컴포넌트에 navigate 핸들러를 주입하는 Route Wrapper 컴포넌트 모음.
 *
 * 역할:
 * - Page 컴포넌트는 onXxx props 만 받고, 실제 navigate 호출은 이 파일에서 처리한다.
 * - Mock 데이터를 주입하여 API 연동 전 개발·데모 환경을 지원한다.
 * - 각 Wrapper는 routes/index.tsx 에서 pageRoutes / modalRoutes 에 등록된다.
 *
 * 네이밍 규칙:
 * - 일반 페이지: XxxRoute (예: LoginRoute)
 * - 모달 오버레이: XxxModal (예: HanaCardMenuModal)
 */
import {
  useState,
  useEffect,
  useRef,
  useMemo,
  type ComponentType,
} from "react";
import {
  useNavigate,
  useLocation,
  useSearchParams,
  useParams,
} from "react-router-dom";
import type { NoticePayload } from "@/hooks/useEmergencyNotice";
import { useAuth } from "@/contexts/AuthContext";
import { axiosInstance } from "@/api/axiosInstance";
import { maskAccountNumber } from "@/utils/format";
import {
  Landmark,
  Building,
  Receipt,
  FileText,
  Wallet,
  Settings,
  Gift,
  CreditCard,
  Headphones,
} from "lucide-react";

import { LoginPage } from "@/pages/common/LoginPage";
import { CardDashboardPage } from "@/pages/card/CardDashboardPage";
import { EmergencyNoticeBanner } from "@/components/EmergencyNoticeBanner";
import { useEmergencyNotice } from "@/hooks/useEmergencyNotice";
import { HanaCardMenuPage } from "@/pages/card/HanaCardMenuPage";
import { UsageHistoryPage } from "@/pages/card/UsageHistoryPage";
import { PaymentStatementPage } from "@/pages/card/PaymentStatementPage";
import { ImmediatePaymentPage } from "@/pages/card/ImmediatePaymentPage";
import { ImmediatePayPage } from "@/pages/card/ImmediatePayPage";
import type { CardInfo } from "@/pages/card/ImmediatePayPage/types";
import { ImmediatePayRequestPage } from "@/pages/card/ImmediatePayRequestPage";
import { ImmediatePayMethodPage } from "@/pages/card/ImmediatePayMethodPage";
import { ImmediatePayConfirmSheet } from "@/pages/card/ImmediatePayMethodPage/ImmediatePayConfirmSheet";
import { ImmediatePayCompletePage } from "@/pages/card/ImmediatePayCompletePage";
import { MyCardManagementPage } from "@/pages/card/MyCardManagementPage";
import { UserManagementPage } from "@/pages/card/UserManagementPage";
import { PinConfirmSheet } from "@cl/modules/common/PinConfirmSheet";
import { BottomSheet } from "@cl/modules/common/BottomSheet";
import { Modal } from "@cl/modules/common/Modal";
import { CardInfoPanel } from "@cl/biz/card/CardInfoPanel";
import { ModalSlideOver } from "@cl/layout/ModalSlideOver";

import type { MenuItem } from "@/pages/card/HanaCardMenuPage/types";
import type { CardItem } from "@/pages/card/MyCardManagementPage/types";
import type {
  Transaction,
  SearchFilter,
} from "@/pages/card/UsageHistoryPage/types";
import type {
  PaymentTabData,
  StatementTabData,
  CardPaymentEntry,
} from "@/pages/card/PaymentStatementPage/types";
import { PATHS } from "@/constants/paths";
import {
  MOCK_USERS,
  MOCK_MANAGEMENT_ROWS,
  MOCK_CAUTIONS,
} from "@/mocks/cardMocks";

/* ------------------------------------------------------------------ */
/* 공통 / 인증                                                           */
/* ------------------------------------------------------------------ */

/** localStorage 키 상수 */
const LS_SAVED_ID = "hnc_saved_id";

export function LoginRoute() {
  const navigate = useNavigate();
  const { login } = useAuth();

  // 이전 방문에서 저장된 값으로 초기화
  const [saveId, setSaveId] = useState(
    () => !!localStorage.getItem(LS_SAVED_ID),
  );

  // 아이디 저장이 켜져 있으면 저장된 아이디로 초기화, 아니면 빈 문자열
  const [userId, setUserId] = useState(
    () => localStorage.getItem(LS_SAVED_ID) ?? "",
  );
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [hasError, setHasError] = useState(false);
  // 세션 만료 시 AuthContext·useSessionActivity가 sessionStorage에 저장한 메시지를 읽어 Modal로 표시
  const [sessionMessage, setSessionMessage] = useState(
    () => sessionStorage.getItem("sessionExpiredMessage") ?? "",
  );
  const { notice } = useEmergencyNotice(); // 로그인 전에도 긴급공지 표시

  const handleLogin = async () => {
    setHasError(false);
    try {
      // withCredentials: true 이므로 백엔드가 Set-Cookie로 httpOnly Refresh Token을 심는다
      const { data } = await axiosInstance.post("/auth/login", {
        userId,
        password,
      });
      if (data.success) {
        // 아이디 저장: 체크 시 userId를 localStorage에 보관, 해제 시 삭제
        if (saveId) {
          localStorage.setItem(LS_SAVED_ID, userId);
        } else {
          localStorage.removeItem(LS_SAVED_ID);
        }
        login({
          userId: data.userId,
          userName: data.userName,
          userGrade: data.userGrade,
          token: data.token,
          lastLogin: data.lastLogin ?? "", // 최초 로그인 시 LAST_LOGIN_DTIME이 null이면 빈 문자열
        });
        navigate(PATHS.CARD.DASHBOARD);
      } else {
        setHasError(true);
      }
    } catch {
      setHasError(true);
    }
  };

  return (
    <>
      <LoginPage
        userId={userId}
        password={password}
        onUserIdChange={setUserId}
        onPasswordChange={setPassword}
        hasError={hasError}
        showPassword={showPassword}
        onTogglePassword={() => setShowPassword((v) => !v)}
        onLogin={handleLogin}
        saveId={saveId}
        onSaveIdChange={setSaveId}
      />
      {notice && <EmergencyNoticeBanner data={notice} />}
      <Modal
        open={!!sessionMessage}
        onClose={() => {
          sessionStorage.removeItem("sessionExpiredMessage");
          setSessionMessage("");
        }}
        title="세션 만료"
        size="sm"
        titleAlign="center"
        className="text-sm text-center"
      >
        {sessionMessage}
      </Modal>
    </>
  );
}

/* ------------------------------------------------------------------ */
/* 카드 대시보드                                                         */
/* ------------------------------------------------------------------ */

/**
 * "YYYY.MM.DD" 또는 "YYMMDD" / "YYYYMMDD" → "M월 D일" 형식 변환.
 * billingPeriod.dueDate(YYYY.MM.DD) 또는 dueDate(YYMMDD) 모두 처리한다.
 */
function parseDueDateKo(raw: string): string {
  if (!raw) return "";
  // YYYY.MM.DD
  const dots = raw.split(".");
  if (dots.length === 3 && dots[0].length === 4) {
    return `${Number(dots[1])}월 ${Number(dots[2])}일`;
  }
  // YYMMDD (결제예정일 DB 형식)
  if (raw.length === 6)
    return `${Number(raw.slice(2, 4))}월 ${Number(raw.slice(4, 6))}일`;
  // YYYYMMDD
  if (raw.length === 8)
    return `${Number(raw.slice(4, 6))}월 ${Number(raw.slice(6, 8))}일`;
  return "";
}

/** 햄버거 메뉴 클릭 시 background location 패턴으로 /card/menu 를 모달로 열기 */
export function CardDashboardRoute() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, setLastLogin } = useAuth();
  const { notice } = useEmergencyNotice(); // SSE 긴급공지 구독

  /** StatementHeroCard: 공여기간 기준 이번 달 결제 예정 금액 */
  const [statementAmount, setStatementAmount] = useState(0);
  /** StatementHeroCard: 결제일 레이블 (예: "1월 25일") */
  const [statementDueDate, setStatementDueDate] = useState("");
  /** SummaryCard(spending): 당월 이용내역 합산 금액 */
  const [spendingAmount, setSpendingAmount] = useState(0);

  // lastLogin이 없는 경우(변경 전 로그인 세션 등) /api/auth/me로 보완한다.
  // Access Token이 유효한 동안은 refresh가 호출되지 않으므로 대시보드 진입 시점에 한 번 확인한다.
  useEffect(() => {
    if (!user?.userId || user.lastLogin) return;
    axiosInstance
      .get<{ lastLogin: string }>("/auth/me")
      .then(({ data }) => {
        if (data.lastLogin) setLastLogin(data.lastLogin);
      })
      .catch(() => {}); // 실패해도 화면 진입은 허용
  }, [user?.userId, user?.lastLogin, setLastLogin]);

  // 대시보드 진입 시 이전 즉시결제 플로우의 세션 데이터를 일괄 삭제한다.
  // 완료 화면에서 삭제하지 않는 이유: 완료 화면을 건너뛰고 직접 대시보드로 오는 경우에도
  // 잔여 세션이 남지 않도록 대시보드 진입 시점을 기준으로 정리한다.
  useEffect(() => {
    [
      "immediatePaySelectedCard",
      "immediatePayAmountInfo",
      "immediatePayRequestData",
      "immediatePaySelectedAccount",
      "immediatePayCompletedAt",
      "immediatePayError",
    ].forEach((key) => sessionStorage.removeItem(key));
  }, []); // 대시보드 마운트 시 한 번만 실행

  useEffect(() => {
    if (!user?.userId) return;

    const controller = new AbortController();

    /* 당월 범위 계산 (YYYYMMDD 형식) */
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const lastDay = new Date(year, now.getMonth() + 1, 0).getDate();
    const fromDate = `${year}${month}01`;
    const toDate = `${year}${month}${String(lastDay).padStart(2, "0")}`;

    Promise.all([
      axiosInstance
        .get("/payment-statement", { signal: controller.signal })
        .then((r) => r.data),
      axiosInstance
        .get(`/transactions?fromDate=${fromDate}&toDate=${toDate}`, {
          signal: controller.signal,
        })
        .then((r) => r.data),
    ])
      .then(([stmtData, txData]) => {
        setStatementAmount(stmtData.totalAmount ?? 0);
        // billingPeriod.dueDate(YYYY.MM.DD) 우선 사용, 없으면 raw dueDate(YYMMDD) 파싱
        const rawDue =
          stmtData.billingPeriod?.dueDate ?? stmtData.dueDate ?? "";
        setStatementDueDate(parseDueDateKo(rawDue));
        setSpendingAmount(txData.paymentSummary?.totalAmount ?? 0);
      })
      .catch((err: { code?: string }) => {
        // AbortController 취소 시 axios가 ERR_CANCELED 코드를 반환하므로 무시
        if (err.code !== "ERR_CANCELED") console.error(err);
      });

    return () => controller.abort();
  }, [user?.userId]);

  return (
    <>
      {/* 배포 중인 긴급공지가 있을 때 화면 최상단에 배너 표시 */}
      {notice && <EmergencyNoticeBanner data={notice} />}
      <CardDashboardPage
        userName={user?.userName}
        statementAmount={statementAmount}
        statementDueDate={statementDueDate}
        spendingAmount={spendingAmount}
        onMenu={() =>
          navigate(PATHS.CARD.MENU, { state: { background: location } })
        }
        onNotification={() => {}}
        onStatementDetail={() => navigate(PATHS.CARD.PAYMENT_STATEMENT)}
        onShortLoan={() => {}}
        onLongLoan={() => {}}
        onRevolving={() => {}}
        onCardPerformance={() => {}}
        onUsageHistory={() => navigate(PATHS.CARD.USAGE_HISTORY)}
        onMyCards={() => navigate(PATHS.CARD.MY_CARD_MANAGEMENT)}
        onCoupons={() => {}}
        onLimitCheck={() => {}}
        onInstallment={() => {}}
        onCardApply={() => {}}
      />
    </>
  );
}

/* ------------------------------------------------------------------ */
/* 이용내역                                                              */
/* ------------------------------------------------------------------ */

/** 분할납부·즉시결제 클릭 시 즉시결제 안내로 이동 */
export function UsageHistoryRoute() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [totalCount, setTotalCount] = useState(0);
  const [paymentSummary, setPaymentSummary] = useState({
    date: "",
    totalAmount: 0,
  });
  const [cardOptions, setCardOptions] = useState<
    { value: string; label: string }[]
  >([]);
  const [loading, setLoading] = useState(true);

  /** SearchFilter → query string 변환 후 이용내역 재조회 */
  const fetchTransactions = (filter?: SearchFilter) => {
    const params = new URLSearchParams();
    if (filter) {
      if (filter.selectedCard && filter.selectedCard !== "all")
        params.set("cardId", filter.selectedCard);
      if (filter.period) params.set("period", filter.period);
      if (filter.customMonth) params.set("customMonth", filter.customMonth);
      if (filter.usageType && filter.usageType !== "all")
        params.set("usageType", filter.usageType);
    }
    const qs = params.toString() ? `?${params}` : "";
    axiosInstance
      .get(`/transactions${qs}`)
      .then((r) => {
        const data = r.data;
        setTransactions(data.transactions ?? []);
        setTotalCount(Number(data.totalCount ?? 0));
        setPaymentSummary(data.paymentSummary ?? { date: "", totalAmount: 0 });
      })
      .catch(console.error);
  };

  useEffect(() => {
    if (!user?.userId) return;
    /* 이번 달 첫째 날 ~ 마지막 날을 YYYYMMDD 형식으로 계산 */
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, "0");
    const lastDay = new Date(year, now.getMonth() + 1, 0).getDate(); // 월의 실제 마지막 날
    const fromDate = `${year}${month}01`;
    const toDate = `${year}${month}${String(lastDay).padStart(2, "0")}`;

    Promise.all([
      axiosInstance
        .get(`/transactions?fromDate=${fromDate}&toDate=${toDate}`)
        .then((r) => r.data),
      axiosInstance.get("/cards").then((r) => r.data),
    ])
      .then(([txData, cardData]) => {
        setTransactions(txData.transactions ?? []);
        setTotalCount(Number(txData.totalCount ?? 0));
        setPaymentSummary(
          txData.paymentSummary ?? { date: "", totalAmount: 0 },
        );
        setCardOptions(
          (cardData.cards ?? []).map((c: { id: string; name: string }) => ({
            value: c.id,
            label: c.name,
          })),
        );
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [user?.userId]);

  if (loading) return null;

  return (
    <UsageHistoryPage
      transactions={transactions}
      totalCount={totalCount}
      paymentSummary={paymentSummary}
      cardOptions={cardOptions}
      onBack={() => navigate(-1)}
      onClose={() => navigate(PATHS.CARD.DASHBOARD, { replace: true })}
      onLoadMore={() => {}}
      onRevolving={() => {}}
      onSearch={fetchTransactions}
      onInstallment={() => navigate(PATHS.CARD.IMMEDIATE_PAYMENT)}
      onImmediatePayment={() => navigate(PATHS.CARD.IMMEDIATE_PAYMENT)}
    />
  );
}

/* ------------------------------------------------------------------ */
/* 결제예정금액 / 명세서                                                  */
/* ------------------------------------------------------------------ */

/** /api/payment-statement 응답 items 단건 타입 */
interface StmtApiItem {
  cardNo: string;
  cardName: string;
  amount: number;
  /** 고정길이 파서가 MESSAGE_FIELD_ID "itemDueDate" 로 반환 (헤더 dueDate 와 충돌 방지) */
  itemDueDate: string;
}
/** /api/payment-statement 응답 billingPeriod 타입 */
interface StmtBillingPeriod {
  usageStart: string;
  usageEnd: string;
  dueDate: string;
}
/** /api/payment-statement 전체 응답 타입 */
interface StmtApiResponse {
  dueDate: string;
  totalAmount: number;
  items: StmtApiItem[];
  cardInfo: {
    paymentBank: string;
    paymentAccount: string;
    paymentDay: string;
  } | null;
  billingPeriod: StmtBillingPeriod | null;
}

/** YYMMDD or YYYYMMDD → "M월 D일 결제" 레이블 */
function fmtDueDateLabel(raw: string | undefined): string {
  if (!raw) return "";
  if (raw.length === 6)
    return `${Number(raw.slice(2, 4))}월 ${Number(raw.slice(4, 6))}일 결제`;
  if (raw.length === 8)
    return `${Number(raw.slice(4, 6))}월 ${Number(raw.slice(6, 8))}일 결제`;
  return "";
}

/**
 * 결제일이 오늘 이후인지 판단해 '예정' 배지 표시 여부를 결정한다.
 *
 * 판단 우선순위:
 *   1. billingPeriod.dueDate(YYYY.MM.DD) — API가 계산한 정확한 결제예정일
 *   2. billingPeriod 없을 때 — yearMonth + paymentDay로 결제일 직접 구성
 */
function isPaymentUpcoming(
  billingPeriod: StmtBillingPeriod | null,
  yearMonth: string,
  paymentDay?: string | null,
): boolean {
  const today = new Date();
  today.setHours(0, 0, 0, 0); // 시간 무시, 날짜 단위 비교

  if (billingPeriod?.dueDate) {
    /* YYYY.MM.DD 형식 파싱 */
    const [y, m, d] = billingPeriod.dueDate.split(".").map(Number);
    return new Date(y, m - 1, d) >= today;
  }

  /* billingPeriod 없을 때: yearMonth + paymentDay로 결제일 구성해 비교 */
  const [y, m] = yearMonth.split("-").map(Number);
  const day = paymentDay ? Number(paymentDay) : 1; // paymentDay 없으면 1일로 보수적 처리
  return new Date(y, m - 1, day) >= today;
}

/** YYMMDD or YYYYMMDD → { dateFull, dateYM, dateMD } */
function parseDueDate(raw: string | undefined) {
  if (!raw) return { dateFull: "", dateYM: "", dateMD: "" };
  if (raw.length === 6) {
    const y = `20${raw.slice(0, 2)}`,
      m = raw.slice(2, 4),
      d = raw.slice(4, 6);
    return {
      dateFull: `${y}.${m}.${d}`,
      dateYM: `${raw.slice(0, 2)}년 ${Number(m)}월`,
      dateMD: `${m}.${d}`,
    };
  }
  if (raw.length === 8) {
    const y = raw.slice(0, 4),
      m = raw.slice(4, 6),
      d = raw.slice(6, 8);
    return {
      dateFull: `${y}.${m}.${d}`,
      dateYM: `${y.slice(2)}년 ${Number(m)}월`,
      dateMD: `${m}.${d}`,
    };
  }
  return { dateFull: "", dateYM: "", dateMD: "" };
}

/**
 * 오늘 날짜 기준으로 화면 진입 시 보여줄 초기 청구월을 결정한다.
 *
 *   1 ~ 14일 → 이번 달(M)
 *     이유: 주요 결제일(25일 등)이 아직 도래하지 않아 이번 달 결제 예정 금액이
 *           사용자에게 가장 유의미한 정보다.
 *
 *   15일 ~   → 다음 달(M+1)
 *     이유: 이번 달 결제일이 이미 지났거나 며칠 내 완료될 예정이므로
 *           다음 달 청구분을 미리 확인하는 흐름이 자연스럽다.
 *
 * 월 이동 시 new Date(year, month, 1) 생성자를 사용해
 * setMonth의 월말 오버플로우를 원천 차단한다.
 * (e.g. 1월 31일에 setMonth(month+1) → 3월 2일로 밀리는 오류 방지)
 */
function getInitialBillingMonth(): string {
  const today = new Date();
  const day = today.getDate();
  /* 1일로 고정한 뒤 월 이동 — setDate 후 setMonth 방식의 오버플로우 없음 */
  const base = new Date(today.getFullYear(), today.getMonth(), 1);
  if (day >= 15) {
    // 15일 이후: 다음 달로 이동
    base.setMonth(base.getMonth() + 1);
  }
  // 1~14일: base 그대로(이번 달)
  return `${base.getFullYear()}-${String(base.getMonth() + 1).padStart(2, "0")}`;
}

/** 즉시결제·분할납부 클릭 시 즉시결제 안내로 이동 */
export function PaymentStatementRoute() {
  const navigate = useNavigate();
  const { user } = useAuth();

  /** 전체 카드 이용내역 items */
  const [allItems, setAllItems] = useState<StmtApiItem[]>([]);
  /** /api/cards 에서 가져온 카드 상세 (결제은행·계좌·결제일 표시용) */
  const [rawCards, setRawCards] = useState<ApiCardFull[]>([]);
  /** /api/payment-statement 응답의 cardInfo — rawCards 없을 때 fallback */
  const [stmtCardInfo, setStmtCardInfo] =
    useState<StmtApiResponse["cardInfo"]>(null);
  /** API 응답의 공여기간 정보 — infoSections 표시에 사용 */
  const [billingPeriod, setBillingPeriod] = useState<StmtBillingPeriod | null>(
    null,
  );
  const [loading, setLoading] = useState(true);
  /**
   * 현재 적용된 날짜 필터 — 날짜 변경 시 재조회에 사용.
   * getInitialBillingMonth()로 오늘 날짜 기준 최적 청구월을 초기값으로 설정한다.
   * (1~14일: 이번 달 / 15일~: 다음 달)
   */
  const yearMonthRef = useRef(getInitialBillingMonth());

  /**
   * items 재조회.
   * yearMonth 전달 시: 결제예정일(YYMMDD) LIKE 필터 (공여기간 미적용)
   */
  function fetchItems(yearMonth = yearMonthRef.current) {
    yearMonthRef.current = yearMonth;
    const params = new URLSearchParams();
    if (yearMonth) params.set("yearMonth", yearMonth);
    const qs = params.toString() ? `?${params}` : "";
    axiosInstance
      .get<StmtApiResponse>(`/payment-statement${qs}`)
      .then((r) => {
        const data = r.data;
        setAllItems(data.items ?? []);
        setBillingPeriod(data.billingPeriod ?? null);
        setStmtCardInfo(data.cardInfo ?? null);
      })
      .catch(console.error);
  }

  useEffect(() => {
    if (!user?.userId) return;
    /* 초기 조회: getInitialBillingMonth() 기준 청구월 */
    const initQs = `?yearMonth=${yearMonthRef.current}`;
    Promise.all([
      axiosInstance
        .get<StmtApiResponse>(`/payment-statement${initQs}`)
        .then((r) => r.data),
      axiosInstance.get<{ cards: ApiCardFull[] }>("/cards").then((r) => r.data),
    ])
      .then(([stmtData, cardData]) => {
        setAllItems(stmtData.items ?? []);
        setBillingPeriod(stmtData.billingPeriod ?? null);
        setStmtCardInfo(stmtData.cardInfo ?? null);
        setRawCards(cardData.cards ?? []);
      })
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [user?.userId]);

  /**
   * 전체 카드 합산 데이터 계산.
   * - 백엔드가 카드별 공여기간 규칙을 적용해 선택 청구월 항목만 반환하므로
   *   프론트는 allItems를 그대로 두 탭에 공통 사용한다.
   * - infoSections: 첫 번째 카드의 결제정보 + 공여기간
   */
  const { paymentData, statementData } = useMemo<{
    paymentData: PaymentTabData;
    statementData: StatementTabData;
  }>(() => {
    /* 결제예정금액 탭 — 백엔드가 청구월 기준으로 필터링한 allItems 사용 */
    /* Number() 변환: 고정길이 파서 N 타입이 String으로 반환되므로 숫자 합산 보정 */
    const paymentTotalAmt = allItems.reduce((s, i) => s + Number(i.amount), 0);
    const firstDue = allItems[0]?.itemDueDate ?? "";
    const { dateFull, dateYM, dateMD } = parseDueDate(firstDue);
    const paymentItems: CardPaymentEntry[] = allItems.map((item) => ({
      id: `${item.cardNo}_${item.itemDueDate}`,
      icon: <CreditCard className="size-5" />,
      cardEnName: fmtDueDateLabel(item.itemDueDate),
      cardName: item.cardName,
      amount: Number(item.amount),
    }));

    /* 이용대금명세서 탭 — 동일 항목 재사용 */
    const statementTotalAmt = paymentTotalAmt;
    const allPaymentItems = paymentItems;

    /* 결제정보: stmtCardInfo(payment-statement API) 우선, 없으면 rawCards[0] fallback */
    const cardInfoSrc =
      stmtCardInfo ??
      (rawCards[0]
        ? {
            paymentBank: rawCards[0].paymentBank,
            paymentAccount: rawCards[0].paymentAccount,
            paymentDay: rawCards[0].paymentDay,
          }
        : null);
    const paymentInfoRows = cardInfoSrc
      ? [
          { label: "결제은행명", value: cardInfoSrc.paymentBank },
          {
            label: "결제계좌",
            value: maskAccountNumber(cardInfoSrc.paymentAccount),
          },
          { label: "결제일", value: `${cardInfoSrc.paymentDay}일` },
        ]
      : [];
    const billingRows = billingPeriod
      ? [
          { label: "이용 시작", value: billingPeriod.usageStart },
          { label: "이용 종료", value: billingPeriod.usageEnd },
        ]
      : [];
    const infoSections = cardInfoSrc
      ? [{ title: "결제정보", rows: [...paymentInfoRows, ...billingRows] }]
      : [];

    return {
      paymentData: {
        dateFull,
        dateYM,
        dateMD,
        totalAmount: paymentTotalAmt,
        revolving: 0,
        cardLoan: 0,
        cashAdvance: 0,
        infoSections,
        paymentItems: paymentItems,
      },
      statementData: {
        totalAmount: statementTotalAmt,
        badge: isPaymentUpcoming(
          billingPeriod,
          yearMonthRef.current,
          cardInfoSrc?.paymentDay,
        )
          ? "예정"
          : undefined,
        paymentItems: allPaymentItems,
        infoSections,
      },
    };
  }, [allItems, rawCards, stmtCardInfo, billingPeriod]);

  if (loading) return null;

  return (
    <PaymentStatementPage
      initialMonth={yearMonthRef.current}
      paymentData={paymentData}
      statementData={statementData}
      onBack={() => navigate(-1)}
      onClose={() => navigate(PATHS.CARD.DASHBOARD, { replace: true })}
      onDateClick={(yearMonth) => fetchItems(yearMonth)}
      onRevolving={() => {}}
      onCardLoan={() => {}}
      onCashAdvance={() => {}}
      onStatementDetail={() => navigate(PATHS.CARD.USAGE_HISTORY)}
      onInstallment={() => navigate(PATHS.CARD.IMMEDIATE_PAYMENT)}
      onImmediatePayment={() => navigate(PATHS.CARD.IMMEDIATE_PAYMENT)}
    />
  );
}

/* ------------------------------------------------------------------ */
/* 즉시결제 플로우  (안내 → STEP1 → STEP2 → STEP3 → 완료)              */
/* ------------------------------------------------------------------ */

/** 즉시결제(선결제) 안내 — 즉시결제·건별즉시결제 클릭 시 STEP 1 으로 이동 */
export function ImmediatePaymentRoute() {
  const navigate = useNavigate();
  return (
    <ImmediatePaymentPage
      hanaAccount={{
        title: "하나은행 결제계좌",
        hours: "365일 06:00~23:30",
        icon: <Landmark className="size-5" />,
      }}
      otherAccount={{
        title: "타행 결제계좌",
        hours: "365일 06:00~23:30",
        icon: <Building className="size-5" />,
      }}
      cautions={MOCK_CAUTIONS}
      onImmediatePayment={() => navigate(PATHS.CARD.IMMEDIATE_PAY)}
      onItemPayment={() => navigate(PATHS.CARD.IMMEDIATE_PAY)}
      onAutoPayment={() => {}}
      onBack={() => navigate(-1)}
      onClose={() => navigate(PATHS.CARD.DASHBOARD, { replace: true })}
    />
  );
}

/** STEP 1 — 카드·결제유형 선택, 다음 클릭 시 STEP 2 로 이동 */
export function ImmediatePayRoute() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [cards, setCards] = useState<CardInfo[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user?.userId) return;

    const controller = new AbortController();

    axiosInstance
      .get<{ cards: ApiCardFull[] }>("/cards", { signal: controller.signal })
      .then((r) => {
        // ApiCardFull → CardInfo (결제은행·계좌는 STEP 3 출금계좌 표시에 사용)
        setCards(
          r.data.cards.map((c) => ({
            id: c.id,
            name: c.name,
            maskedNumber: c.maskedNumber,
            paymentBank: c.paymentBank,
            paymentAccount: c.paymentAccount,
          })),
        );
        setLoading(false);
      })
      .catch((err: { code?: string }) => {
        // abort된 경우 loading을 유지 — StrictMode 2차 실행에서 정상 완료 후 false로 전환
        if (err.code !== "ERR_CANCELED") {
          console.error(err);
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [user?.userId]);

  if (loading) return null;

  return (
    <ImmediatePayPage
      cards={cards}
      initialCardId={cards[0]?.id}
      initialPaymentType="total"
      onPaymentTypeChange={() => {}}
      onCardChange={() => {}}
      onBack={() => navigate(-1)}
      onClose={() => navigate(PATHS.CARD.DASHBOARD, { replace: true })}
      onNext={(cardId) => {
        // STEP 1에서 선택한 카드를 sessionStorage에 저장해 STEP 2로 전달
        const card = cards.find((c) => c.id === cardId);
        if (card)
          sessionStorage.setItem(
            "immediatePaySelectedCard",
            JSON.stringify(card),
          );
        navigate(PATHS.CARD.IMMEDIATE_PAY_REQUEST);
      }}
    />
  );
}

/** STEP 2 — 결제금액 확인, 다음 클릭 시 STEP 3 으로 이동 */
export function ImmediatePayRequestRoute() {
  const navigate = useNavigate();

  // STEP 1에서 sessionStorage에 저장된 카드 정보를 읽는다.
  const storedCard = sessionStorage.getItem("immediatePaySelectedCard");
  const card: CardInfo = storedCard
    ? (JSON.parse(storedCard) as CardInfo)
    : {
        id: "",
        name: "",
        maskedNumber: "",
        paymentBank: "",
        paymentAccount: "",
      };

  // POC_카드사용내역에서 누적결제금액 < 이용금액인 건의 미결제 잔액 합산을 조회한다.
  const [payableAmount, setPayableAmount] = useState<number>(0);
  useEffect(() => {
    if (!card.id) return;

    const controller = new AbortController();

    axiosInstance
      .get<{ payableAmount: number; creditLimit: number }>(
        `/cards/${card.id}/payable-amount`,
        { signal: controller.signal },
      )
      .then((r) => {
        setPayableAmount(Number(r.data.payableAmount));
        // STEP 3 확인 시트에서 결제 후 이용가능한도 계산에 필요한 값을 세션에 저장한다.
        sessionStorage.setItem(
          "immediatePayAmountInfo",
          JSON.stringify({
            payableAmount: Number(r.data.payableAmount),
            creditLimit: Number(r.data.creditLimit),
          }),
        );
      })
      .catch((err: { code?: string }) => {
        if (err.code !== "ERR_CANCELED")
          console.error(
            "[ImmediatePayRequestRoute] 결제가능금액 조회 실패",
            err,
          );
      });

    return () => controller.abort();
  }, [card.id]);

  // [검증 4] 카드 정보 없음 — 직접 URL 접근·새로고침 등으로 세션이 비어있는 경우
  if (!card.id) {
    return (
      <div className="flex flex-col items-center justify-center h-full gap-md px-standard text-center">
        <p className="text-sm text-muted">
          카드 정보가 없습니다.
          <br />
          이전 단계에서 카드를 선택해 주세요.
        </p>
        <button
          className="text-sm text-brand underline"
          onClick={() => navigate(-1)}
        >
          이전으로 돌아가기
        </button>
      </div>
    );
  }

  return (
    <ImmediatePayRequestPage
      card={card}
      payableAmount={payableAmount}
      amountHelperText="금액 입금 시 당사의 입금공제 순서에 따라 처리되며, 특정 건만 처리 할 수 없습니다."
      cautions={[
        {
          title: "결제 제한 안내",
          content:
            "결제일 당일 출금 가능 잔액이 부족할 경우 즉시결제가 취소될 수 있습니다.",
        },
        {
          title: "취소 불가 안내",
          content: "즉시결제 신청 후에는 취소가 불가합니다.",
        },
      ]}
      onChangeCard={() => navigate(-1)}
      onNext={(usageType, payAmount) => {
        // STEP 3에서 신청정보 확인에 표시할 데이터를 세션에 저장한다.
        sessionStorage.setItem(
          "immediatePayRequestData",
          JSON.stringify({ usageType, payAmount }),
        );
        navigate(PATHS.CARD.IMMEDIATE_PAY_METHOD);
      }}
      onBack={() => navigate(-1)}
      onClose={() => navigate(PATHS.CARD.DASHBOARD, { replace: true })}
    />
  );
}

/** STEP 3 — 출금계좌 선택, 신청 클릭 시 확인 시트 → PIN 입력 시트 → 완료 시 STEP 4 로 이동 */
export function ImmediatePayMethodRoute() {
  const navigate = useNavigate();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [pinOpen, setPinOpen] = useState(false);
  // 신청 클릭 시 선택된 계좌를 확인 시트에 전달하기 위해 저장한다.
  const [selectedAccount, setSelectedAccount] = useState({
    bankName: "",
    maskedAccount: "",
  });
  // PIN 오류 메시지 — PinConfirmSheet에 전달해 도트 아래에 표시한다.
  const [pinError, setPinError] = useState<string | undefined>();
  // PIN 횟수 초과 여부 — true일 때만 PinConfirmSheet에 초기화 버튼을 표시한다.
  const [pinExceeded, setPinExceeded] = useState(false);
  // (payErrorMessage 상태 제거 — 에러 시 완료 화면으로 이동해 세션에서 읽는 방식으로 변경)

  // STEP 1·2에서 세션에 저장된 카드 정보·결제 요청 데이터·금액 정보를 읽는다.
  const storedCard = sessionStorage.getItem("immediatePaySelectedCard");
  const storedRequest = sessionStorage.getItem("immediatePayRequestData");
  const storedAmountInfo = sessionStorage.getItem("immediatePayAmountInfo");
  const card: CardInfo = storedCard
    ? (JSON.parse(storedCard) as CardInfo)
    : {
        id: "",
        name: "",
        maskedNumber: "",
        paymentBank: "",
        paymentAccount: "",
      };
  const { usageType, payAmount } = storedRequest
    ? (JSON.parse(storedRequest) as { usageType: string; payAmount: number })
    : { usageType: "lump", payAmount: 0 };
  const { payableAmount: totalPayable, creditLimit } = storedAmountInfo
    ? (() => {
        const parsed = JSON.parse(storedAmountInfo) as { payableAmount: unknown; creditLimit: unknown };
        return { payableAmount: Number(parsed.payableAmount), creditLimit: Number(parsed.creditLimit) };
      })()
    : { payableAmount: 0, creditLimit: 0 };

  // 이용구분 코드 → 표시 문자열
  const usageTypeLabel = usageType === "lump" ? "일시불" : "금액별";

  // 결제 후 이용가능한도 = 한도금액 - (미결제금액 - 결제금액)
  const availableLimit = creditLimit - (totalPayable - payAmount);

  // STEP 1에서 세션에 저장된 card 객체에 결제은행·계좌가 포함되어 있으므로
  // 별도 API 호출 없이 세션 데이터를 직접 사용한다.
  // 계좌는 카드에 1:1로 연결되어 있으므로 단일 항목 배열로 제공한다.
  const ACCOUNTS = [
    {
      id: card.id,
      bankName: card.paymentBank,
      maskedAccount: maskAccountNumber(card.paymentAccount),
    },
  ];

  return (
    <>
      <ImmediatePayMethodPage
        summaryItems={[
          {
            label: "청구단위",
            value: (
              <span className="text-sm font-bold text-text-heading">
                {card.name}
                <br />
                {card.maskedNumber}
              </span>
            ),
          },
          { label: "이용구분", value: usageTypeLabel },
          {
            label: "결제금액",
            value: `${payAmount.toLocaleString("ko-KR")}원`,
          },
        ]}
        accounts={ACCOUNTS}
        initialAccountId={card.id}
        onApply={(accountId) => {
          // 선택된 계좌를 state와 세션에 저장 후 확인 시트를 표시한다.
          // 세션 저장: STEP 4 완료 화면에서 출금계좌를 표시하는 데 사용된다.
          const found = ACCOUNTS.find((a) => a.id === accountId);
          if (found) {
            setSelectedAccount(found);
            sessionStorage.setItem(
              "immediatePaySelectedAccount",
              JSON.stringify(found),
            );
          }
          setConfirmOpen(true);
        }}
        onBack={() => navigate(-1)}
        onClose={() => navigate(PATHS.CARD.DASHBOARD, { replace: true })}
        pinExceeded={pinExceeded}
        onResetPinAttempts={async () => {
          await axiosInstance.delete(`/cards/${card.id}/pin-attempts`);
          setPinExceeded(false);
        }}
      />
      {/* 즉시결제 확인 시트 — PIN 입력 전 최종 결제 내용 확인 */}
      <ImmediatePayConfirmSheet
        open={confirmOpen}
        payAmount={payAmount}
        cardName={card.name}
        cardNumber={card.maskedNumber}
        account={`${selectedAccount.bankName} ${selectedAccount.maskedAccount}`.trim()}
        availableLimit={availableLimit}
        onClose={() => setConfirmOpen(false)}
        onConfirm={() => {
          setConfirmOpen(false);
          setPinOpen(true);
        }}
      />
      <PinConfirmSheet
        open={pinOpen}
        errorMessage={pinError}
        onClose={() => {
          setPinOpen(false);
          setPinError(undefined);
          // pinExceeded는 여기서 초기화하지 않는다 —
          // 시트를 닫아도 초과 상태는 유지되어야 페이지의 초기화 버튼이 표시된다
        }}
        onConfirm={async (pin) => {
          // 횟수 초과 상태에서는 키패드를 잠그지 않고 검증 로직만 차단한다
          if (pinExceeded) return;
          try {
            const { data } = await axiosInstance.post<{
              paidAmount: number;
              processedCount: number;
              completedAt: string;
            }>(`/cards/${card.id}/immediate-pay`, {
              pin,
              amount: payAmount,
              // card 객체의 원본(비마스킹) 계좌번호를 그대로 전달한다
              accountNumber: card.paymentAccount,
            });
            // 서버 처리일시를 세션에 저장 → STEP 4 완료 화면에서 사용
            sessionStorage.setItem("immediatePayCompletedAt", data.completedAt);
            setPinOpen(false);
            navigate(PATHS.CARD.IMMEDIATE_PAY_COMPLETE, { replace: true });
          } catch (err: unknown) {
            const response = (
              err as {
                response?: {
                  status?: number;
                  data?: { error?: string; attemptsLeft?: number };
                };
              }
            )?.response;
            const status = response?.status;
            const data = response?.data;

            if (status === 403) {
              // 403은 항상 PIN 오류 — 완료 화면으로 이동하지 않고 시트 안에서 처리한다
              if (data?.attemptsLeft === 0) {
                setPinError(
                  "PIN 입력 횟수를 초과하였습니다. 초기화 후 다시 시도해 주세요.",
                );
                setPinExceeded(true);
              } else {
                const remaining = data?.attemptsLeft;
                setPinError(
                  remaining !== undefined
                    ? `PIN이 올바르지 않습니다. (${remaining}회 남음)`
                    : (data?.error ?? "PIN이 올바르지 않습니다."),
                );
              }
            } else {
              // PIN 검증 통과 후 결제 처리 중 발생한 오류는 완료 화면으로 이동해 표시한다.
              // 4xx(비즈니스 오류): 서버가 보낸 구체적인 사유를 그대로 노출한다.
              // 5xx(시스템 오류): 내부 오류를 사용자에게 노출하지 않고 일반 메시지를 사용한다.
              const isBusinessError =
                status !== undefined && status >= 400 && status < 500;
              const errorMessage = isBusinessError
                ? (data?.error ?? "결제 처리 중 오류가 발생했습니다.")
                : "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
              sessionStorage.setItem("immediatePayError", errorMessage);
              setPinOpen(false);
              navigate(PATHS.CARD.IMMEDIATE_PAY_COMPLETE, { replace: true });
            }
          }
        }}
      />
    </>
  );
}

/** STEP 4 — 완료 화면, 확인 클릭 시 대시보드로 이동 */
export function ImmediatePayCompleteRoute() {
  const navigate = useNavigate();

  // 이전 단계에서 세션에 저장된 카드·결제·계좌·금액·처리일시·에러 정보를 읽는다.
  // 세션 삭제는 대시보드 진입 시 수행한다 (CardDashboardRoute useEffect 참고).
  const payError = sessionStorage.getItem("immediatePayError") ?? undefined;
  const storedCard = sessionStorage.getItem("immediatePaySelectedCard");
  const storedRequest = sessionStorage.getItem("immediatePayRequestData");
  const storedAccount = sessionStorage.getItem("immediatePaySelectedAccount");
  const storedAmountInfo = sessionStorage.getItem("immediatePayAmountInfo");

  // 서버 반환 처리일시 사용. 세션 없음·"undefined" 문자열인 경우 클라이언트 현재 시각으로 대체
  const rawCompletedAt = sessionStorage.getItem("immediatePayCompletedAt");
  const storedCompletedAt =
    rawCompletedAt && rawCompletedAt !== "undefined"
      ? rawCompletedAt
      : new Date()
          .toLocaleString("ko-KR", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
            hour12: false,
          })
          .replace(/\. /g, ".")
          .replace(",", "");

  const card = storedCard
    ? (JSON.parse(storedCard) as { name: string; maskedNumber: string })
    : { name: "", maskedNumber: "" };
  const request = storedRequest
    ? (JSON.parse(storedRequest) as { payAmount: number })
    : { payAmount: 0 };
  const account = storedAccount
    ? (JSON.parse(storedAccount) as { bankName: string; maskedAccount: string })
    : { bankName: "", maskedAccount: "" };
  const amountInfo = storedAmountInfo
    ? (() => {
        const parsed = JSON.parse(storedAmountInfo) as { payableAmount: unknown; creditLimit: unknown };
        return { payableAmount: Number(parsed.payableAmount), creditLimit: Number(parsed.creditLimit) };
      })()
    : { payableAmount: 0, creditLimit: 0 };

  // 결제 후 이용가능한도 = 한도금액 - (미결제금액 - 결제금액)
  const availableLimit =
    amountInfo.creditLimit - (amountInfo.payableAmount - request.payAmount);

  const completedAt = storedCompletedAt;

  return (
    <ImmediatePayCompletePage
      cardName={card.name}
      cardNumber={card.maskedNumber}
      amount={request.payAmount}
      account={`${account.bankName} ${account.maskedAccount}`.trim()}
      availableLimit={availableLimit}
      completedAt={completedAt}
      error={payError}
      onConfirm={() => navigate(PATHS.CARD.DASHBOARD, { replace: true })}
    />
  );
}

/* ------------------------------------------------------------------ */
/* 카드 관리 / 사용자 관리                                               */
/* ------------------------------------------------------------------ */

/** brand 값 → 카드 배경 그라디언트 */
const CARD_GRADIENTS: Record<string, string> = {
  VISA: "linear-gradient(135deg, #008485, #14b8a6)",
  Mastercard: "linear-gradient(135deg, #1a1a2e, #16213e)",
  AMEX: "linear-gradient(135deg, #2d6a4f, #1b4332)",
  JCB: "linear-gradient(135deg, #1e40af, #1e3a8a)",
  UnionPay: "linear-gradient(135deg, #c0392b, #96281b)",
};

/** GET /api/cards 응답의 카드 전체 필드 */
interface ApiCardFull {
  id: string;
  name: string;
  brand: string;
  maskedNumber: string;
  balance: number;
  expiry: string;
  paymentBank: string;
  paymentAccount: string;
  paymentDay: string;
  limitAmount: number;
  usedAmount: number;
}

export function MyCardManagementRoute() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [cards, setCards] = useState<CardItem[]>([]);
  const [rawCards, setRawCards] = useState<ApiCardFull[]>([]);
  const [loading, setLoading] = useState(true);
  /** 칩 탭에서 선택된 카드 ID — onCardSelect 콜백으로 동기화 */
  const [selectedCardId, setSelectedCardId] = useState("");
  const [modalOpen, setModalOpen] = useState(false);

  useEffect(() => {
    if (!user?.userId) return;

    const controller = new AbortController();

    axiosInstance
      .get<{ cards: ApiCardFull[] }>("/cards", { signal: controller.signal })
      .then((r) => {
        const { cards: raw } = r.data;
        setRawCards(raw);
        setCards(
          raw.map((c) => ({
            id: c.id,
            name: c.name,
            brand: c.brand as
              | "VISA"
              | "Mastercard"
              | "AMEX"
              | "JCB"
              | "UnionPay",
            image: (
              <div
                style={{
                  width: "100%",
                  height: "100%",
                  borderRadius: 12,
                  background:
                    CARD_GRADIENTS[c.brand] ??
                    "linear-gradient(135deg, #374151, #1f2937)",
                }}
              />
            ),
            balance: c.balance,
          })),
        );
        setLoading(false);
      })
      .catch((err: { code?: string }) => {
        // abort된 경우 loading을 유지 — StrictMode 2차 실행에서 정상 완료 후 false로 전환
        if (err.code !== "ERR_CANCELED") {
          console.error(err);
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [user?.userId]);

  if (loading) return null;

  /** 현재 선택된 카드의 전체 API 데이터 (선택 전에는 첫 번째 카드 사용) */
  const activeCard =
    rawCards.find((c) => c.id === selectedCardId) ?? rawCards[0];

  /** 모달에 표시할 카드정보 섹션 */
  const cardInfoSections = activeCard
    ? [
        {
          title: "카드정보",
          rows: [
            { label: "카드번호", value: activeCard.maskedNumber },
            { label: "카드구분", value: activeCard.name },
            { label: "유효기간", value: activeCard.expiry },
          ],
        },
        {
          title: "결제정보",
          rows: [
            { label: "결제은행명", value: activeCard.paymentBank },
            { label: "결제계좌", value: activeCard.paymentAccount },
            { label: "결제일", value: `${activeCard.paymentDay}일` },
            {
              label: "한도금액",
              value: `${activeCard.limitAmount.toLocaleString()}원`,
            },
            {
              label: "사용금액",
              value: `${activeCard.usedAmount.toLocaleString()}원`,
            },
          ],
        },
      ]
    : [];

  /**
   * 관리 행 목록 — 선택된 카드 데이터로 동적 구성
   *   - 0번 "카드정보 확인": subText = 마스킹 카드번호, onClick = 모달 오픈
   *   - 1번 "결제계좌": subText = 결제은행명 + 계좌번호
   */
  const managementRows = MOCK_MANAGEMENT_ROWS.map((row, i) => {
    if (i === 0)
      return {
        ...row,
        subText: activeCard?.maskedNumber,
        onClick: () => setModalOpen(true),
      };
    if (i === 1)
      return {
        ...row,
        subText: activeCard
          ? `${activeCard.paymentBank} ${activeCard.paymentAccount}`
          : row.subText,
      };
    return row;
  });

  return (
    <>
      <MyCardManagementPage
        cards={cards}
        managementRows={managementRows}
        onCardSelect={setSelectedCardId}
        onBack={() => navigate(-1)}
        onClose={() => navigate(PATHS.CARD.DASHBOARD, { replace: true })}
      />
      <BottomSheet
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        title="카드정보 확인"
      >
        <CardInfoPanel sections={cardInfoSections} />
      </BottomSheet>
    </>
  );
}

export function UserManagementRoute() {
  const navigate = useNavigate();
  return (
    <UserManagementPage
      users={MOCK_USERS}
      onBack={() => navigate(-1)}
      onMenuClick={() => {}}
      onAddUser={() => {}}
      onEditUser={() => {}}
      onDeleteUser={() => {}}
    />
  );
}

/* ------------------------------------------------------------------ */
/* Admin 미리보기 전용                                                    */
/* ------------------------------------------------------------------ */

/**
 * 긴급공지 미리보기 페이지.
 *
 * Admin 관리 화면의 iframe에서 호출되는 공개 경로(/preview/notice).
 * 인증 없이 접근 가능하며, DEPLOY_STATUS와 무관하게 최신 저장된 공지 내용을 표시한다.
 * displayType이 'N'(사용안함)인 경우에도 내용 확인을 위해 배너를 강제 표시하고 안내 문구를 노출한다.
 */
export function NoticePreviewRoute() {
  const [searchParams] = useSearchParams();
  // lang 파라미터: Admin 미리보기 버튼에서 언어 코드(EMERGENCY_KO / EMERGENCY_EN)를 전달한다.
  const lang = searchParams.get("lang") ?? "EMERGENCY_KO";
  const [data, setData] = useState<NoticePayload | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    const controller = new AbortController();

    fetch("/api/notices/preview", { signal: controller.signal })
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json() as Promise<NoticePayload>;
      })
      .then(setData)
      .catch((err: { name?: string }) => {
        // AbortController 취소 시 발생하는 AbortError는 무시
        if (err.name !== "AbortError") setError(true);
      })
      .finally(() => setLoading(false));

    return () => controller.abort();
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen text-sm text-text-muted">
        로딩 중...
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="flex items-center justify-center min-h-screen text-sm text-text-muted">
        공지 데이터를 불러올 수 없습니다.
      </div>
    );
  }

  // 미리보기 모드: displayType이 N이어도 배너를 강제 표시 (내용 확인 목적)
  const isUnused = data.displayType === "N";
  const previewData: NoticePayload = isUnused
    ? { ...data, displayType: "A" }
    : data;

  return (
    <div className="min-h-screen bg-bg-base">
      {/* 미리보기 모드 안내 */}
      <div className="flex items-center justify-center py-1 bg-blue-50 border-b border-blue-200">
        <span className="text-xs text-blue-600 font-medium">
          미리보기 모드
          {isUnused && (
            <span className="ml-1 text-orange-500">
              (노출 타입: 사용안함 — 실제 배포 시 미표시)
            </span>
          )}
        </span>
      </div>
      <EmergencyNoticeBanner data={previewData} forceOpen lang={lang} />
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* Admin 승인 컴포넌트 뷰어                                              */
/* ------------------------------------------------------------------ */

/**
 * Admin에서 승인 시 저장된 React 컴포넌트 뷰어 페이지.
 *
 * /react/viewer/:codeId 경로로 접근하며, 인증 없이 사용 가능하다.
 * import.meta.glob으로 src/generated/ 디렉토리의 .tsx 파일을 동적으로 탐색하여
 * :codeId에 해당하는 컴포넌트를 렌더링한다.
 *
 * Vite HMR이 파일 추가를 감지하므로 Admin에서 승인 즉시 새 경로가 활성화된다.
 */
export function ReactViewerRoute() {
  const { codeId } = useParams<{ codeId: string }>();
  const [Component, setComponent] = useState<ComponentType | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!codeId) {
      setNotFound(true);
      return;
    }

    // src/generated/*.tsx 파일 전체를 지연 로드(lazy) 맵으로 확보
    // import.meta.glob은 Vite가 빌드 타임에 glob 패턴을 해석하므로 동적 경로 조합 불가
    // → 전체 맵을 먼저 만든 뒤 codeId로 키를 선택한다
    const modules = import.meta.glob<{ default: ComponentType }>(
      "/src/generated/*.tsx",
    );
    const key = `/src/generated/${codeId}.tsx`;

    if (!modules[key]) {
      setNotFound(true);
      return;
    }

    modules[key]()
      .then((mod) => setComponent(() => mod.default))
      .catch((err) => {
        // 네트워크 오류와 파일 미존재를 구분하기 위해 에러를 기록한다
        console.error(
          `[ReactViewer] 컴포넌트 로드 실패 — codeId=${codeId}`,
          err,
        );
        setNotFound(true);
      });
  }, [codeId]);

  return (
    <div className="min-h-screen bg-bg-base">
      {/* 뷰어 안내 배너 */}
      <div className="flex items-center justify-center py-1 bg-blue-50 border-b border-blue-200">
        <span className="text-xs text-blue-600 font-medium">
          승인된 React 컴포넌트 뷰어 — {codeId}
        </span>
      </div>

      {notFound && (
        <div className="flex items-center justify-center min-h-[80vh] text-sm text-text-muted">
          승인된 컴포넌트를 찾을 수 없습니다. (codeId: {codeId})
        </div>
      )}

      {!notFound && !Component && (
        <div className="flex items-center justify-center min-h-[80vh] text-sm text-text-muted">
          로딩 중...
        </div>
      )}

      {Component && <Component />}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* React CMS 배포 페이지 뷰어                                            */
/* ------------------------------------------------------------------ */

/**
 * Admin에서 배포한 React CMS 페이지 뷰어.
 *
 * /react-cms/viewer/:pageId 경로로 접근하며, 인증 없이 사용 가능하다.
 * import.meta.glob으로 src/reactcms/containers/ 디렉토리의 .tsx 파일을 동적으로 탐색하여
 * :pageId에 해당하는 컴포넌트를 렌더링한다.
 *
 * Vite HMR이 파일 추가를 감지하므로 Admin에서 배포 즉시 새 경로가 활성화된다.
 */
export function ReactCmsPageViewerRoute() {
  const { pageId } = useParams<{ pageId: string }>();
  const [Component, setComponent] = useState<ComponentType | null>(null);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!pageId) {
      setNotFound(true);
      return;
    }

    // src/reactcms/containers/*.tsx 파일 전체를 지연 로드(lazy) 맵으로 확보
    const modules = import.meta.glob<{ default: ComponentType }>(
      "/src/reactcms/containers/*.tsx",
    );
    const key = `/src/reactcms/containers/${pageId}.tsx`;

    if (!modules[key]) {
      setNotFound(true);
      return;
    }

    modules[key]()
      .then((mod) => setComponent(() => mod.default))
      .catch((err) => {
        console.error(
          `[ReactCmsViewer] 컴포넌트 로드 실패 — pageId=${pageId}`,
          err,
        );
        setNotFound(true);
      });
  }, [pageId]);

  return (
    <div className="min-h-screen bg-bg-base">
      {/* <div className="flex items-center justify-center py-1 bg-green-50 border-b border-green-200">
        <span className="text-xs text-green-600 font-medium">
          React CMS 배포 페이지 뷰어 — {pageId}
        </span>
      </div> */}

      {notFound && (
        <div className="flex items-center justify-center min-h-[80vh] text-sm text-text-muted">
          배포된 CMS 페이지를 찾을 수 없습니다. (pageId: {pageId})
        </div>
      )}

      {!notFound && !Component && (
        <div className="flex items-center justify-center min-h-[80vh] text-sm text-text-muted">
          로딩 중...
        </div>
      )}

      {Component && <Component />}
    </div>
  );
}

/* ------------------------------------------------------------------ */
/* 모달 오버레이                                                          */
/* ------------------------------------------------------------------ */

/**
 * 전체메뉴 모달.
 * - ModalSlideOver onClose: navigate(-1) — 모달 닫기 = 직전 페이지(대시보드)로 복귀.
 * - 메뉴 항목 클릭: replace: true — 뒤로가기 시 메뉴가 히스토리에 남지 않도록 한다.
 */
export function HanaCardMenuModal() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const menuItems: MenuItem[] = [
    {
      id: "usage-history",
      category: "history",
      label: "이용내역",
      icon: <Receipt className="size-5" />,
      onClick: () => navigate(PATHS.CARD.USAGE_HISTORY, { replace: true }),
    },
    {
      id: "statement",
      category: "history",
      label: "이용대금명세서",
      icon: <FileText className="size-5" />,
      onClick: () => navigate(PATHS.CARD.PAYMENT_STATEMENT, { replace: true }),
    },
    {
      id: "immediate-payment",
      category: "payment",
      label: "즉시결제",
      icon: <Wallet className="size-5" />,
      onClick: () => navigate(PATHS.CARD.IMMEDIATE_PAYMENT, { replace: true }),
    },
    {
      id: "card-management",
      category: "management",
      label: "카드 관리",
      icon: <Settings className="size-5" />,
      onClick: () => navigate(PATHS.CARD.MY_CARD_MANAGEMENT, { replace: true }),
    },
    {
      id: "benefits",
      category: "benefit",
      label: "혜택/포인트 조회",
      icon: <Gift className="size-5" />,
      onClick: () => {},
    },
    {
      id: "card-apply",
      category: "service",
      label: "카드 신청",
      icon: <CreditCard className="size-5" />,
      onClick: () => {},
    },
    {
      id: "customer-service",
      category: "service",
      label: "고객센터",
      icon: <Headphones className="size-5" />,
      onClick: () => {},
    },
  ];

  return (
    <ModalSlideOver onClose={() => navigate(-1)}>
      <HanaCardMenuPage
        userName={user ? `${user.userName}님` : ""}
        lastLogin={user?.lastLogin ?? ""}
        categories={[
          { id: "all", label: "전체" },
          { id: "history", label: "이용내역" },
          { id: "payment", label: "결제" },
          { id: "management", label: "카드관리" },
          { id: "benefit", label: "혜택" },
          { id: "service", label: "서비스" },
        ]}
        menuItems={menuItems}
        onBack={() => navigate(-1)}
        onProfileManage={() => {}}
        onLogout={() => {
          logout();
          navigate(PATHS.LOGIN, { replace: true });
        }}
      />
    </ModalSlideOver>
  );
}
