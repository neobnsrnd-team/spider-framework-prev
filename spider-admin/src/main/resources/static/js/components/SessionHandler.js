/**
 * SessionHandler — 전역 AJAX 세션 만료(401) / 권한 부족(403) 처리
 *
 * jQuery 로드 직후, 다른 컴포넌트보다 먼저 로드해야 한다.
 * - 401: Bootstrap 5 Modal (blocking) — 세션 만료 시 재로그인 유도
 * - 403: Bootstrap 5 Toast (non-blocking) — 권한 부족 안내
 *
 * 사용법:
 *   <script src="jquery.js"></script>
 *   <script src="/js/components/SessionHandler.js"></script>
 */
(function () {
    'use strict';

    let _redirecting = false;

    // ── 401 모달 HTML (최초 1회 DOM에 삽입) ─────────────────────────
    const MODAL_HTML = `
        <div class="modal fade" id="sessionAlertModal" data-bs-backdrop="static"
             data-bs-keyboard="false" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-sm">
                <div class="modal-content">
                    <div class="modal-header py-2 bg-warning bg-opacity-10 border-warning">
                        <h6 class="modal-title" id="sessionAlertTitle"></h6>
                    </div>
                    <div class="modal-body text-center py-4">
                        <i class="bi bi-clock text-warning mb-3 d-block fs-1"></i>
                        <p id="sessionAlertMessage" class="mb-0"></p>
                    </div>
                    <div class="modal-footer justify-content-center py-2">
                        <button type="button" class="btn btn-warning btn-sm px-4" id="sessionAlertBtn">확인</button>
                    </div>
                </div>
            </div>
        </div>`;

    // ── 403 토스트 컨테이너 + 템플릿 ────────────────────────────────
    const TOAST_CONTAINER_HTML = `
        <div id="sessionToastContainer" class="toast-container position-fixed top-0 end-0 p-3"
             class="sp-session-overlay"></div>`;

    const TOAST_TEMPLATE = `
        <div class="toast align-items-center text-bg-danger border-0" role="alert"
             aria-live="assertive" aria-atomic="true" data-bs-delay="3000">
            <div class="d-flex">
                <div class="toast-body">
                    <i class="bi bi-slash-circle me-2"></i><span class="toast-msg"></span>
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto"
                        data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>`;

    function ensureModal() {
        if (!document.getElementById('sessionAlertModal')) {
            document.body.insertAdjacentHTML('beforeend', MODAL_HTML);
        }
    }

    function ensureToastContainer() {
        if (!document.getElementById('sessionToastContainer')) {
            document.body.insertAdjacentHTML('beforeend', TOAST_CONTAINER_HTML);
        }
    }

    /**
     * 401 세션 만료 — blocking 모달
     */
    function showSessionExpiredModal() {
        ensureModal();

        document.getElementById('sessionAlertTitle').textContent = '세션 만료';
        document.getElementById('sessionAlertMessage').textContent = '세션이 만료되었습니다. 다시 로그인해 주세요.';

        const modalEl = document.getElementById('sessionAlertModal');
        modalEl.classList.add('sp-session-overlay');
        const modal = bootstrap.Modal.getOrCreateInstance(modalEl);

        const btn = document.getElementById('sessionAlertBtn');
        const newBtn = btn.cloneNode(true);
        btn.parentNode.replaceChild(newBtn, btn);
        newBtn.addEventListener('click', function () {
            modal.hide();
            window.location.href = '/login?expired=true';
        });

        modalEl.addEventListener('shown.bs.modal', function () {
            const backdrops = document.querySelectorAll('.modal-backdrop');
            const last = backdrops[backdrops.length - 1];
            if (last) last.classList.add('sp-z-under');
        }, { once: true });

        modal.show();
    }

    /**
     * 403 권한 부족 — non-blocking 토스트 (3초 후 자동 닫힘)
     */
    function showAccessDeniedToast() {
        ensureToastContainer();

        const container = document.getElementById('sessionToastContainer');
        const wrapper = document.createElement('div');
        wrapper.innerHTML = TOAST_TEMPLATE.trim();
        const toastEl = wrapper.firstElementChild;
        toastEl.querySelector('.toast-msg').textContent = '접근 권한이 없습니다.';
        container.appendChild(toastEl);

        const toast = new bootstrap.Toast(toastEl);
        toastEl.addEventListener('hidden.bs.toast', function () {
            toastEl.remove();
        });
        toast.show();
    }

    /**
     * 응답이 JSON이 아닌 경우(HTML 에러 페이지 등) 민감 정보 노출을 방지하기 위해
     * xhr.responseJSON을 안전한 일반 메시지 객체로 정규화한다.
     */
    function sanitizeNonJsonResponse(xhr) {
        const contentType = xhr.getResponseHeader('Content-Type') || '';
        if (contentType.indexOf('application/json') === -1) {
            xhr.responseJSON = {
                success: false,
                message: '요청 처리 중 오류가 발생했습니다.',
                data: null,
                code: xhr.status
            };
        }
    }

    // ── $.ajax 래핑 ─────────────────────────────────────────────────
    const _origAjax = $.ajax;
    $.ajax = function (opts) {
        const origError = opts.error;
        opts.error = function (xhr, status, err) {
            if (xhr.status === 401 && !_redirecting) {
                _redirecting = true;
                showSessionExpiredModal();
                return;
            }
            if (xhr.status === 403) {
                showAccessDeniedToast();
                if (origError) origError.call(this, xhr, status, err);
                return;
            }
            sanitizeNonJsonResponse(xhr);
            if (origError) origError.call(this, xhr, status, err);
        };
        return _origAjax.call(this, opts);
    };
})();
