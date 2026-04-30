/**
 * @file event-bus.js
 * @description Admin 전역 이벤트 버스 — LRU 탭 캐시 구조에서 탭 간 상태 동기화를 위한 singleton pub/sub 유틸
 *
 * Admin은 탭 전환 시 DOM을 detach/append하여 init()이 재호출되지 않는다.
 * 이 구조에서 탭 간 통신을 위해 이벤트 버스를 사용한다.
 *
 * 이벤트 네이밍 컨벤션: admin:{domain}:{action}
 * 예) admin:emergencyNotice:statusChanged
 *
 * 핸들러 등록 시 반드시 함수 참조를 변수에 보관해야 off()로 제거할 수 있다.
 *
 * @example
 *   // 발행 (emit)
 *   Admin_EventBus.emit('admin:emergencyNotice:statusChanged', { status: 'DEPLOYED' });
 *
 *   // 구독 (subscribe) — 함수 참조를 보관해야 off()로 제거 가능
 *   function handler(payload) { self.loadDeployStatus(); }
 *   Admin_EventBus.on('admin:emergencyNotice:statusChanged', handler);
 *
 *   // 구독 해제 (unsubscribe) — tab:removed 이벤트 수신 시 호출
 *   Admin_EventBus.off('admin:emergencyNotice:statusChanged', handler);
 */
"use strict";

// window에 직접 할당하여 전역 접근 보장 및 ESLint no-unused-vars 오탐 방지
window.Admin_EventBus = (() => {
    /** @type {Map<string, Set<Function>>} 이벤트명 → 핸들러 집합 */
    const _handlers = new Map();

    /**
     * 이벤트 핸들러를 등록한다.
     * Set 기반이므로 동일 함수 참조를 중복 등록해도 한 번만 실행된다.
     *
     * @param {string}   event   - 이벤트명 (예: 'admin:emergencyNotice:statusChanged')
     * @param {Function} handler - 이벤트 수신 시 실행할 함수
     */
    function on(event, handler) {
        if (!_handlers.has(event)) _handlers.set(event, new Set());
        _handlers.get(event).add(handler);
    }

    /**
     * 등록된 핸들러를 제거한다.
     * on()에 넘긴 함수 참조와 동일한 참조여야 제거된다.
     *
     * @param {string}   event   - 이벤트명
     * @param {Function} handler - 제거할 핸들러 함수
     */
    function off(event, handler) {
        _handlers.get(event)?.delete(handler);
    }

    /**
     * 이벤트를 발행하고 등록된 모든 핸들러에 payload를 전달한다.
     * 핸들러 오류는 격리하여 다른 핸들러 실행에 영향을 주지 않는다.
     *
     * @param {string} event   - 이벤트명
     * @param {*}      payload - 핸들러에 전달할 데이터
     */
    function emit(event, payload) {
        _handlers.get(event)?.forEach((handler) => {
            try {
                handler(payload);
            } catch (err) {
                // 핸들러 오류는 격리 — 콘솔에만 출력하고 다른 핸들러는 계속 실행
                console.error(`[Admin_EventBus] 핸들러 오류 (${event}):`, err);
            }
        });
    }

    return { on, off, emit };
})();
