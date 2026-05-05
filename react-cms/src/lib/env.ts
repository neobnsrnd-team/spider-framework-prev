/**
 * @file env.ts
 * @description 서버 사이드 전용 환경변수 모듈.
 * Vite dev 서버(플러그인)에서만 실행되며 클라이언트 번들에 포함되지 않습니다.
 *
 * getter 방식을 사용하여 process.env를 호출 시점에 읽습니다.
 * Vite는 .env 파일을 서버 초기화 중에 process.env에 주입하므로,
 * 모듈 로드 시점(config 평가 단계)이 아닌 첫 DB 접근 시점에 읽어야 합니다.
 *
 * 필수 변수가 누락된 경우 빈 문자열로 조용히 통과하지 않고
 * getter 호출 시점에 즉시 에러를 throw합니다 (fail-fast).
 */

/** 필수 환경변수를 읽고, 누락 시 즉시 에러 throw */
function requireEnv(key: string): string {
  const value = process.env[key];
  if (!value) throw new Error(`[react-cms] 필수 환경변수 "${key}"가 설정되지 않았습니다. .env 파일을 확인하세요.`);
  return value;
}

export const oracleEnv = {
  get user()      { return requireEnv('ORACLE_USER'); },
  get password()  { return requireEnv('ORACLE_PASSWORD'); },
  get host()      { return requireEnv('ORACLE_HOST'); },
  get port()      { return process.env.ORACLE_PORT        ?? '1521'; },  // 기본 Oracle 포트
  get service()   { return process.env.ORACLE_SERVICE     ?? 'XE'; },   // 기본 서비스명
  get schema()    { return requireEnv('ORACLE_SCHEMA'); },
  /** Instant Client 설치 경로. 비어 있으면 PATH에서 자동 탐색. */
  get clientDir() { return process.env.ORACLE_CLIENT_DIR  ?? ''; },
};

// ── CMS 인증 ─────────────────────────────────────────────────────────────────

/**
 * getter로 선언해 호출 시점에 process.env를 읽는다.
 * vite.config.ts의 static import 평가는 loadEnv 실행보다 앞서므로,
 * 모듈 로드 시 상수로 캡처하면 항상 fallback 값이 된다.
 */
export const authEnv = {
  get SPIDER_ADMIN_API_URL() { return process.env.SPIDER_ADMIN_API_URL ?? ''; },
};
