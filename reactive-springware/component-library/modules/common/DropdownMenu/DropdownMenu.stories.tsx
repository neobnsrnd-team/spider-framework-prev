/**
 * @file DropdownMenu.stories.tsx
 * @description DropdownMenu 스토리.
 */
import type { Meta, StoryObj } from '@storybook/react';
import React from 'react';
import { Settings, User, LogOut, MoreVertical } from 'lucide-react';

import { DropdownMenu } from './index';

const defaultItems = [
  { label: '내 정보 관리', icon: <User className="size-4" />, onClick: () => console.log('내 정보 관리') },
  { label: '로그아웃', icon: <LogOut className="size-4" />, onClick: () => console.log('로그아웃'), variant: 'danger' as const },
];

const meta = {
  title: 'Modules/Common/DropdownMenu',
  component: DropdownMenu,
  tags: ['autodocs'],
  parameters: { layout: 'centered' },
  args: {
    items: defaultItems,
    align: 'right',
  },
} satisfies Meta<typeof DropdownMenu>;

export default meta;
type Story = StoryObj<typeof meta>;

/** 기본 사각형 아이콘 버튼 트리거 */
export const Default: Story = {
  args: {
    triggerIcon: <MoreVertical className="size-5" />,
    triggerVariant: 'default',
    triggerAriaLabel: '더보기 메뉴',
  },
};

/** 원형 배경 버튼 트리거 — UserProfile 설정 버튼 형태 */
export const Rounded: Story = {
  args: {
    triggerIcon: <Settings className="size-4" />,
    triggerVariant: 'rounded',
    triggerAriaLabel: '설정 메뉴',
  },
};

/** 패널 왼쪽 정렬 */
export const AlignLeft: Story = {
  args: {
    triggerIcon: <MoreVertical className="size-5" />,
    triggerVariant: 'default',
    align: 'left',
  },
};

/** children으로 완전히 커스텀한 트리거 */
export const CustomTrigger: Story = {
  args: {
    children: (
      <button
        type="button"
        style={{
          padding: '6px 12px', borderRadius: 8,
          border: '1px solid #e2e8f0', background: '#f8fafc',
          fontSize: 13, cursor: 'pointer',
        }}
      >
        메뉴 열기 ▾
      </button>
    ),
  },
};
