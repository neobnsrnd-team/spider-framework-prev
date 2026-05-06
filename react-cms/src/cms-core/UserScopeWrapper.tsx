/**
 * @file UserScopeWrapper.tsx
 * @description 외부 프로젝트 스타일 격리 래퍼.
 * stylesheetContent(인라인 문자열) 또는 stylesheet(URL fetch) 방식으로 CSS를 로드하고
 * CSS @scope 규칙으로 모든 선택자를 [data-cms-user-scope] 하위에만 적용합니다.
 * CMS 빌더 UI(모달, 사이드바 등)에 외부 프로젝트 스타일이 누출되지 않도록 격리합니다.
 */
import { useContext, useEffect, useRef, useState } from "react";
import { StylesheetContext } from "./context";

// ─── @import url() 추출 ──────────────────────────────────────────────────────

/**
 * @description CSS 텍스트에서 `@import url(...)` 규칙을 추출합니다.
 * CSS 스펙상 `@import`는 최상위 레벨에만 허용되므로, `@scope` 블록 안에 포함되면
 * 브라우저가 `@scope` 전체를 무시하거나 전역으로 적용하는 문제가 발생합니다.
 * (Google Fonts 등 외부 URL import가 대표적인 사례)
 *
 * @param css 원본 CSS 문자열
 * @returns imports: 최상위에 둬야 할 @import 규칙, rest: 나머지 CSS
 */
function extractTopLevelImports(css: string): { imports: string; rest: string } {
  const imports: string[] = [];
  const rest = css.replace(
    /^(@import\s+url\([^)]*\)[^;]*;)[ \t]*\n?/gm,
    (_match, rule: string) => {
      imports.push(rule);
      return "";
    },
  );
  return { imports: imports.join("\n"), rest };
}

// ─── CSS @scope 래핑 ──────────────────────────────────────────────────────────

/**
 * @description CSS 텍스트를 @scope ([data-cms-user-scope]) 블록으로 감싸
 * 모든 선택자가 [data-cms-user-scope] 하위 요소에만 적용되도록 격리합니다.
 * :root는 :scope로 변환해 스코프 루트 요소(UserScopeWrapper div)에 CSS 변수가 설정됩니다.
 * 이를 통해 외부 프로젝트의 button, input 등 전역 선택자가 CMS 빌더 UI에 누출되는 것을 방지합니다.
 * @param css @import url()이 이미 제거된 CSS 문자열
 * @returns @scope 블록으로 래핑된 CSS 문자열
 */
function scopeCss(css: string): string {
  // 주석·문자열 리터럴은 원본 유지.
  // :root → :scope 변환 (CSS 변수를 스코프 루트에 설정).
  // 줄 시작의 [data-*] 선택자 → :scope[data-*] 변환:
  //   stylesheetScope가 data-cms-user-scope div 자체에 data-brand/domain을 설정하므로
  //   @scope 내 [data-*] 선택자는 스코프 루트의 자식이 아닌 루트 자신을 타겟해야 한다.
  //   :scope 를 붙이면 스코프 루트 자체를 선택할 수 있어 CSS 변수가 올바르게 적용된다.
  const scoped = css.replace(
    /(\/\*[\s\S]*?\*\/|"[^"]*"|'[^']*')|(:root\b)|(^[ \t]*)\[data-/gm,
    (match, skip, root, linePrefix) => {
      if (skip !== undefined) return skip;
      if (root !== undefined) return ':scope';
      return `${linePrefix}:scope[data-`;
    },
  );
  return `@scope ([data-cms-user-scope]) {\n${scoped}\n}`;
}

// ─── 인라인 CSS 캐시 ──────────────────────────────────────────────────────────
//
// 동일한 stylesheetContent를 가진 UserScopeWrapper 인스턴스가 여럿 마운트될 때
// (예: BlockThumbnail 57개) style 태그를 공유합니다.
//
// 구조: content → { elements: <style>[], count: 참조 카운트 }
//   - 첫 마운트: style 태그 생성·삽입, count = 1
//   - 추가 마운트: count++ 만 실행 (style 태그 재사용)
//   - 언마운트: count-- → 0이 되면 style 태그 제거
const inlineStyleCache = new Map<string, { elements: HTMLStyleElement[]; count: number }>();

