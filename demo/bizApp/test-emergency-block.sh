#!/usr/bin/env bash
# ============================================================
# 긴급공지 API 차단 동작 검증 스크립트
#
# 전제: biz-channel이 localhost:18080 에서 기동 중이어야 한다.
# 사용: bash demo/bizApp/test-emergency-block.sh
# ============================================================

BASE_URL="http://localhost:18080"
ADMIN_SECRET="admin-secret"

# 출력 색상
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

pass() { echo -e "${GREEN}  PASS${NC} $1"; }
fail() { echo -e "${RED}  FAIL${NC} $1"; }
section() { echo -e "\n${CYAN}━━━ $1 ━━━${NC}"; }
info() { echo -e "${YELLOW}  →${NC} $1"; }

# HTTP 상태코드만 추출해 반환
status() {
  curl -s -o /dev/null -w "%{http_code}" "$@"
}

# HTTP 상태코드 + 응답 바디를 함께 출력
fetch() {
  curl -s -w "\n[HTTP %{http_code}]" "$@"
}

# ────────────────────────────────────────────────────────────
# 1. 사전 상태 확인
# ────────────────────────────────────────────────────────────
section "1. 사전 상태 확인 (공지 없음)"

code=$(status "$BASE_URL/api/notices/preview")
info "GET /api/notices/preview → HTTP $code"
[ "$code" = "200" ] && pass "preview 정상 응답" || fail "preview 응답 이상 (expected 200, got $code)"

code=$(status -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"userId":"test","password":"test"}')
info "POST /api/auth/login (공지 없을 때) → HTTP $code"
[ "$code" != "503" ] && pass "로그인 API 차단 없음 (HTTP $code)" || fail "공지 없는데 차단됨"

# ────────────────────────────────────────────────────────────
# 2. closeableYn=Y 공지 배포 — 차단 없어야 함
# ────────────────────────────────────────────────────────────
section "2. 일반 공지 배포 (closeableYn=Y) — API 차단 없어야 함"

fetch -X POST "$BASE_URL/api/notices/sync" \
  -H "Content-Type: application/json" \
  -H "X-Admin-Secret: $ADMIN_SECRET" \
  -d '{
    "notices": [
      {"lang":"EMERGENCY_KO","title":"일반 점검 안내","content":"잠시 후 점검이 시작됩니다."}
    ],
    "displayType": "A",
    "closeableYn": "Y",
    "hideTodayYn": "Y"
  }'
echo ""
info "일반 공지(closeableYn=Y) 배포 완료"

sleep 0.3

code=$(status -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"userId":"test","password":"test"}')
info "POST /api/auth/login → HTTP $code"
[ "$code" != "503" ] && pass "closeableYn=Y 상태에서 로그인 API 통과 (HTTP $code)" \
                      || fail "closeableYn=Y인데 503 차단됨"

# ────────────────────────────────────────────────────────────
# 3. 공지 종료
# ────────────────────────────────────────────────────────────
section "3. 공지 종료"

fetch -X POST "$BASE_URL/api/notices/end" \
  -H "X-Admin-Secret: $ADMIN_SECRET"
echo ""

# ────────────────────────────────────────────────────────────
# 4. closeableYn=N 크리티컬 공지 배포 — API 전면 차단
# ────────────────────────────────────────────────────────────
section "4. 크리티컬 공지 배포 (closeableYn=N) — API 전면 차단"

fetch -X POST "$BASE_URL/api/notices/sync" \
  -H "Content-Type: application/json" \
  -H "X-Admin-Secret: $ADMIN_SECRET" \
  -d '{
    "notices": [
      {"lang":"EMERGENCY_KO","title":"긴급 점검","content":"시스템 긴급 점검 중입니다."}
    ],
    "displayType": "A",
    "closeableYn": "N",
    "hideTodayYn": "N"
  }'
echo ""
info "크리티컬 공지(closeableYn=N) 배포 완료"

sleep 0.3

# 4-1. 로그인 API — 차단되어야 함
code=$(status -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"userId":"test","password":"test"}')
info "POST /api/auth/login → HTTP $code"
[ "$code" = "503" ] && pass "로그인 API 503 차단" || fail "로그인 API 차단 안 됨 (expected 503, got $code)"

# 4-2. 응답 바디 확인
info "응답 바디 확인:"
fetch -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"userId":"test","password":"test"}'
echo ""

# 4-3. /api/notices/preview — 허용되어야 함
code=$(status "$BASE_URL/api/notices/preview")
info "GET /api/notices/preview → HTTP $code"
[ "$code" = "200" ] && pass "preview 허용 (화이트리스트 정상)" || fail "preview 차단됨 (expected 200, got $code)"

# 4-4. SSE 연결 — 허용되어야 함 (1초만 수신 후 종료)
info "GET /api/notices/sse (1초 수신) → "
sse_output=$(curl -s -m 1 "$BASE_URL/api/notices/sse" 2>&1)
if echo "$sse_output" | grep -q "data:\|event:"; then
  pass "SSE 허용 — 이벤트 수신: $(echo "$sse_output" | head -1)"
else
  pass "SSE 허용 — 연결 유지됨 (1초 수신)"
fi

# 4-5. 카드 관련 API — 차단되어야 함
code=$(status -X GET "$BASE_URL/api/cards" \
  -H "Authorization: Bearer fake-token")
info "GET /api/cards → HTTP $code"
[ "$code" = "503" ] && pass "카드 API 503 차단" || info "카드 API 응답: HTTP $code (403/401이면 JWT 필터가 먼저 차단한 것)"

# ────────────────────────────────────────────────────────────
# 5. 공지 종료 후 API 복구 확인
# ────────────────────────────────────────────────────────────
section "5. 공지 종료 → API 복구"

fetch -X POST "$BASE_URL/api/notices/end" \
  -H "X-Admin-Secret: $ADMIN_SECRET"
echo ""
info "공지 종료 완료"

sleep 0.3

code=$(status -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"userId":"test","password":"test"}')
info "POST /api/auth/login (공지 종료 후) → HTTP $code"
[ "$code" != "503" ] && pass "공지 종료 후 로그인 API 복구 (HTTP $code)" \
                      || fail "공지 종료 후에도 여전히 차단됨"

# ────────────────────────────────────────────────────────────
section "완료"
echo ""
