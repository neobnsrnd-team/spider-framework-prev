/**
 * @file PageLayout.stories.tsx
 * @description PageLayout 컴포넌트 스토리.
 */
import type { Meta, StoryObj } from '@storybook/react';
import React from 'react';
import { PageLayout } from './index';

const meta = {
  title: 'Layout/PageLayout',
  component: PageLayout,
  tags: ['autodocs'],
  parameters: { brand: 'hana', domain: 'banking', layout: 'fullscreen' },
  argTypes: {
    title:    { control: 'text' },
    onBack:   { action: '뒤로가기 클릭' },
  },
  args: { title: '계좌 상세', onBack: () => {} },
} satisfies Meta<typeof PageLayout>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  render: (args) => (
    <PageLayout {...args}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div style={{ background: '#f1f5f9', borderRadius: 12, padding: 16, fontSize: 14, color: '#475569' }}>
          페이지 콘텐츠 영역
        </div>
        <div style={{ background: '#f1f5f9', borderRadius: 12, padding: 16, fontSize: 14, color: '#475569' }}>
          카드 또는 리스트 컴포넌트 위치
        </div>
      </div>
    </PageLayout>
  ),
};

export const WithRightAction: Story = {
  args: {
    title: '이체하기',
    rightAction: (
      <button style={{ fontSize: 13, color: 'var(--color-brand-text)', background: 'none', border: 'none', cursor: 'pointer' }}>
        취소
      </button>
    ),
  },
  render: (args) => (
    <PageLayout {...args}>
      <div style={{ fontSize: 14, color: '#475569' }}>폼 콘텐츠 영역</div>
    </PageLayout>
  ),
};

export const NoBack: Story = {
  args: { title: '알림', onBack: undefined },
  render: (args) => (
    <PageLayout {...args}>
      <div style={{ fontSize: 14, color: '#475569' }}>뒤로가기 버튼 없는 페이지</div>
    </PageLayout>
  ),
};