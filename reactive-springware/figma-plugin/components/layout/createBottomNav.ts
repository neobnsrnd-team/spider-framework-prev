/**
 * @file createBottomNav.ts
 * @description Figma BottomNav 컴포넌트 생성.
 * 5개 탭에 Lucide 아이콘(Wallet, ShoppingBag, Home, CreditCard, MessageSquare)을 사용한다.
 *
 * 컴포넌트 이름: "BottomNav"
 */

import { BRAND, COLOR, SPACING, FONT_SIZE } from '../../utils/tokens';
import { createComponent, setAutoLayout, setPadding, setFill, clearFill, addText } from '../../utils/helpers';
import { createIcon, type IconName } from '../../utils/icons';

const NAV_WIDTH  = 390;
const NAV_HEIGHT = 60;

const TAB_ITEMS: { label: string; icon: IconName }[] = [
  { label: '자산', icon: 'Wallet'        },
  { label: '상품', icon: 'ShoppingBag'   },
  { label: '홈',   icon: 'Home'          },
  { label: '카드', icon: 'CreditCard'    },
  { label: '챗봇', icon: 'MessageSquare' },
];

export async function createBottomNav(): Promise<ComponentNode> {
  const comp = createComponent('BottomNav');
  setAutoLayout(comp, 'HORIZONTAL', 0);
  setPadding(comp, SPACING.sm, SPACING.standard, SPACING.sm, SPACING.standard);
  comp.resize(NAV_WIDTH, NAV_HEIGHT);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'FIXED';
  comp.primaryAxisAlignItems = 'SPACE_BETWEEN';
  comp.counterAxisAlignItems = 'CENTER';
  setFill(comp, COLOR.surface);

  /* forEach(async)는 내부 await를 기다리지 않으므로 for...of 로 순회 */
  for (const [i, { label, icon }] of TAB_ITEMS.entries()) {
    const isActive = i === 2; // 홈(중앙)이 기본 활성

    const tab = figma.createFrame();
    tab.name = isActive ? 'TabItem (Active)' : 'TabItem';
    setAutoLayout(tab, 'VERTICAL', SPACING.xs);
    setPadding(tab, SPACING.xs, SPACING.md);
    tab.primaryAxisSizingMode = 'AUTO';
    tab.counterAxisSizingMode = 'AUTO';
    tab.primaryAxisAlignItems = 'CENTER';
    tab.counterAxisAlignItems = 'CENTER';
    clearFill(tab);

    /* Lucide 아이콘 — 활성 탭은 브랜드 색상, 비활성은 muted */
    const iconSize = isActive ? 24 : 20;
    tab.appendChild(createIcon(icon, iconSize, isActive ? BRAND.primary : COLOR.textMuted));

    await addText(tab, label, FONT_SIZE.xs, isActive ? BRAND.text : COLOR.textMuted, isActive);

    /* 활성 탭 하단 인디케이터 점 */
    if (isActive) {
      const dot = figma.createEllipse();
      dot.resize(4, 4);
      setFill(dot, BRAND.primary);
      tab.appendChild(dot);
    }

    comp.appendChild(tab);
  }

  figma.currentPage.appendChild(comp);
  return comp;
}
