/**
 * @file current-user.test.ts
 * @description current-user.ts 핵심 보안 함수 단위 테스트.
 *
 * 테스트 대상:
 *   - 순수 함수: hasAuthority, canReadCms, canWriteCms, canAdminScreen
 *   - 비동기 함수: getCurrentUser (AUTH_BYPASS 분기, API 성공/실패, 폴백)
 *   - 권한 가드: requireCmsRead, requireCmsWrite
 */
import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import {
  hasAuthority,
  canReadCms,
  canWriteCms,
  canAdminScreen,
  getCurrentUser,
  requireCmsRead,
  requireCmsWrite,
  UnauthorizedError,
  CMS_ROLE,
} from "../current-user";

// ── 픽스처 ────────────────────────────────────────────────────────────────────

const userWithReadWrite = { authorities: ["REACT_CMS:R", "REACT_CMS:W"] };
const userWithReadOnly = { authorities: ["REACT_CMS:R"] };
const userWithWriteOnly = { authorities: ["REACT_CMS:W"] };
const userWithNone = { authorities: [] as string[] };

// ── 순수 함수 ─────────────────────────────────────────────────────────────────

describe("hasAuthority", () => {
  it("권한 목록에 해당 권한이 있으면 true", () => {
    expect(hasAuthority(userWithReadWrite, "REACT_CMS:R")).toBe(true);
    expect(hasAuthority(userWithReadWrite, "REACT_CMS:W")).toBe(true);
  });

  it("권한 목록에 해당 권한이 없으면 false", () => {
    expect(hasAuthority(userWithReadOnly, "REACT_CMS:W")).toBe(false);
    expect(hasAuthority(userWithNone, "REACT_CMS:R")).toBe(false);
  });
});

describe("canReadCms", () => {
  it("REACT_CMS:R 권한이 있으면 true", () => {
    expect(canReadCms(userWithReadOnly)).toBe(true);
  });

  it("REACT_CMS:W만 있어도 읽기 허용 (상위 권한 포함)", () => {
    expect(canReadCms(userWithWriteOnly)).toBe(true);
  });

  it("권한이 없으면 false", () => {
    expect(canReadCms(userWithNone)).toBe(false);
  });
});

describe("canWriteCms", () => {
  it("REACT_CMS:W 권한이 있으면 true", () => {
    expect(canWriteCms(userWithWriteOnly)).toBe(true);
    expect(canWriteCms(userWithReadWrite)).toBe(true);
  });

  it("REACT_CMS:R만 있으면 false", () => {
    expect(canWriteCms(userWithReadOnly)).toBe(false);
  });

  it("권한이 없으면 false", () => {
    expect(canWriteCms(userWithNone)).toBe(false);
  });
});

describe("canAdminScreen", () => {
  it("ADMIN 역할이면 true", () => {
    expect(canAdminScreen({ roleId: CMS_ROLE.SPIDER_ADMIN })).toBe(true);
  });

  it("react-adm 역할이면 true", () => {
    expect(canAdminScreen({ roleId: CMS_ROLE.CMS_ADMIN })).toBe(true);
  });

  it("react-user 역할이면 false", () => {
    expect(canAdminScreen({ roleId: CMS_ROLE.USER })).toBe(false);
  });

  it("guest 역할이면 false", () => {
    expect(canAdminScreen({ roleId: "guest" })).toBe(false);
  });
});

// ── getCurrentUser ────────────────────────────────────────────────────────────

describe("getCurrentUser — AUTH_BYPASS=true", () => {
  beforeEach(() => {
    process.env.AUTH_BYPASS = "true";
    delete process.env.SPIDER_ADMIN_API_URL;
  });

  afterEach(() => {
    delete process.env.AUTH_BYPASS;
  });

  it("cms_bypass_role=react-adm 쿠키가 있으면 관리자 반환", async () => {
    const user = await getCurrentUser("cms_bypass_role=react-adm");
    expect(user.roleId).toBe("react-adm");
    expect(user.authorities).toContain("REACT_CMS:W");
  });

  it("쿠키가 없으면 일반 사용자 반환", async () => {
    const user = await getCurrentUser("");
    expect(user.roleId).toBe("react-user");
    expect(user.authorities).toContain("REACT_CMS:R");
  });

  it("다른 값의 쿠키가 있으면 일반 사용자 반환", async () => {
    const user = await getCurrentUser("cms_bypass_role=other-role");
    expect(user.roleId).toBe("react-user");
  });

  it("여러 쿠키 중 cms_bypass_role=react-adm이 있으면 관리자 반환", async () => {
    const user = await getCurrentUser(
      "session=abc; cms_bypass_role=react-adm; theme=dark",
    );
    expect(user.roleId).toBe("react-adm");
  });
});

