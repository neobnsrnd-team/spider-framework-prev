/**
 * @file EmptyState.stories.tsx
 * @description EmptyState 컴포넌트 스토리.
 */
import type { Meta, StoryObj } from '@storybook/react';
import { FileX, WalletCards } from 'lucide-react';
import React from 'react';
import { EmptyState } from './index';

const meta = {
  title: 'Modules/Common/EmptyState',
  component: EmptyState,
  tags: ['autodocs'],
  parameters: { brand: 'hana', domain: 'banking', layout: 'centered' },
  decorators: [(Story: React.ComponentType) => <div style={{ width: 340 }}><Story /></div>],
  argTypes: {
    title:       { control: 'text' },
    description: { control: 'text' },
  },
  args: { title: '내용이 없어요', description: '아직 데이터가 없습니다.' },
} satisfies Meta<typeof EmptyState>;

export default meta;
type Story = StoryObj<typeof meta>;

/** 텍스트만 표시 */
export const Default: Story = {};

/** 아이콘 + 액션 버튼 (actionLabel/onAction) */
export const WithAction: Story = {
  args: {
    illustration: <WalletCards size={56} style={{ color: 'var(--color-brand)' }} />,
    title: '등록된 계좌가 없어요',
    description: '계좌를 추가하면 잔액과 거래 내역을 확인할 수 있어요',
    actionLabel: '계좌 추가하기',
    onAction: () => console.log('계좌 추가하기'),
  },
};

/** 일러스트 아이콘만 표시 */
export const WithIllustration: Story = {
  args: {
    illustration: <FileX size={48} style={{ color: 'var(--color-text-muted)' }} />,
    title: '거래 내역이 없어요',
    description: '선택한 기간에 거래 내역이 없습니다.',
  },
};

/** 커스텀 action 슬롯 (기존 하위 호환) */
export const WithCustomAction: Story = {
  args: {
    illustration: <WalletCards size={56} style={{ color: 'var(--color-brand)' }} />,
    title: '등록된 계좌가 없어요',
    description: '계좌를 추가하면 잔액과 거래 내역을 확인할 수 있어요',
    action: (
      <button style={{ background: 'var(--color-brand)', color: '#fff', border: 'none', borderRadius: 12, padding: '10px 24px', fontWeight: 700, cursor: 'pointer' }}>
        계좌 추가하기
      </button>
    ),
  },
};