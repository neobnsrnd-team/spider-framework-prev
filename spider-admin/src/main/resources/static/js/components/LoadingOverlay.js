/**
 * @file LoadingOverlay.js
 * @description 전역 dim + 스피너 오버레이 공용 컴포넌트.
 *
 *   - 장시간 요청(이미지 승인 처리 등) 중 사용자 입력 차단 + 진행 상황 시각화.
 *   - body 에 1회 lazy 생성 후 depth 카운팅으로 중첩 show/hide 안전 처리.
 *   - z-index 2000 — Bootstrap modal(1055) 위에서도 동작.
 *
 * @example
 *   LoadingOverlay.show('승인 처리 중...');
 *   fetch(...).finally(() => LoadingOverlay.hide());
 */
window.LoadingOverlay = (function () {
    'use strict';

    var OVERLAY_ID = 'spLoadingOverlay';
    var $overlay = null;
    // 동시에 여러 비동기 요청이 overlay 를 띄우는 경우를 고려해 참조 카운트로 관리.
    var depth = 0;

    /** body 에 오버레이 DOM 을 최초 1회만 생성. */
    function ensureDom() {
        if ($overlay) return;
        $overlay = $(
            '<div id="' + OVERLAY_ID + '" class="loading-overlay d-none" role="status" aria-live="polite">' +
                '<div class="loading-overlay__panel">' +
                    '<div class="spinner-border text-light" aria-hidden="true"></div>' +
                    '<div class="loading-overlay__msg"></div>' +
                '</div>' +
            '</div>'
        ).appendTo(document.body);
    }

    return {
        /**
         * 오버레이를 표시한다. 이미 표시 중이면 depth 만 증가 (메시지는 최근 호출로 갱신).
         * @param {string} [message] 기본 "처리 중..."
         */
        show: function (message) {
            ensureDom();
            depth++;
            $overlay.find('.loading-overlay__msg').text(message || '처리 중...');
            $overlay.removeClass('d-none');
        },

        /** 오버레이를 숨긴다. 중첩 show 가 있는 경우 마지막 hide 에서만 실제로 숨김 처리. */
        hide: function () {
            if (!$overlay) return;
            depth = Math.max(0, depth - 1);
            if (depth === 0) {
                $overlay.addClass('d-none');
            }
        },

        /** 현재 오버레이가 표시 중인지 여부. */
        isVisible: function () {
            return depth > 0;
        },
    };
})();
