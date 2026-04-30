/**
 * HtmlUtils - 공통 HTML 이스케이프 유틸리티
 * XSS 방지를 위한 전역 유틸리티 모듈
 */
window.HtmlUtils = window.HtmlUtils || {};

(function (HtmlUtils) {
    const MAP = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;',
        '/': '&#x2F;'
    };

    /**
     * HTML 특수문자를 이스케이프합니다.
     * @param {*} text - 이스케이프할 값 (null/undefined 허용)
     * @returns {string} 이스케이프된 문자열
     */
    HtmlUtils.escape = function (text) {
        if (text === null || text === undefined) return '';
        return String(text).replace(/[&<>"'/]/g, function (m) { return MAP[m]; });
    };
})(window.HtmlUtils);
