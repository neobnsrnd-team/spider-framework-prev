// 레이아웃: page

import { PageLayout, Stack, BrandBanner } from "@cl";

export default function NewPage() {
  return (
    <PageLayout title="페이지 제목" showBack rightBtnType="close" bottomBtnCnt="0" bottomBtn1Label="확인" bottomBtn2Label="취소">
      <Stack gap="xs">
        <BrandBanner title="브랜드 타이틀" subtitle="서브 타이틀" />
        <BrandBanner title="브랜드 타이틀" subtitle="서브 타이틀" />
        <BrandBanner title="브랜드 타이틀" subtitle="서브 타이틀" />
        <BrandBanner title="브랜드 타이틀" subtitle="서브 타이틀" />
        <BrandBanner title="브랜드 타이틀" subtitle="서브 타이틀" />
        <BrandBanner title="브랜드 타이틀" subtitle="서브 타이틀" />
        <BrandBanner title="브랜드 타이틀" subtitle="서브 타이틀" />
        <BrandBanner title="브랜드 타이틀" subtitle="서브 타이틀" />
        <BrandBanner title="브랜드 타이틀" subtitle="서브 타이틀" />
        <BrandBanner title="브랜드 타이틀" subtitle="서브 타이틀" />
        <BrandBanner title="브랜드 타이틀" subtitle="서브 타이틀" />
        <BrandBanner title="브랜드 타이틀" subtitle="서브 타이틀" />
        <BrandBanner title="브랜드 타이틀" subtitle="서브 타이틀" />
      </Stack>
    </PageLayout>
  );
}