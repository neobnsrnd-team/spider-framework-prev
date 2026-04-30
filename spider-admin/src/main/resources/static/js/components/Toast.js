/**
 * Toast Component — 통합 알림 시스템
 * Bootstrap 5 Toast + 확인 대화상자 (browser alert/confirm 대체)
 */
window.Toast = (function () {
    'use strict';

    var CONTAINER_ID = 'spToastContainer';
    var CONFIRM_MODAL_ID = 'spConfirmModal';

    function ensureContainer() {
        if (!document.getElementById(CONTAINER_ID)) {
            document.body.insertAdjacentHTML('beforeend',
                '<div id="' + CONTAINER_ID + '" class="toast-container position-fixed top-0 end-0 p-3 sp-toast-z"></div>'
            );
        }
        return document.getElementById(CONTAINER_ID);
    }

    function showToast(message, type) {
        var container = ensureContainer();

        var iconMap = {
            success: 'bi-check-circle-fill',
            danger: 'bi-exclamation-circle-fill',
            warning: 'bi-exclamation-triangle-fill',
            info: 'bi-info-circle-fill'
        };

        var wrapper = document.createElement('div');
        wrapper.innerHTML =
            '<div class="toast align-items-center text-bg-' + type + ' border-0" role="alert" ' +
            'aria-live="assertive" aria-atomic="true" data-bs-delay="3000">' +
                '<div class="d-flex">' +
                    '<div class="toast-body">' +
                        '<i class="bi ' + (iconMap[type] || iconMap.info) + ' me-2"></i>' +
                        '<span></span>' +
                    '</div>' +
                    '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>' +
                '</div>' +
            '</div>';

        var toastEl = wrapper.firstElementChild;
        toastEl.querySelector('span').textContent = message;
        container.appendChild(toastEl);

        toastEl.addEventListener('hidden.bs.toast', function () {
            toastEl.remove();
        });

        new bootstrap.Toast(toastEl).show();
    }

    function ensureConfirmModal() {
        if (document.getElementById(CONFIRM_MODAL_ID)) return;

        document.body.insertAdjacentHTML('beforeend',
            '<div class="modal fade sp-modal-stacked-top" id="' + CONFIRM_MODAL_ID + '" tabindex="-1" aria-hidden="true">' +
                '<div class="modal-dialog modal-dialog-centered modal-sm">' +
                    '<div class="modal-content">' +
                        '<div class="modal-header py-2">' +
                            '<h6 class="modal-title">확인</h6>' +
                            '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>' +
                        '</div>' +
                        '<div class="modal-body text-center py-3">' +
                            '<p id="' + CONFIRM_MODAL_ID + 'Msg" class="mb-0"></p>' +
                        '</div>' +
                        '<div class="modal-footer justify-content-center py-2">' +
                            '<button type="button" class="btn btn-secondary btn-sm px-3" data-bs-dismiss="modal">취소</button>' +
                            '<button type="button" class="btn btn-primary btn-sm px-3" id="' + CONFIRM_MODAL_ID + 'Ok">확인</button>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>'
        );
    }

    return {
        success: function (msg) { showToast(msg, 'success'); },
        error: function (msg) { showToast(msg, 'danger'); },
        warning: function (msg) { showToast(msg, 'warning'); },
        info: function (msg) { showToast(msg, 'info'); },

        /**
         * 확인 대화상자 (confirm 대체)
         * @param {string} msg - 메시지
         * @param {function} onConfirm - 확인 시 콜백
         */
        confirm: function (msg, onConfirm) {
            ensureConfirmModal();

            var msgEl = document.getElementById(CONFIRM_MODAL_ID + 'Msg');
            msgEl.textContent = msg;

            var modalEl = document.getElementById(CONFIRM_MODAL_ID);
            var modal = bootstrap.Modal.getOrCreateInstance(modalEl);

            var okBtn = document.getElementById(CONFIRM_MODAL_ID + 'Ok');
            var newBtn = okBtn.cloneNode(true);
            okBtn.parentNode.replaceChild(newBtn, okBtn);
            newBtn.id = CONFIRM_MODAL_ID + 'Ok';

            newBtn.addEventListener('click', function () {
                modal.hide();
                if (onConfirm) onConfirm();
            });

            modal.show();
        }
    };
})();
