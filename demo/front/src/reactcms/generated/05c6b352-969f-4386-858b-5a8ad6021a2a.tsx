// 레이아웃: blank

import { BlankLayout, Stack, CardPaymentSummary, CardPerformanceBar } from "@cl";

export default function NewPage() {
  return (
    <BlankLayout>
      <Stack gap="lg">
        <CardPaymentSummary dateFull="2026년 4월" dateYM="2026.04" dateMD="04.15" totalAmount={350000} revolving={0} cardLoan={0} cashAdvance={0} />
        <CardPerformanceBar cardName="하나카드" currentAmount={350000} targetAmount={500000} benefitDescription="캐시백 1% 달성까지" />
      </Stack>
    </BlankLayout>
  );
}