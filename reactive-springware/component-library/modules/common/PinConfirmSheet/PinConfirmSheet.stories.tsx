/**
 * @file PinConfirmSheet.stories.tsx
 * @description PinConfirmSheet 컴포넌트 스토리.
 *
 * BottomSheet + PinDotIndicator + NumberKeypad 조합.
 * - pinLength 자리 입력 완료 시 onConfirm 자동 호출
 * - 재배열 버튼으로 키패드 순서 셔플 가능
 * - errorMessage 전달 시 도트 아래 에러 표시 + PIN 초기화
 */
import type { Meta, StoryObj } from '@storybook/react';
import React, { useState } from 'react';
import { PinConfirmSheet } from './index';
import { Button } from '../../../core/Button';

const meta = {
  title: 'Modules/Common/PinConfirmSheet',
  component: PinConfirmSheet,
  tags: ['autodocs'],
  parameters: { brand: 'hana', domain: 'banking', layout: 'centered' },
  argTypes: {
    open:         { control: 'boolean' },
    title:        { control: 'text' },
    pinLength:    { control: 'select', options: [4, 6] },
    errorMessage: { control: 'text' },
  },
  args: {
    open:      true,
    title:     '비밀번호 입력',
    pinLength: 4,
    onClose:   () => {},
    onConfirm: (pin: string) => { alert(`입력된 PIN: ${pin}`); },
  },
} satisfies Meta<typeof PinConfirmSheet>;

export default meta;
type Story = StoryObj<typeof meta>;

/** 기본 — 4자리 PIN */
export const Default: Story = {};

/** 6자리 PIN */
export const SixDigit: Story = {
  args: {
    title: '결제 비밀번호 입력',
    pinLength: 6,
  },
};

/** 에러 메시지 표시 — PIN 불일치 등 외부 오류 */
export const WithError: Story = {
  args: {
    errorMessage: '비밀번호가 일치하지 않습니다.',
  },
};

/** 버튼으로 제어 */
export const Controlled: Story = {
  render: () => {
    const [open, setOpen] = useState(false);
    const [error, setError] = useState<string | undefined>();

    const handleConfirm = (pin: string) => {
      // 1234가 아닌 경우 에러 처리 시뮬레이션
      if (pin !== '1234') {
        setError('비밀번호가 일치하지 않습니다. (정답: 1234)');
      } else {
        setError(undefined);
        setOpen(false);
        alert('PIN 확인 완료!');
      }
    };

    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16 }}>
        <Button variant="primary" onClick={() => { setError(undefined); setOpen(true); }}>
          결제 비밀번호 입력
        </Button>
        <PinConfirmSheet
          open={open}
          onClose={() => setOpen(false)}
          onConfirm={handleConfirm}
          title="결제 비밀번호"
          errorMessage={error}
        />
      </div>
    );
  },
};
