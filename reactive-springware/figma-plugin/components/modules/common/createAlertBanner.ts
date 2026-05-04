/**
 * @file createAlertBanner.ts
 * @description Figma AlertBanner 컴포넌트 세트 생성.
 * intent(warning|danger|success|info)를 Figma variant로 매핑한다.
 * 컴포넌트 이름: "AlertBanner"
 */
import { COLOR, SPACING, RADIUS, FONT_SIZE } from '../../../tokens';
import { createComponent, combineVariants, setAutoLayout, setPadding, setFill, setStroke, addText } from '../../../helpers';
import { createIcon, type IconName } from '../../../icons';

type AlertIntent = 'Warning' | 'Danger' | 'Success' | 'Info';

const INTENT_ICON: Record<AlertIntent, IconName> = {
  Warning: 'AlertTriangle',
  Danger:  'AlertCircle',
  Success: 'CheckCircle2',
  Info:    'Info',
};

const INTENT_CONFIG: Record<AlertIntent, {
  bg: Parameters<typeof setFill>[1];
  border: Parameters<typeof setFill>[1];
  text: Parameters<typeof setFill>[1];
}> = {
  Warning: { bg: COLOR.warningSurface, border: COLOR.warningBorder, text: COLOR.warningText },
  Danger:  { bg: COLOR.dangerSurface,  border: COLOR.dangerBorder,  text: COLOR.dangerText  },
  Success: { bg: COLOR.successSurface, border: COLOR.successBorder, text: COLOR.successText },
  Info:    { bg: COLOR.primarySurface, border: COLOR.border,        text: COLOR.primaryText },
};

async function createAlertVariant(intent: AlertIntent): Promise<ComponentNode> {
  const { bg, border, text } = INTENT_CONFIG[intent];
  const comp = createComponent(`Intent=${intent}`);
  setAutoLayout(comp, 'HORIZONTAL', SPACING.sm);
  setPadding(comp, SPACING.standard, SPACING.standard);
  comp.resize(328, 52);
  comp.primaryAxisSizingMode = 'FIXED';
  comp.counterAxisSizingMode = 'AUTO';
  comp.counterAxisAlignItems = 'MIN';
  comp.cornerRadius = RADIUS.md;
  setFill(comp, bg);
  setStroke(comp, border);

  comp.appendChild(createIcon(INTENT_ICON[intent], 18, text));

  const msg = await addText(comp, `${intent} 알림 메시지입니다.`, FONT_SIZE.sm, text);
  msg.layoutGrow = 1;

  return comp;
}

export async function createAlertBanner(): Promise<ComponentSetNode> {
  const intents: AlertIntent[] = ['Warning', 'Danger', 'Success', 'Info'];
  return combineVariants(await Promise.all(intents.map(createAlertVariant)), 'AlertBanner', 1);
}
