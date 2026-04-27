# Reactive-Springware Claude Code 지침

## 컴포넌트 신규 추가 / 수정 시

`component-library/`에 새 컴포넌트를 추가하거나 기존 컴포넌트를 수정할 때는 반드시 아래 파일을 먼저 읽고 체크리스트를 따른다.

```
docs/component-checklist.md
```

---

## 코드 작성 규칙

### 주석

**파일 상단 JSDoc**

새 파일을 생성할 때는 반드시 파일 상단에 JSDoc 주석을 작성한다.

- `@file` — 파일명
- `@description` — 역할 설명
- `@param` / `@returns` — 주요 파라미터 및 반환값
- `@example` — 필요한 경우 사용 예시

**인라인 주석**

다음 경우에는 반드시 인라인 주석을 추가한다.

- 왜 이 값을 선택했는지 이유가 필요한 상수·기본값
- 외부 개발자가 처음 봤을 때 의도를 오해할 수 있는 로직
- 타입 단언(`as`)이나 예외 처리 등 방어 코드
- 여러 분기 중 특정 분기가 존재하는 이유가 명확하지 않은 경우

---

## 디자인 토큰 규칙

### temp.json을 받으면 반드시 아래 절차를 수행한다

```
1. temp.json 파싱
   └─ 최상위 키(primitives / semantic / brand.* / domain.*)를 기준으로 분류

2. temp.json → globals.css 전체 재생성
   ├─ [data-brand="*"] 블록 — brand.* 토큰에서 생성
   ├─ [data-domain="*"] 블록 — domain.* 토큰에서 생성
   ├─ @theme 블록 — semantic.* / primitives 토큰에서 생성
   └─ @layer base 블록 — 전역 기본 스타일 유지

3. temp.json 삭제 안내
   └─ "temp.json은 삭제해도 됩니다" 메시지 출력
```
