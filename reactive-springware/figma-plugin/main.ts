/// <reference types="@figma/plugin-typings" />
/**
 * @file main.ts
 * @description Figma 플러그인 진입점. manifest.json의 커맨드에 따라 실행을 위임한다.
 *
 * - createComponents : 컴포넌트 전체 생성 (createComponents.ts)
 * - createIcons      : Lucide 아이콘 생성 (createIcons.ts)
 * - createVariables  : 디자인 변수 등록 (createVariables.ts)
 *
 * 각 커맨드 실행 중에는 figma.notify()로 진행 상태를 표시한다.
 * createComponents는 단계별 step 이름을 실시간으로 갱신하고,
 * createIcons / createVariables는 단순 "실행 중..." 토스트를 유지한다.
 */

import { createComponents, getLastStep } from './commands/createComponents';
import { createIcons }                   from './commands/createIcons';
import { createVariables }               from './commands/createVariables';

(async () => {
  if (figma.command === 'createComponents') {
    let toast: NotificationHandler | null = null;

    const message = await createComponents((step) => {
      /* 이전 토스트 취소 후 새 단계명으로 갱신 — await 사이 yield 시점에 Figma UI에 반영됨 */
      toast?.cancel();
      toast = figma.notify(`⏳ 생성 중: ${step}`, { timeout: Infinity });
    });

    /* TS 5.9 클로저 내 let 재할당 추적으로 await 이후 never로 좁혀짐 — 선언 타입으로 재단언 */
    (toast as NotificationHandler | null)?.cancel();
    figma.closePlugin(message);
    return;
  }

  if (figma.command === 'createIcons') {
    const toast = figma.notify('⏳ 아이콘 생성 중...', { timeout: Infinity });
    const message = await createIcons();
    toast.cancel();
    figma.closePlugin(message);
    return;
  }

  if (figma.command === 'createVariables') {
    const toast = figma.notify('⏳ 변수 등록 중...', { timeout: Infinity });
    const message = await createVariables();
    toast.cancel();
    figma.closePlugin(message);
    return;
  }
})().catch((err) => {
  /* getLastStep()으로 마지막 실행 단계를 포함해 오류 위치를 표시한다 */
  const step = getLastStep();
  figma.closePlugin(`❌ 오류 in [${step}]: ${err instanceof Error ? err.message : String(err)}`);
});
