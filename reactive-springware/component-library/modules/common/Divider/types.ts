/**
 * @file types.ts
 * @description Divider 컴포넌트 Props 타입 정의.
 *   섹션 구분을 위한 단순 수평선. DividerWithLabel과 달리 텍스트 없이 선(border)만 렌더링한다.
 *
 * @example
 * <Stack gap="md">
 *   <InfoRow label="출금가능액" value="2,950,000원" />
 *   <Divider />
 *   <SectionHeader title="거래내역" />
 * </Stack>
 */

export interface DividerProps {
  /** 추가 Tailwind 클래스 (색상·두께 override 시) */
  className?: string;
}