/**
 * @description 컴파일된 CSS 문자열을 두 개의 <style> 태그로 분리 주입합니다.
 * 동일 content는 모듈 레벨 캐시로 공유 — 중복 삽입 및 CSS 파싱 반복 방지.
 * 1. @import url() 규칙 → 최상위 <style> (fonts 등 전역 리소스)
 * 2. 나머지 CSS → @scope로 래핑한 <style> (캔버스 내부에만 적용)
 * @param content 컴파일된 CSS 문자열
 */
function useInlineStylesheet(content: string | undefined) {
  useEffect(() => {
    if (!content) return;

    const cached = inlineStyleCache.get(content);
    if (cached) {
      // 이미 삽입된 style 태그 재사용 — count만 증가
      cached.count++;
    } else {
      const { imports, rest } = extractTopLevelImports(content);
      const elements: HTMLStyleElement[] = [];

      // @import url()은 @scope 밖에서 최상위로 주입
      if (imports) {
        const importEl = document.createElement("style");
        importEl.setAttribute("data-cms-user-stylesheet", "fonts");
        importEl.textContent = imports;
        document.head.appendChild(importEl);
        elements.push(importEl);
      }

      // 나머지 스타일은 @scope로 격리
      const scopedEl = document.createElement("style");
      scopedEl.setAttribute("data-cms-user-stylesheet", "inline");
      scopedEl.textContent = scopeCss(rest);
      document.head.appendChild(scopedEl);
      elements.push(scopedEl);

      inlineStyleCache.set(content, { elements, count: 1 });
    }

    return () => {
      const c = inlineStyleCache.get(content);
      if (!c) return;
      c.count--;
      // 마지막 인스턴스가 언마운트될 때만 style 태그 제거
      if (c.count === 0) {
        c.elements.forEach((el) => el.remove());
        inlineStyleCache.delete(content);
      }
    };
  }, [content]);
}

// ─── 외부 URL CSS 캐시 ────────────────────────────────────────────────────────
//
// 동일 URL을 여러 인스턴스가 동시에 마운트할 때 fetch를 한 번만 수행합니다.
//
// 구조: url → { elements, count, ready, listeners }
//   - 첫 마운트: fetch 시작, count = 1
//   - fetch 진행 중 추가 마운트: count++, listeners에 setReady 등록 → fetch 완료 시 일괄 ready
//   - 이미 fetch 완료된 상태에서 마운트: count++, 즉시 ready = true
//   - 언마운트: count-- → 0이 되면 style 태그 제거 및 캐시 삭제
type ExternalStyleEntry = {
  elements: HTMLStyleElement[];
  count: number;
  ready: boolean;
  /** fetch 완료를 기다리는 인스턴스들의 setReady 콜백 */
  listeners: Set<() => void>;
};

const externalStyleCache = new Map<string, ExternalStyleEntry>();

/**
 * @description stylesheet URL을 fetch해 스코프 변환 후 <style> 태그를 head에 삽입합니다.
 * 동일 URL은 모듈 레벨 캐시로 공유 — fetch와 style 삽입을 한 번만 수행합니다.
 * `stylesheetContent`가 제공된 경우에는 실행되지 않습니다.
 * @param stylesheet CSS URL
 * @param skip true이면 아무것도 하지 않음
 */
