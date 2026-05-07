/**
 * @file exportCode.test.ts
 * @description generateJSX 통합 테스트.
 * 코드 생성 파이프라인의 핵심 동작을 회귀 방지 차원에서 검증한다.
 *
 * 검증 항목:
 *  1. string 이스케이프 — 따옴표·백슬래시 포함 사용자 입력 안전 출력
 *  2. icon-picker 자동 변환 — 빈 문자열 prop 생략, 값이 있으면 PascalCase JSX + lucide import
 *  3. event 필드 noop 자동 주입 — interaction과 중복 없이
 *  4. codegenImports — codegenProps가 참조하는 추가 컴포넌트가 import에 포함
 *  5. defaultProps 머지 — JSON 미저장 신규 필드도 codegenProps에 전달
 *  6. children JSXExpr — raw JSX 표현식 직접 삽입
 *  7. lucide 화이트리스트 — 컴포넌트 라이브러리 이름은 lucide import에서 제외
 */
import { describe, it, expect } from "vitest";
import { generateJSX } from "../exportCode";
import type { BlockDefinition, CMSPage } from "../../types";

// ── 테스트 헬퍼 ────────────────────────────────────────────────────────────────

function makeBlock(
  component: string,
  props: Record<string, unknown>,
  interaction: CMSPage["blocks"][number]["interaction"] = undefined,
): CMSPage["blocks"][number] {
  return {
    id: "b1",
    component,
    props,
    padding: { top: 0, right: 0, bottom: 0, left: 0 },
    ...(interaction ? { interaction } : {}),
  };
}

function makePage(blocks: CMSPage["blocks"]): CMSPage {
  return { blocks };
}

// ── 1. string 이스케이프 ──────────────────────────────────────────────────────

describe("generateJSX — string escape", () => {
  it("attribute 문자열에 따옴표가 있어도 JSX expression으로 안전 직렬화한다", () => {
    const def: BlockDefinition = {
      meta: {
        name: "Button",
        category: "core",
        defaultProps: { label: "" },
        propSchema: {
          label: { type: "string", label: "레이블", default: "" },
        },
      },
      component: () => null as never,
    };
    const code = generateJSX(
      makePage([makeBlock("Button", { label: '하나"테스트"' })]),
      [], {}, [], [def],
    );
    // JSON.stringify로 직렬화돼 expression 형태로 출력 — 깨진 attribute 형태가 발생하지 않는다.
    expect(code).toContain('label={"하나\\"테스트\\""}');
    expect(code).not.toContain('label="하나"테스트""');
  });

  it("백슬래시·줄바꿈 등 이스케이프 시퀀스를 안전하게 직렬화한다", () => {
    const def: BlockDefinition = {
      meta: {
        name: "Typography",
        category: "core",
        defaultProps: { text: "" },
        propSchema: { text: { type: "string", label: "텍스트", default: "" } },
      },
      component: () => null as never,
    };
    const code = generateJSX(
      makePage([makeBlock("Typography", { text: "line1\nline2\\path" })]),
      [], {}, [], [def],
    );
    expect(code).toContain('text={"line1\\nline2\\\\path"}');
  });
});

// ── 2. icon-picker 자동 변환 ──────────────────────────────────────────────────

describe("generateJSX — icon-picker auto-convert", () => {
  it("icon-picker 필드가 비어있으면 prop을 생략한다", () => {
    const def: BlockDefinition = {
      meta: {
        name: "MyBlock",
        category: "modules",
        defaultProps: { icon: "" },
        propSchema: {
          icon: { type: "icon-picker", label: "아이콘", default: "" },
        },
      },
      component: () => null as never,
    };
    const code = generateJSX(
      makePage([makeBlock("MyBlock", { icon: "" })]),
      [], {}, [], [def],
    );
    expect(code).not.toContain("icon=");
  });

  it("icon-picker 값을 PascalCase JSX로 변환하고 lucide import를 추가한다", () => {
    const def: BlockDefinition = {
      meta: {
        name: "MyBlock",
        category: "modules",
        defaultProps: { icon: "bell" },
        propSchema: {
          icon: { type: "icon-picker", label: "아이콘", default: "bell" },
        },
      },
      component: () => null as never,
    };
    const code = generateJSX(
      makePage([makeBlock("MyBlock", { icon: "bell" })]),
      [], {}, [], [def],
    );
    expect(code).toContain('icon={<Bell className="size-5" />}');
    expect(code).toContain('import { Bell } from "lucide-react";');
  });
});

// ── 3. event noop 자동 주입 ───────────────────────────────────────────────────

describe("generateJSX — event noop injection", () => {
  it("interaction에 바인딩되지 않은 event 필드에 noop을 자동 주입한다", () => {
    const def: BlockDefinition = {
      meta: {
        name: "MyBlock",
        category: "modules",
        defaultProps: {},
        propSchema: {
          onClick:  { type: "event", label: "클릭" },
          onChange: { type: "event", label: "변경" },
        },
      },
      component: () => null as never,
    };
    const code = generateJSX(
      makePage([makeBlock("MyBlock", {})]),
      [], {}, [], [def],
    );
    expect(code).toContain("onClick={() => {}}");
    expect(code).toContain("onChange={() => {}}");
  });

  it("interaction에 바인딩된 event 필드는 noop이 추가되지 않는다 (중복 방지)", () => {
    const def: BlockDefinition = {
      meta: {
        name: "MyBlock",
        category: "modules",
        defaultProps: {},
        propSchema: {
          onClick: { type: "event", label: "클릭" },
        },
      },
      component: () => null as never,
    };
    const code = generateJSX(
      makePage([
        makeBlock("MyBlock", {}, {
          onClick: { type: "navigate", path: "/home" },
        }),
      ]),
      [], {}, [], [def],
    );
    expect(code).toContain('onClick={() => navigate("/home")}');
    expect(code).not.toContain("onClick={() => {}}");
    // onClick은 정확히 한 번만 등장해야 한다
    const occurrences = (code.match(/onClick=/g) ?? []).length;
    expect(occurrences).toBe(1);
  });
});

