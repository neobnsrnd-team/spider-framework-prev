// ContentBuilderRuntimeOptions 타입 확장 — 라이브러리 타입 누락 항목 보완
declare global {
    interface ContentBuilderRuntimeOptions {
        onReInit?: () => void;
    }
    interface Window {
        __spwEditor?: boolean;
        builderReinit?: () => void;
        /** ContentBuilderRuntime 인스턴스 — EditClient.tsx에서 전역 노출 */
        builderRuntime?: { reinitialize: (container?: Document | HTMLElement) => Promise<number> };
    }
}

export {};
