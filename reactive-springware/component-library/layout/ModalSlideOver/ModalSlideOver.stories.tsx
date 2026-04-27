/**
 * @file ModalSlideOver.stories.tsx
 * @description ModalSlideOver 컴포넌트 스토리.
 *
 * Route 기반 모달 패턴에서 사용한다.
 * - direction="right": 오른쪽에서 슬라이드 (기본값, 서브 페이지 진입)
 * - direction="bottom": 아래에서 슬라이드 (카드 메뉴 등 부분 오버레이)
 * - onClose 미전달 시 백드롭 클릭으로 닫기 비활성화
 */
import type { Meta, StoryObj } from '@storybook/react';
import React, { useState } from 'react';
import { ModalSlideOver } from './index';
import { Button } from '../../core/Button';

const meta = {
  title: 'Layout/ModalSlideOver',
  component: ModalSlideOver,
  tags: ['autodocs'],
  parameters: { brand: 'hana', domain: 'banking', layout: 'fullscreen' },
  argTypes: {
    direction: { control: 'select', options: ['right', 'bottom'] },
    zIndex:    { control: 'number' },
  },
  args: {
    direction: 'right',
    zIndex: 50,
    onClose: () => {},
    children: null as unknown as React.ReactNode,
  },
} satisfies Meta<typeof ModalSlideOver>;

export default meta;
type Story = StoryObj<typeof meta>;

/** 기본 — 오른쪽에서 슬라이드 (서브 페이지 진입 패턴) */
export const Default: Story = {
  render: (args) => (
    <div style={{ width: '100%', height: '100vh', background: '#f1f5f9', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <p style={{ fontSize: 14, color: '#94a3b8' }}>배경 페이지</p>
      <ModalSlideOver {...args}>
        <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
          <p style={{ fontWeight: 700, fontSize: 18 }}>슬라이드 오버 콘텐츠</p>
          <p style={{ fontSize: 14, color: '#64748b' }}>오른쪽에서 슬라이드되는 모달입니다.</p>
        </div>
      </ModalSlideOver>
    </div>
  ),
};

/** direction="bottom" — 아래에서 슬라이드 */
export const Bottom: Story = {
  args: { direction: 'bottom' },
  render: (args) => (
    <div style={{ width: '100%', height: '100vh', background: '#f1f5f9', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <p style={{ fontSize: 14, color: '#94a3b8' }}>배경 페이지</p>
      <ModalSlideOver {...args}>
        <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
          <p style={{ fontWeight: 700, fontSize: 18 }}>하단 슬라이드 오버</p>
          <p style={{ fontSize: 14, color: '#64748b' }}>아래에서 슬라이드되는 모달입니다.</p>
        </div>
      </ModalSlideOver>
    </div>
  ),
};

/** 버튼으로 제어 */
export const Controlled: Story = {
  render: () => {
    const [open, setOpen] = useState(false);
    return (
      <div style={{ width: '100%', height: '100vh', background: '#f1f5f9', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Button variant="primary" onClick={() => setOpen(true)}>모달 열기</Button>
        {open && (
          <ModalSlideOver direction="right" onClose={() => setOpen(false)}>
            <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
              <p style={{ fontWeight: 700, fontSize: 18 }}>슬라이드 오버</p>
              <p style={{ fontSize: 14, color: '#64748b' }}>백드롭 또는 아래 버튼으로 닫을 수 있습니다.</p>
              <Button variant="outline" fullWidth onClick={() => setOpen(false)}>닫기</Button>
            </div>
          </ModalSlideOver>
        )}
      </div>
    );
  },
};

/** onClose 미전달 — 백드롭 클릭으로 닫기 비활성화 */
export const NoBackdropClose: Story = {
  args: { onClose: undefined },
  render: (args) => (
    <div style={{ width: '100%', height: '100vh', background: '#f1f5f9', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <p style={{ fontSize: 14, color: '#94a3b8' }}>배경 페이지</p>
      <ModalSlideOver {...args}>
        <div style={{ padding: 24, display: 'flex', flexDirection: 'column', gap: 16 }}>
          <p style={{ fontWeight: 700, fontSize: 18 }}>닫기 불가 모달</p>
          <p style={{ fontSize: 14, color: '#64748b' }}>백드롭 클릭으로 닫을 수 없습니다.</p>
        </div>
      </ModalSlideOver>
    </div>
  ),
};
