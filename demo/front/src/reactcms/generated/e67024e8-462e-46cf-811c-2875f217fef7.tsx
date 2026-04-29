// 레이아웃: page

import { PageLayout, Stack, Typography } from "@cl";

export default function NewPage() {
  return (
    <PageLayout title="페이지 제목" showBack rightBtnType="close" bottomBtnCnt="0" bottomBtn1Label="확인" bottomBtn2Label="취소">
      <Stack gap="md">
        <div className="px-[16px]"><Typography variant="heading" weight="bold" color="primary" as="p" numeric={false}>테스트중입니다!!</Typography></div>
      </Stack>
    </PageLayout>
  );
}