function useExternalStylesheet(stylesheet: string | undefined, skip: boolean) {
  // 이미 캐시에 fetch 완료된 항목이 있으면 초기값을 true로 설정해 불필요한 리렌더 방지
  const [ready, setReady] = useState<boolean>(() => {
    if (!stylesheet || skip) return true;
    return externalStyleCache.get(stylesheet)?.ready ?? false;
  });

  useEffect(() => {
    if (!stylesheet || skip) {
      setReady(true);
      return;
    }

    const cleanup = () => {
      const c = externalStyleCache.get(stylesheet);
      if (!c) return;
      c.count--;
      if (c.count === 0) {
        c.elements.forEach((el) => el.remove());
        externalStyleCache.delete(stylesheet);
      }
    };

    const existing = externalStyleCache.get(stylesheet);
    if (existing) {
      existing.count++;
      if (existing.ready) {
        // 이미 fetch 완료 — 즉시 ready
        setReady(true);
      } else {
        // fetch 진행 중 — 완료 시 알림 받도록 등록
        const onReady = () => setReady(true);
        existing.listeners.add(onReady);
        return () => {
          existing.listeners.delete(onReady);
          cleanup();
        };
      }
      return cleanup;
    }

    // 첫 번째 인스턴스 — fetch 시작
    const entry: ExternalStyleEntry = {
      elements: [],
      count: 1,
      ready: false,
      listeners: new Set(),
    };
    externalStyleCache.set(stylesheet, entry);
    let cancelled = false;

    fetch(stylesheet)
      .then((r) => r.text())
      .then((css) => {
        if (cancelled) return;

        const { imports, rest } = extractTopLevelImports(css);

        if (imports) {
          const importEl = document.createElement("style");
          importEl.setAttribute("data-cms-user-stylesheet", `${stylesheet}:fonts`);
          importEl.textContent = imports;
          document.head.appendChild(importEl);
          entry.elements.push(importEl);
        }

        const scopedEl = document.createElement("style");
        scopedEl.setAttribute("data-cms-user-stylesheet", stylesheet);
        scopedEl.textContent = scopeCss(rest);
        document.head.appendChild(scopedEl);
        entry.elements.push(scopedEl);

        entry.ready = true;
        setReady(true);
        // fetch 완료를 기다리던 모든 인스턴스에 알림
        entry.listeners.forEach((fn) => fn());
        entry.listeners.clear();
      })
      .catch(() => {
        if (!cancelled) {
          entry.ready = true;
          setReady(true);
          entry.listeners.forEach((fn) => fn());
          entry.listeners.clear();
        }
      });

    return () => {
      cancelled = true;
      cleanup();
    };
  }, [stylesheet, skip]);

  return ready;
}

// ─── UserScopeWrapper ────────────────────────────────────────────────────────

interface UserScopeWrapperProps {
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
}

/**
 * @description 외부 프로젝트 CSS를 격리해 자식 컴포넌트에만 적용합니다.
 * StylesheetContext에서 설정을 읽어 두 가지 방식 중 하나로 CSS를 주입합니다.
 *
 * - `stylesheetContent` (우선): 컴파일된 CSS 문자열 → 개발 환경에서 Vite `?inline` 사용
 * - `stylesheet` (폴백): 외부 CSS URL → fetch 후 스코프 변환 (프로덕션 URL용)
 *
 * 두 방식 모두 @import url()은 최상위 <style>로 분리하고,
 * 나머지 스타일은 CSS @scope로 [data-cms-user-scope] 범위로 제한합니다.
 *
 * @param children 스타일을 적용할 자식 컴포넌트
 * @param className 추가 className
 * @param style 추가 인라인 스타일
 */
export function UserScopeWrapper({ children, className, style }: UserScopeWrapperProps) {
  const { stylesheet, stylesheetContent, stylesheetScope } = useContext(StylesheetContext);
  const ref = useRef<HTMLDivElement>(null);

  const hasContent = !!stylesheetContent;

  // stylesheetContent 우선 — 인라인 문자열 직접 주입
  useInlineStylesheet(stylesheetContent);
  // stylesheet URL은 stylesheetContent가 없을 때만 fetch
  useExternalStylesheet(stylesheet, hasContent);

  // stylesheetScope data 속성 적용
  useEffect(() => {
    const el = ref.current;
    if (!el || !stylesheetScope) return;
    for (const [key, value] of Object.entries(stylesheetScope)) {
      el.setAttribute(key, value);
    }
    return () => {
      if (!el || !stylesheetScope) return;
      for (const key of Object.keys(stylesheetScope)) {
        el.removeAttribute(key);
      }
    };
  }, [stylesheetScope]);

  const hasStylesheet = hasContent || !!stylesheet;

  if (!hasStylesheet) {
    // 스타일시트가 없으면 래퍼 없이 그대로 렌더링
    return <>{children}</>;
  }

  return (
    <div ref={ref} data-cms-user-scope className={className} style={style}>
      {children}
    </div>
  );
}