// ── 4. codegenImports ─────────────────────────────────────────────────────────

describe("generateJSX — codegenImports", () => {
  it("codegenImports에 명시된 컴포넌트가 라이브러리 import에 포함된다", () => {
    const def: BlockDefinition = {
      meta: {
        name: "Card",
        category: "modules",
        defaultProps: {},
        propSchema: {},
      },
      component: () => null as never,
      codegenProps: () => ({
        children: { __jsx: '<CardHeader title="제목" /><CardRow label="x" value="y" />' },
      }),
      codegenImports: ["CardHeader", "CardRow"],
    };
    const code = generateJSX(
      makePage([makeBlock("Card", {})]),
      [], {}, [], [def],
    );
    const importMatch = code.match(/import \{ ([^}]+) \} from "@neobnsrnd-team\/cms-ui";/);
    expect(importMatch).not.toBeNull();
    const names = importMatch![1].split(",").map((s) => s.trim());
    expect(names).toContain("Card");
    expect(names).toContain("CardHeader");
    expect(names).toContain("CardRow");
  });
});

// ── 5. defaultProps 머지 ──────────────────────────────────────────────────────

describe("generateJSX — defaultProps merge", () => {
  it("JSON에 저장되지 않은 필드도 defaultProps 값이 codegenProps에 전달된다", () => {
    const def: BlockDefinition = {
      meta: {
        name: "OtpInput",
        category: "modules",
        defaultProps: { length: "6", masked: true },
        propSchema: {
          length: { type: "select", label: "길이", default: "6", options: ["4", "6"] },
          masked: { type: "boolean", label: "마스킹", default: true },
        },
      },
      component: () => null as never,
      codegenProps: (p) => ({ ...p, length: Number(p.length) }),
    };
    // props가 비어 있어도 defaultProps의 length/masked가 채워져 codegenProps가 정상 동작
    const code = generateJSX(
      makePage([makeBlock("OtpInput", {})]),
      [], {}, [], [def],
    );
    expect(code).toContain("length={6}");
    expect(code).toContain("masked");
  });
});

// ── 6. children JSXExpr ───────────────────────────────────────────────────────

describe("generateJSX — children JSXExpr", () => {
  it("codegenProps가 children으로 반환한 JSXExpr은 raw로 삽입된다", () => {
    const def: BlockDefinition = {
      meta: {
        name: "Container",
        category: "modules",
        defaultProps: {},
        propSchema: {},
      },
      component: () => null as never,
      codegenProps: () => ({
        children: { __jsx: '<span className="x">안녕</span>' },
      }),
    };
    const code = generateJSX(
      makePage([makeBlock("Container", {})]),
      [], {}, [], [def],
    );
    expect(code).toContain('<Container><span className="x">안녕</span></Container>');
  });
});

// ── 8. LayoutTemplate codegenProps ────────────────────────────────────────────

describe("generateJSX — LayoutTemplate codegenProps", () => {
  it("LayoutTemplate.codegenProps가 정의되면 layoutProps를 변환해 출력한다", () => {
    const layout = {
      id: "home",
      label: "Home",
      componentName: "HomePageLayout",
      defaultProps: { logo: "landmark" },
      propSchema: {
        logo: { type: "icon-picker" as const, label: "로고", default: "landmark" },
      },
      codegenProps: (p: Record<string, unknown>) => {
        const logoName = (p.logo as string | undefined) ?? "";
        const { logo: _logo, ...rest } = p;
        return logoName
          ? { ...rest, logo: { __jsx: `<${(logoName.charAt(0).toUpperCase() + logoName.slice(1))} className="size-4 text-brand" />` } }
          : rest;
      },
    };
    const page: CMSPage = {
      layoutType: "home",
      layoutProps: { logo: "bell" },
      blocks: [],
    };
    const code = generateJSX(page, [layout], {}, [], []);
    expect(code).toContain('logo={<Bell className="size-4 text-brand" />}');
    expect(code).toContain('import { Bell } from "lucide-react";');
  });
});

// ── 9. lucide 화이트리스트 ────────────────────────────────────────────────────

describe("generateJSX — lucide whitelist", () => {
  it("codegenProps JSXExpr에 등장하는 PascalCase 토큰 중 lucide 아이콘만 import에 추가한다", () => {
    const def: BlockDefinition = {
      meta: {
        name: "Card",
        category: "modules",
        defaultProps: {},
        propSchema: {},
      },
      component: () => null as never,
      codegenProps: () => ({
        children: { __jsx: '<CardHeader title="x" icon={<Bell className="size-5" />} />' },
      }),
      codegenImports: ["CardHeader"],
    };
    const code = generateJSX(
      makePage([makeBlock("Card", {})]),
      [], {}, [], [def],
    );
    expect(code).toContain('import { Bell } from "lucide-react";');
    // CardHeader는 컴포넌트 라이브러리 이름이므로 lucide import에서는 제외돼야 한다
    const lucideMatch = code.match(/import \{ ([^}]+) \} from "lucide-react";/);
    expect(lucideMatch).not.toBeNull();
    expect(lucideMatch![1]).not.toContain("CardHeader");
  });
});
