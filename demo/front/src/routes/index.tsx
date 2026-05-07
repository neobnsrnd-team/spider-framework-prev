/**
 * @file index.tsx
 * @description 애플리케이션 라우트 설정.
 *
 * "어떤 경로에 어떤 컴포넌트가 연결되는지"만 선언한다.
 * 페이지 이동 로직은 RouteWrappers.tsx, 경로 상수는 paths.ts 에서 관리한다.
 *
 * pageRoutes  — 일반 페이지 (항상 렌더링)
 * modalRoutes — 모달 오버레이 (App.tsx 의 background location 패턴에서만 렌더링)
 */
import type { ReactElement } from "react";
import { PATHS } from "@/constants/paths";
import {
  LoginRoute,
  CardDashboardRoute,
  UsageHistoryRoute,
  PaymentStatementRoute,
  ImmediatePaymentRoute,
  ImmediatePayRoute,
  ImmediatePayRequestRoute,
  ImmediatePayMethodRoute,
  ImmediatePayCompleteRoute,
  MyCardManagementRoute,
  UserManagementRoute,
  HanaCardMenuModal,
  NoticePreviewRoute,
  ReactViewerRoute,
  ReactCmsPageViewerRoute,
} from "./RouteWrappers";

export interface RouteConfig {
  path: string;
  element: ReactElement;
}

export const pageRoutes: RouteConfig[] = [
  { path: PATHS.LOGIN,                element: <LoginRoute /> },
  { path: PATHS.PREVIEW.NOTICE,       element: <NoticePreviewRoute /> },
  { path: PATHS.VIEWER.REACT,          element: <ReactViewerRoute /> },
  { path: PATHS.VIEWER.REACT_CMS,      element: <ReactCmsPageViewerRoute /> },
  { path: PATHS.CARD.DASHBOARD, element: <CardDashboardRoute /> },
  { path: PATHS.CARD.USAGE_HISTORY, element: <UsageHistoryRoute /> },
  { path: PATHS.CARD.PAYMENT_STATEMENT, element: <PaymentStatementRoute /> },
  { path: PATHS.CARD.IMMEDIATE_PAYMENT, element: <ImmediatePaymentRoute /> },
  { path: PATHS.CARD.IMMEDIATE_PAY, element: <ImmediatePayRoute /> },
  {
    path: PATHS.CARD.IMMEDIATE_PAY_REQUEST,
    element: <ImmediatePayRequestRoute />,
  },
  {
    path: PATHS.CARD.IMMEDIATE_PAY_METHOD,
    element: <ImmediatePayMethodRoute />,
  },
  {
    path: PATHS.CARD.IMMEDIATE_PAY_COMPLETE,
    element: <ImmediatePayCompleteRoute />,
  },
  { path: PATHS.CARD.MY_CARD_MANAGEMENT, element: <MyCardManagementRoute /> },
  { path: PATHS.CARD.USER_MANAGEMENT, element: <UserManagementRoute /> },
];

export const modalRoutes: RouteConfig[] = [
  { path: PATHS.CARD.MENU, element: <HanaCardMenuModal /> },
];
