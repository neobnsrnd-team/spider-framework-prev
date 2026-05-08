/**
 * @file savePath.ts
 * @description 페이지 저장 경로(savePath) 공용 검증 유틸.
 *
 * 클라이언트(SavePageModal)와 서버(cmsBankPlugin) 양쪽에서 동일한 규칙을 적용하기 위해
 * 한 곳에 모아둔다. 둘 중 한쪽 검증이 누락되면 보안 우회 가능하므로,
 * 새 검증 규칙을 추가할 때는 반드시 이 파일에서 처리한다.
 */

/**
 * 저장 경로가 상대 경로인지 검증한다.
 *
 * 거부 패턴:
 *   - 빈 문자열 / 공백
 *   - POSIX 절대경로 ('/foo')
 *   - Windows 드라이브 절대경로 ('C:\foo' 또는 'C:/foo')
 *   - UNC 경로 ('\\\\server\\share') — Windows에서 path.isAbsolute가 잡지 못하는 형태도 정규식이 차단
 *
 * @param input 검증할 경로 문자열
 * @returns 통과 시 null, 거부 시 사용자에게 보여줄 한국어 에러 메시지
 *
 * @example
 *   validateRelativeSavePath("src/pages")           // → null
 *   validateRelativeSavePath("../demo/front/pages") // → null (../ 허용 — monorepo 인접 패키지 접근 케이스)
 *   validateRelativeSavePath("/etc/passwd")         // → "저장 위치는 상대 경로여야 합니다."
 *   validateRelativeSavePath("C:\\Windows")         // → "저장 위치는 상대 경로여야 합니다."
 *   validateRelativeSavePath("")                    // → "저장 위치를 입력하세요."
 */
export function validateRelativeSavePath(input: string | undefined | null): string | null {
  if (!input || !input.trim()) return "저장 위치를 입력하세요.";
  // POSIX('/foo'), Windows('C:\foo' 또는 'C:/foo'), UNC('\\\\server') 모두 거부
  if (/^([a-zA-Z]:[\\/]|[\\/])/.test(input)) return "저장 위치는 상대 경로여야 합니다.";
  return null;
}
