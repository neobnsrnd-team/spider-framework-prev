/**
 * @file SavePageModal.tsx
 * @description 페이지 저장 모달.
 * 컴포넌트명(PascalCase)과 저장 위치(파일 시스템 경로)를 입력받아 onSave 핸들러를 호출합니다.
 * 저장 성공 시 성공 상태를 표시하고, 실패 시 오류 메시지를 인라인으로 표시합니다.
 *
 * 저장 위치는 호스트 프로젝트의 라우팅 정책에 영향을 주지 않으며,
 * 라우트 등록은 호출 측 개발자가 직접 수행해야 합니다.
 *
 * @param page 저장할 CMSPage 데이터
 * @param onClose 모달 닫기 핸들러
 * @param onSave 저장 핸들러. 생략 시 저장 버튼 비활성
 * @param initialPageName 편집 모드에서 사용할 초기 컴포넌트명
 * @param requireSavePath true(기본)이면 저장 위치 입력란을 표시하고 필수값으로 검증한다.
 *                       false면 입력란을 숨기고 savePath 없이 onSave를 호출한다(예: DB 저장 모드).
 * @param defaultSavePath 저장 위치 입력란의 초기값 (requireSavePath=true일 때만 의미)
 */
import { useState } from "react";
import type { CMSPage } from "./types";
import { validateRelativeSavePath } from "./utils/savePath";

export interface SavePageParams {
  /** PascalCase 컴포넌트명, 예: "MyPage" */
  pageName: string;
  /**
   * 페이지 파일을 저장할 디렉토리 경로 (Vite 프로젝트 root 기준 상대 경로).
   * 예: "../demo/front/src/pages/cms"
   * DB 저장 모드 등 파일을 쓰지 않는 경우 undefined.
   */
  savePath?: string;
  /** CMSBuilder가 Context 정보(layouts, codegenConfig, overlayTemplates)를 포함해 사전 생성한 JSX 코드 */
  code?: string;
}

interface SavePageModalProps {
  page: CMSPage;
  onClose: () => void;
  /** 소비자가 제공하는 저장 핸들러. 생략 시 저장 버튼은 비활성 */
  onSave?: (page: CMSPage, params: SavePageParams) => void | Promise<void>;
  /** 편집 모드에서 기존 페이지명을 초기값으로 설정 */
  initialPageName?: string;
  /**
   * 저장 위치 입력란 표시 여부.
   * true(기본): 단독 실행 모드 등 파일 시스템에 저장하는 경우.
   * false: admin 연동 모드(DB 저장)처럼 위치 개념이 없는 경우 입력란 숨김.
   */
  requireSavePath?: boolean;
  /** 저장 위치 입력란의 초기값 (requireSavePath=true일 때만 의미) */
  defaultSavePath?: string;
}

interface ValidateOptions {
  requireSavePath: boolean;
}

function validate(params: SavePageParams, opts: ValidateOptions): string | null {
  if (!params.pageName) return "컴포넌트명을 입력하세요.";
  if (!/^[A-Z][A-Za-z0-9]*$/.test(params.pageName))
    return "컴포넌트명은 대문자로 시작하는 영문/숫자만 사용할 수 있습니다.";
  if (opts.requireSavePath) {
    // 저장 경로 검증은 클라이언트·서버 공통 헬퍼로 위임 (cms-core/utils/savePath)
    const savePathError = validateRelativeSavePath(params.savePath);
    if (savePathError) return savePathError;
  }
  return null;
}

export default function SavePageModal({
  page,
  onClose,
  onSave,
  initialPageName,
  requireSavePath = true,
  defaultSavePath,
}: SavePageModalProps) {
  const [pageName, setPageName] = useState(initialPageName ?? "");
  const [savePath, setSavePath] = useState(defaultSavePath ?? "");
  const [status, setStatus] = useState<"idle" | "loading" | "success" | "error">("idle");
  const [errorMsg, setErrorMsg] = useState("");

  async function handleSave() {
    // requireSavePath=false인 경우(예: DB 저장 모드) savePath 자체를 페이로드에서 제외
    const params: SavePageParams = requireSavePath
      ? { pageName, savePath }
      : { pageName };
    const validationError = validate(params, { requireSavePath });
    if (validationError) {
      setErrorMsg(validationError);
      setStatus("error");
      return;
    }

    setStatus("loading");
    setErrorMsg("");
    try {
      if (onSave) {
        await onSave(page, params);
      }
      setStatus("success");
      // 팝업으로 열린 경우 어드민 대시보드에 저장 완료 신호 전달
      // 동일 Origin으로 한정하여 타 도메인 메시지 수신 차단
      window.opener?.postMessage('reactCmsSaved', window.location.origin);
    } catch (e) {
      setErrorMsg(e instanceof Error ? e.message : "알 수 없는 오류가 발생했습니다.");
      setStatus("error");
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-6"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-2xl shadow-2xl w-full max-w-[24rem] p-6 flex flex-col gap-4"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-sm font-bold text-gray-900">페이지 저장</h2>

        {status === "success" ? (
          <div className="flex flex-col gap-4">
            <div className="rounded-lg bg-green-50 border border-green-200 p-3 text-xs text-green-700">
              <p className="font-semibold mb-1">저장 완료</p>
              {requireSavePath ? (
                <>
                  <p>
                    <span className="font-mono">{savePath}/{pageName}.tsx</span> 파일이 생성되었습니다.
                  </p>
                  <p className="mt-1 text-green-600">
                    라우트 등록은 직접 진행해주세요.
                  </p>
                </>
              ) : (
                <p>페이지가 저장되었습니다.</p>
              )}
            </div>
            <button
              className="w-full py-2 text-xs font-medium rounded-lg bg-gray-100 hover:bg-gray-200 text-gray-700 transition-colors"
              onClick={onClose}
            >
              닫기
            </button>
          </div>
        ) : (
          <>
            <div className="flex flex-col gap-3">
              <Field label="컴포넌트명" hint="PascalCase (예: MyPage)">
                <input
                  type="text"
                  className="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-primary/40"
                  placeholder="MyPage"
                  value={pageName}
                  onChange={(e) => setPageName(e.target.value)}
                  disabled={status === "loading"}
                />
              </Field>

              {requireSavePath && (
                <Field label="저장 위치" hint="프로젝트 root 기준 상대 경로">
                  <input
                    type="text"
                    className="w-full border border-gray-200 rounded-lg px-3 py-2 text-xs focus:outline-none focus:ring-2 focus:ring-primary/40"
                    placeholder="src/pages/cms"
                    value={savePath}
                    onChange={(e) => setSavePath(e.target.value)}
                    disabled={status === "loading"}
                  />
                </Field>
              )}
            </div>

            {status === "error" && (
              <p className="text-xs text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
                {errorMsg}
              </p>
            )}

            <div className="flex gap-2 justify-end">
              <button
                className="px-4 py-2 text-xs rounded-lg bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium transition-colors"
                onClick={onClose}
                disabled={status === "loading"}
              >
                취소
              </button>
              <button
                className="px-4 py-2 text-xs rounded-lg bg-primary hover:bg-primary-dark text-white font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                onClick={handleSave}
                disabled={status === "loading"}
              >
                {status === "loading" ? "저장 중…" : "저장"}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function Field({
  label,
  hint,
  children,
}: {
  label: string;
  hint: string;
  children: React.ReactNode;
}) {
  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-center justify-between">
        <span className="text-xs font-medium text-gray-700">{label}</span>
        <span className="text-[10px] text-gray-400">{hint}</span>
      </div>
      {children}
    </div>
  );
}
