/**
 * @file createDivider.ts
 * @description Figma Divider 컴포넌트 생성.
 * 텍스트 없이 순수 수평선만 렌더링하는 단일 컴포넌트.
 * DividerWithLabel(텍스트 포함)과 역할이 다름.
 * 컴포넌트 이름: "Divider"
 */

import { COLOR, COLOR_VAR } from '../../../tokens';
import { createComponent, setFillWithVar } from '../../../helpers';

export async function createDivider(): Promise<ComponentNode> {
  const comp = createComponent('Divider');
  /* 1px 높이 수평선 — border-border-subtle 색상 */
  comp.resize(390, 1);
  comp.layoutMode = 'NONE';
  await setFillWithVar(comp, COLOR_VAR.borderSubtle, COLOR.borderSubtle);
  return comp;
}
