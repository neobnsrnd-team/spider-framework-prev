/// <reference types="@figma/plugin-typings" />
/**
 * @file main.ts
 * @description Figma 플러그인 진입점. manifest.json의 커맨드에 따라 실행을 위임한다.
 *
 * - createComponents : 컴포넌트 전체 생성 (createComponents.ts)
 * - createIcons      : Lucide 아이콘 생성 (createIcons.ts)
 * - createVariables  : 디자인 변수 등록 (createVariables.ts)
 */

import { createComponents, getLastStep } from './createComponents';
import { createIcons }                   from './createIcons';
import { createVariables }               from './createVariables';

(async () => {
  if (figma.command === 'createComponents') {
    const message = await createComponents();
    figma.closePlugin(message);
    return;
  }

  if (figma.command === 'createIcons') {
    const message = await createIcons();
    figma.closePlugin(message);
    return;
  }

  if (figma.command === 'createVariables') {
    const message = await createVariables();
    figma.closePlugin(message);
    return;
  }
})().catch((err) => {
  /* getLastStep()으로 마지막 실행 단계를 포함해 오류 위치를 표시한다 */
  const step = getLastStep();
  figma.closePlugin(`❌ 오류 in [${step}]: ${err instanceof Error ? err.message : String(err)}`);
});