describe("getCurrentUser — AUTH_BYPASS=false", () => {
  beforeEach(() => {
    process.env.AUTH_BYPASS = "false";
  });

  afterEach(() => {
    delete process.env.AUTH_BYPASS;
    delete process.env.SPIDER_ADMIN_API_URL;
    vi.unstubAllGlobals();
  });

  it("SPIDER_ADMIN_API_URL 미설정 시 GUEST 반환", async () => {
    delete process.env.SPIDER_ADMIN_API_URL;
    const user = await getCurrentUser("");
    expect(user.roleId).toBe("guest");
    expect(user.authorities).toHaveLength(0);
  });

  it("Spider Admin API 정상 응답 시 사용자 정보 반환", async () => {
    process.env.SPIDER_ADMIN_API_URL = "http://localhost:8080";
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          success: true,
          data: {
            userId: "u1",
            userName: "홍길동",
            roleId: "react-adm",
            authorities: ["REACT_CMS:R", "REACT_CMS:W"],
          },
        }),
      }),
    );

    const user = await getCurrentUser("session=token123");
    expect(user.userId).toBe("u1");
    expect(user.roleId).toBe("react-adm");
    expect(user.authorities).toContain("REACT_CMS:W");
  });

  it("authorities 필드 없으면 빈 배열로 처리", async () => {
    process.env.SPIDER_ADMIN_API_URL = "http://localhost:8080";
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          success: true,
          data: { userId: "u2", userName: "테스트", roleId: "react-user" },
        }),
      }),
    );

    const user = await getCurrentUser("");
    expect(user.authorities).toEqual([]);
  });

  it("API 응답이 ok=false이면 GUEST 반환", async () => {
    process.env.SPIDER_ADMIN_API_URL = "http://localhost:8080";
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: false }));

    const user = await getCurrentUser("");
    expect(user.roleId).toBe("guest");
  });

  it("API 응답 success=false이면 GUEST 반환", async () => {
    process.env.SPIDER_ADMIN_API_URL = "http://localhost:8080";
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({ success: false }),
      }),
    );

    const user = await getCurrentUser("");
    expect(user.roleId).toBe("guest");
  });

  it("네트워크 오류 발생 시 GUEST 반환", async () => {
    process.env.SPIDER_ADMIN_API_URL = "http://localhost:8080";
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValue(new Error("Network error")),
    );

    const user = await getCurrentUser("");
    expect(user.roleId).toBe("guest");
  });

  it("요청 시 Cookie 헤더를 Spider Admin으로 전달", async () => {
    process.env.SPIDER_ADMIN_API_URL = "http://localhost:8080";
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          userId: "u3",
          userName: "테스트",
          roleId: "react-user",
          authorities: [],
        },
      }),
    });
    vi.stubGlobal("fetch", mockFetch);

    await getCurrentUser("session=mysession");

    expect(mockFetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/auth/me",
      expect.objectContaining({ headers: { cookie: "session=mysession" } }),
    );
  });

  it("SPIDER_ADMIN_API_URL 말미 슬래시를 제거하고 URL을 정규화", async () => {
    process.env.SPIDER_ADMIN_API_URL = "http://localhost:8080/";
    const mockFetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        success: true,
        data: {
          userId: "u4",
          userName: "테스트",
          roleId: "react-user",
          authorities: [],
        },
      }),
    });
    vi.stubGlobal("fetch", mockFetch);

    await getCurrentUser("");

    // 말미 슬래시가 제거되어 이중 슬래시 없이 호출되어야 함
    expect(mockFetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/auth/me",
      expect.anything(),
    );
  });

  it("타임아웃(AbortError) 발생 시 GUEST 반환", async () => {
    process.env.SPIDER_ADMIN_API_URL = "http://localhost:8080";
    const abortError = new DOMException(
      "The operation was aborted.",
      "AbortError",
    );
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(abortError));

    const user = await getCurrentUser("");
    expect(user.roleId).toBe("guest");
  });
});

// ── requireCmsRead / requireCmsWrite ─────────────────────────────────────────

describe("requireCmsWrite", () => {
  afterEach(() => {
    delete process.env.AUTH_BYPASS;
    vi.unstubAllGlobals();
  });

  it("REACT_CMS:W 권한이 있으면 CurrentUser 반환", async () => {
    process.env.AUTH_BYPASS = "true";
    const user = await requireCmsWrite("cms_bypass_role=react-adm");
    expect(user.roleId).toBe("react-adm");
  });

  it("권한이 없으면 UnauthorizedError throw", async () => {
    process.env.AUTH_BYPASS = "false";
    delete process.env.SPIDER_ADMIN_API_URL; // → GUEST 반환
    await expect(requireCmsWrite("")).rejects.toThrow(UnauthorizedError);
  });
});

describe("requireCmsRead", () => {
  afterEach(() => {
    delete process.env.AUTH_BYPASS;
    vi.unstubAllGlobals();
  });

  it("REACT_CMS:R 권한이 있으면 CurrentUser 반환", async () => {
    process.env.AUTH_BYPASS = "true";
    const user = await requireCmsRead("");
    expect(user.authorities).toContain("REACT_CMS:R");
  });

  it("권한이 없으면 UnauthorizedError throw", async () => {
    process.env.AUTH_BYPASS = "false";
    delete process.env.SPIDER_ADMIN_API_URL; // → GUEST 반환
    await expect(requireCmsRead("")).rejects.toThrow(UnauthorizedError);
  });
});
