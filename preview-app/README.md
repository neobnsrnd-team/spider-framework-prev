# preview-app

**Claude가 생성한 TSX 코드를 브라우저에서 즉시 렌더링하는 미리보기 앱**

`reactPlatform`의 React 코드 생성 화면에 `<iframe>`으로 삽입되며,
부모 페이지로부터 `postMessage`로 TSX 코드를 수신해 런타임에 트랜스파일 후 렌더링한다.

---

## 목차

- [기술 스택](#기술-스택)
- [동작 방식](#동작-방식)
  - [통신 흐름](#통신-흐름)
  - [렌더링 파이프라인](#렌더링-파이프라인)
  - [컴포넌트 레지스트리](#컴포넌트-레지스트리)
  - [오류 처리](#오류-처리)
- [빌드 및 배포](#빌드-및-배포)
- [개발 서버](#개발-서버)
- [파일 구조](#파일-구조)

---

## 기술 스택

| 구분       | 기술                               |
| ---------- | ---------------------------------- |
| Framework  | React 19 + TypeScript              |
| Build      | Vite 8                             |
| Style      | Tailwind CSS 4 + `@cl/dist/styles.css` (디자인 토큰 + 유틸리티) |
| 트랜스파일 | @babel/standalone (CDN)            |
| 컴포넌트   | reactive-springware + lucide-react |

---

## 동작 방식

### 통신 흐름

```
reactPlatform (Thymeleaf)
  │
  │  // 코드 생성 완료 후 iframe으로 전달
  │  frame.contentWindow.postMessage(
  │    { type: 'UPDATE_CODE', code: tsxCode, codeId: id },
  │    window.location.origin
  │  )
  │
  ↓
preview-app (iframe, /preview-app/index.html)
  │
  │  window.addEventListener('message', handler)
  │  → type === 'UPDATE_CODE' 이면 code / codeId 상태 갱신
  │
  ↓
Renderer.tsx — 렌더링 파이프라인 실행
```

### 렌더링 파이프라인

`Renderer.tsx`가 수신한 TSX 코드를 5단계로 처리한다.

```
① patchImports(code)
   import { Button } from '@cl'
     → const { Button } = window.__components
   import * as Icons from 'lucide-react'
     → const Icons = window.__components
   import type { ... }  → 제거 (런타임 불필요)

② patchExport(code)
   export default function MyPage()
     → var __Component = function MyPage()

③ window.Babel.transform(patched, { presets: ['react', 'typescript'] })
   TSX + TypeScript → 브라우저 실행 가능한 JavaScript

④ new Function('React', `${compiled}\nreturn __Component`)(React)
   React를 명시적으로 주입하여 JSX 변환(React.createElement) 지원
   → React.ComponentType 추출

⑤ ReactDOM.createRoot(container).render(
     <ErrorBoundary ...>
       <Component />
     </ErrorBoundary>
   )
```

> **왜 @babel/standalone을 CDN으로 로드하는가?**
> npm 번들에 포함하면 최종 번들 크기가 ~8 MB 이상 커진다.
> CDN 로드는 `<script type="module">` 보다 먼저 실행되므로
> React 앱 초기화 시점에 `window.Babel`이 항상 보장된다.

### 컴포넌트 레지스트리

`componentRegistry.ts`는 앱 진입 전(`main.tsx`에서 import)에 `window.__components`를 초기화한다.
Renderer가 import 구문을 교체한 코드는 모든 심볼을 이 전역 객체에서 찾으므로,
생성 코드가 사용할 수 있는 컴포넌트와 아이콘은 반드시 여기에 등록되어야 한다.

```ts
// componentRegistry.ts
window.__components = {
  ...LucideIcons, // lucide-react 아이콘 전체
  ...RSW, // reactive-springware 컴포넌트 전체 (충돌 시 우선)
};
```

reactive-springware 컴포넌트는 `@cl` alias로 소스를 직접 참조하므로
별도 dist 빌드 없이 변경 사항이 즉시 반영된다.

### 디자인 토큰 적용

`src/index.css`가 `@cl/dist/styles.css`를 import하여 디자인 토큰과 Tailwind 유틸리티를 로드한다.
생성된 컴포넌트 코드에 `data-brand="hana"` / `data-domain="card"` 속성이 있으면
해당 브랜드·도메인의 CSS 변수가 자동으로 활성화된다.

### 오류 처리

오류 발생 단계에 따라 처리 방식이 다르다.

| 단계 | 오류 유형                                            | 처리 방식                             |
| ---- | ---------------------------------------------------- | ------------------------------------- |
| ①~④  | Babel 파싱 실패, `new Function` 실패 등              | 동기 `try-catch`                      |
| ⑤    | undefined 컴포넌트, props 타입 불일치 등 런타임 오류 | `ErrorBoundary` (`componentDidCatch`) |

> React 18+의 `createRoot().render()`는 비동기이므로,
> 컴포넌트 트리 내부 오류는 `try-catch`로 잡히지 않는다. `ErrorBoundary`가 필수인 이유다.

오류가 포착되면 `reportRenderError()`가 `POST /api/react-generate/render-error`로
`codeId`와 오류 메시지를 서버에 기록한다 (fire-and-forget).

---

## 빌드 및 배포

빌드 결과물은 `admin` Spring Boot 서버의 정적 리소스 경로에 직접 출력된다.

```bash
# preview-app 디렉토리에서 실행
npm install
npm run build
# → ../reactPlatform/src/main/resources/static/preview-app/ 에 출력
```

`reactPlatform` 서버는 `/preview-app/**` 경로로 이 파일들을 서빙하며,
`reactPlatform`의 Thymeleaf 화면이 `<iframe src="/preview-app/index.html">`으로 삽입한다.

---

## 개발 서버

```bash
npm run dev
# → http://localhost:5173/preview-app/
```

단독 개발 서버로 실행하면 `postMessage`를 보내는 부모 페이지가 없으므로
"코드 생성 후 미리보기가 여기에 표시됩니다." 안내 문구만 나타난다.
실제 동작 확인은 `reactPlatform` + `admin` 서버를 함께 실행해야 한다.

---

## 파일 구조

```
preview-app/
├── index.html              # @babel/standalone CDN 로드 + 루트 마운트 포인트
├── vite.config.ts          # base: /preview-app/, outDir: ../admin/.../static/preview-app
├── src/
│   ├── main.tsx            # 진입점 — postMessage 수신, componentRegistry 초기화
│   ├── index.css           # Tailwind 엔트리 + @import "@cl/dist/styles.css"
│   ├── Renderer.tsx        # 핵심 렌더링 엔진 (import 패치 → Babel → new Function → React)
│   ├── componentRegistry.ts# window.__components 초기화 (reactive-springware + lucide-react)
│   ├── ErrorBoundary.tsx   # 런타임 렌더링 오류 포착 (React 클래스형 컴포넌트)
│   └── global.d.ts         # window.Babel, window.__components 타입 선언
└── package.json
```
