/**
 * @file index.tsx
 * @description 이체 입력 페이지 컴포넌트.
 *
 * Figma 원본:
 *   - 계좌번호 탭 기본 상태  (node-id: 1:1075) — 계좌번호 입력 + CMS 접힘
 *   - 연락처송금 탭 상태     (node-id: 1:997)  — 휴대폰번호·이름 입력 + 안내 배너
 *   - 통장표시내용/CMS 펼침 (node-id: 1:2349) — 계좌번호 탭 + CMS 폼 펼침
 *
 * 화면 구성:
 *   - 상단 헤더: 뒤로가기 + "이체" 타이틀 + 메뉴 아이콘
 *   - 출금 계좌 카드: 계좌명 + 계좌번호 + 출금가능금액 + 변경 버튼
 *   - 등록된 이체정보로 빠르게 이체하기 버튼
 *   - 금액 입력: AmountInput (이체 한도 안내 포함)
 *   - 수신 방식 탭: 계좌번호 / 연락처송금 (TabNav)
 *   - [계좌번호 탭] 필터 칩 + 금융권 선택 + 계좌번호 입력
 *   - [연락처 탭] 안내 배너 + 휴대폰번호 + 받는 분 이름 입력
 *   - 통장표시내용/CMS 섹션 (CollapsibleSection)
 *   - 예약이체 체크박스 (Checkbox)
 *   - 꼭 알아두세요 섹션 (CollapsibleSection)
 *   - 하단 고정 "다음" 버튼
 *
 * 실제 앱 구현 시 주의사항:
 *   - 모든 상태와 핸들러는 useTransferInput 훅에서 주입한다.
 *   - Page에서 직접 useState 사용 금지 (page-generation-rules.md 아키텍처 원칙).
 *   - 여기서는 Storybook 시각 확인 목적으로만 예외 적용한다.
 *
 * @param initialTab         - 초기 활성 탭 (기본: 'account')
 * @param cmsSectionExpanded - 통장표시내용/CMS 섹션 초기 펼침 여부 (기본: false)
 */
import React, { useState } from 'react';
import { Menu, WalletMinimal, Zap } from 'lucide-react';

/* ── Layout ──────────────────────────────────────────────────── */
import { PageLayout } from '../../../layout/PageLayout';
import { Stack }      from '../../../layout/Stack';
import { Inline }     from '../../../layout/Inline';

/* ── Core ────────────────────────────────────────────────────── */
import { Button } from '../../../core/Button';
import { Input }  from '../../../core/Input';
import { Select } from '../../../core/Select';
import { Typography }   from '../../../core/Typography';

/* ── Modules ─────────────────────────────────────────────────── */
import { AmountInput }        from '../../../modules/banking/AmountInput';
import { AlertBanner }        from '../../../modules/common/AlertBanner';
import { Checkbox }           from '../../../modules/common/Checkbox';
import { CollapsibleSection } from '../../../modules/common/CollapsibleSection';
import { TabNav }             from '../../../modules/common/TabNav';

/* ── Biz ─────────────────────────────────────────────────────── */
import { AccountSelectorCard } from '../../../biz/banking/AccountSelectorCard';

/* ── Pages ───────────────────────────────────────────────────── */
import { AccountSelectPage } from '../AccountSelectPage';
import type { AccountItem, AccountSelectTab } from '../AccountSelectPage';

import type { TransferInputPageProps, TransferInputTab, RecipientFilter } from './types';

// ── 상수 ──────────────────────────────────────────────────────

/**
 * Storybook 전용 mock 계좌 목록.
 * 실제 앱에서는 useTransferInput 훅을 통해 API에서 받아야 한다.
 */
const MOCK_ACCOUNTS: AccountItem[] = [
  { id: 'acc-1', accountName: '하나 주거래 통장',   accountNumber: '123-456-789012', balance: '3,000,000원' },
  { id: 'acc-2', accountName: '하나 저축 통장',     accountNumber: '123-456-789034', balance: '1,500,000원' },
  { id: 'acc-3', accountName: '하나 외화 통장',     accountNumber: '123-456-789056', balance: '$2,000.00'   },
];

/** 수신 방식 탭 목록 */
const TRANSFER_TABS = [
  { id: 'account', label: '계좌번호'   },
  { id: 'contact', label: '연락처송금' },
];

/** 금융권 선택 드롭다운 옵션 */
const BANK_OPTIONS = [
  { value: '',         label: '은행 선택'   },
  { value: 'hana',     label: '하나은행'      },
  { value: 'kb',       label: 'KB국민은행'    },
  { value: 'shinhan',  label: '신한은행'      },
  { value: 'woori',    label: '우리은행'      },
  { value: 'nh',       label: 'NH농협은행'    },
  { value: 'ibk',      label: 'IBK기업은행'   },
  { value: 'kakao',    label: '카카오뱅크'    },
  { value: 'toss',     label: '토스뱅크'      },
];

/**
 * 은행별 계좌번호 입력 포맷 패턴.
 * '#'은 숫자 한 자리, 그 외 문자('-')는 구분자로 자동 삽입된다.
 * 은행이 선택되지 않은 경우(빈 문자열)는 포맷 없이 자유 입력으로 처리한다.
 */
const BANK_ACCOUNT_FORMATS: Record<string, string> = {
  hana:    '###-######-#####',   // 하나은행    3-6-5  (14자리)
  kb:      '######-##-######',   // KB국민은행  6-2-6  (14자리)
  shinhan: '###-###-######',     // 신한은행    3-3-6  (12자리)
  woori:   '####-###-######',    // 우리은행    4-3-6  (13자리)
  nh:      '###-####-####-##',   // NH농협은행  3-4-4-2(13자리)
  ibk:     '###-######-##-###',  // IBK기업은행 3-6-2-3(14자리)
  kakao:   '####-##-#######',    // 카카오뱅크  4-2-7  (13자리)
  toss:    '####-####-####',     // 토스뱅크    4-4-4  (12자리)
};

/** 수신인 필터 칩 목록 */
const RECIPIENT_FILTERS: { id: RecipientFilter; label: string }[] = [
  { id: 'frequent',  label: '자주'   },
  { id: 'recent',    label: '최근'   },
  { id: 'myAccount', label: '내계좌' },
];

// ── 서브 컴포넌트 ──────────────────────────────────────────────

/**
 * 계좌번호 탭 콘텐츠.
 * 필터 칩 + 금융권 선택 드롭다운 + 계좌번호 입력 필드로 구성된다.
 */
function AccountTabContent({
  recipientFilter,
  onFilterChange,
  selectedBank,
  onBankChange,
  accountNumber,
  onAccountNumberChange,
}: {
  recipientFilter:      RecipientFilter;
  onFilterChange:       (filter: RecipientFilter) => void;
  selectedBank:         string;
  onBankChange:         (bank: string) => void;
  accountNumber:        string;
  onAccountNumberChange: (value: string) => void;
}) {
  return (
    <Stack gap="md">
      {/* 수신인 필터 칩: 자주 / 최근 / 내계좌 */}
      <Inline gap="xs">
        {RECIPIENT_FILTERS.map(({ id, label }) => (
          <Button
            key={id}
            variant={recipientFilter === id ? 'primary' : 'outline'}
            size="sm"
            onClick={() => onFilterChange(id)}
            className="rounded-full text-xs px-md"
          >
            {label}
          </Button>
        ))}
      </Inline>

      {/* 금융권 선택 드롭다운 */}
      <Select
        options={BANK_OPTIONS}
        value={selectedBank}
        onChange={onBankChange}
        aria-label="금융권 선택"
      />

      {/* 계좌번호 입력 — 선택된 은행의 포맷 패턴 적용 (미선택 시 자유 입력) */}
      <Input
        value={accountNumber}
        onChange={e => onAccountNumberChange(e.target.value)}
        placeholder={
          BANK_ACCOUNT_FORMATS[selectedBank]
            ? BANK_ACCOUNT_FORMATS[selectedBank].replace(/#/g, '0')
            : '계좌번호 입력'
        }
        formatPattern={BANK_ACCOUNT_FORMATS[selectedBank]}
        aria-label="계좌번호 입력"
      />
    </Stack>
  );
}

/**
 * 연락처송금 탭 콘텐츠.
 * 안내 배너 + 휴대폰번호 입력 + 받는 분 이름 입력으로 구성된다.
 */
function ContactTabContent({
  phoneNumber,
  onPhoneNumberChange,
  recipientName,
  onRecipientNameChange,
}: {
  phoneNumber:          string;
  onPhoneNumberChange:  (value: string) => void;
  recipientName:        string;
  onRecipientNameChange: (value: string) => void;
}) {
  return (
    <Stack gap="md">
      {/* 연락처송금 안내 배너 */}
      <AlertBanner intent="info">
        연락처송금은 상대방이 입금계좌번호를 입력할 때 출금이 이루어지며,
        받는 계좌가 타행인 경우 수수료가 발생할 수 있습니다.
      </AlertBanner>

      {/* 휴대폰번호 입력 — 자릿수에 따라 010-XXX-XXXX / 010-XXXX-XXXX 자동 포맷 */}
      <Input
        value={phoneNumber}
        onChange={e => onPhoneNumberChange(e.target.value)}
        placeholder="010-0000-0000"
        phoneFormat
        aria-label="휴대폰번호 입력"
      />

      {/* 받는 분 이름 입력 */}
      <Input
        value={recipientName}
        onChange={e => onRecipientNameChange(e.target.value)}
        placeholder="받는 분 이름 입력"
        aria-label="받는 분 이름 입력"
      />
    </Stack>
  );
}

/**
 * 통장표시내용/CMS 섹션 폼.
 * 내 통장 표시 / 받는 통장 표시 / CMS 코드 / 송금 메시지 입력 필드로 구성된다.
 */
function CmsFormContent({
  myMemo,
  recipientMemo,
  cmsCode,
  transferMessage,
  onMyMemoChange,
  onRecipientMemoChange,
  onCmsCodeChange,
  onTransferMessageChange,
}: {
  myMemo:                  string;
  recipientMemo:           string;
  cmsCode:                 string;
  transferMessage:         string;
  onMyMemoChange:          (v: string) => void;
  onRecipientMemoChange:   (v: string) => void;
  onCmsCodeChange:         (v: string) => void;
  onTransferMessageChange: (v: string) => void;
}) {
  return (
    <Stack gap="md" className="pt-xs">
      {/* label prop이 <label htmlFor>로 렌더링되므로 aria-label 중복 제거 */}
      <Input
        value={myMemo}
        onChange={e => onMyMemoChange(e.target.value)}
        label="내 통장 표시"
        placeholder="내 통장에 표시할 내용 입력 (최대 10자)"
        maxLength={10}
      />
      <Input
        value={recipientMemo}
        onChange={e => onRecipientMemoChange(e.target.value)}
        label="받는 통장 표시"
        placeholder="받는 분 통장에 표시할 내용 입력 (최대 10자)"
        maxLength={10}
      />
      <Input
        value={cmsCode}
        onChange={e => onCmsCodeChange(e.target.value)}
        label="CMS 코드"
        placeholder="CMS 코드 입력 (숫자)"
        inputMode="numeric"
      />
      <Input
        value={transferMessage}
        onChange={e => onTransferMessageChange(e.target.value)}
        label="송금 메시지"
        placeholder="받는 분께 보낼 메시지 입력 (선택)"
      />
    </Stack>
  );
}

// ── 메인 페이지 컴포넌트 ──────────────────────────────────────

export function TransferInputPage({
  initialTab         = 'account',
  cmsSectionExpanded = false,
}: TransferInputPageProps) {
  /* 스토리북 전용 — 실제 앱에서는 useTransferInput 훅에서 받아야 함 */

  /* 수신 방식 탭 */
  const [activeTab, setActiveTab] = useState<TransferInputTab>(initialTab);

  /* 출금 계좌 선택 BottomSheet */
  const [accountSelectOpen, setAccountSelectOpen]       = useState(false);
  const [accountSelectTab,  setAccountSelectTab]        = useState<AccountSelectTab>('mine');
  const [selectedAccountId, setSelectedAccountId]       = useState<string>(MOCK_ACCOUNTS[0].id);

  /* 선택된 계좌 정보 — id로부터 파생 */
  const selectedAccount = MOCK_ACCOUNTS.find(a => a.id === selectedAccountId) ?? MOCK_ACCOUNTS[0];

  /** 계좌 선택 완료 — 선택 후 BottomSheet 닫기 */
  const handleAccountSelect = (id: string) => {
    setSelectedAccountId(id);
    setAccountSelectOpen(false);
  };

  /* 금액 */
  const [amount, setAmount] = useState<number | null>(100000);

  /* 계좌번호 탭 폼 */
  const [recipientFilter,  setRecipientFilter]  = useState<RecipientFilter>('frequent');
  const [selectedBank,     setSelectedBank]     = useState('');
  const [accountNumber,    setAccountNumber]    = useState('');

  /* 연락처송금 탭 폼 */
  const [phoneNumber,    setPhoneNumber]    = useState('');
  const [recipientName,  setRecipientName]  = useState('');

  /* 통장표시내용/CMS 폼 */
  const [myMemo,          setMyMemo]          = useState('');
  const [recipientMemo,   setRecipientMemo]   = useState('');
  const [cmsCode,         setCmsCode]         = useState('');
  const [transferMessage, setTransferMessage] = useState('');

  /* 예약이체 체크박스 */
  const [scheduledTransfer, setScheduledTransfer] = useState(false);

  return (
    <div data-brand="hana" data-domain="banking">
      <PageLayout
        title="이체"
        onBack={() => console.log('뒤로가기')}
        rightAction={
          <button
            type="button"
            aria-label="메뉴 열기"
            className="flex items-center justify-center size-9 rounded-lg text-text-muted hover:bg-surface-raised transition-colors duration-150"
          >
            <Menu className="size-5" aria-hidden="true" />
          </button>
        }
        bottomBar={
          <Button
            variant="primary"
            size="lg"
            fullWidth
            onClick={() => console.log('다음')}
          >
            다음
          </Button>
        }
      >
        <Stack>
          {/* ── 출금 계좌 카드 ── */}
          <section className="px-standard pt-standard pb-xs">
            <AccountSelectorCard
              accountName={selectedAccount.accountName}
              accountNumber={selectedAccount.accountNumber}
              availableBalance={`출금가능금액: ${selectedAccount.balance}`}
              icon={<WalletMinimal size={18} aria-hidden="true" />}
              onAccountChange={() => setAccountSelectOpen(true)}
              onIconClick={() => setAccountSelectOpen(true)}
              iconAriaLabel="출금 계좌 변경"
            />
          </section>

          {/* ── 등록된 이체정보 빠른 이체 버튼 ── */}
          <section className="px-standard pb-xs">
            <Button
              variant="outline"
              size="sm"
              fullWidth
              leftIcon={<Zap className="size-4 text-brand-text" aria-hidden="true" />}
              onClick={() => console.log('빠른 이체')}
              className="rounded-3xl bg-surface-raised border-none text-text-secondary"
            >
              등록된 이체정보로 빠르게 이체하기
            </Button>
          </section>

          {/* ── 금액 입력 ── */}
          <section className="bg-surface px-standard py-lg">
            <AmountInput
              value={amount}
              onChange={setAmount}
              label=""
              placeholder="0"
              transferLimitText="1회 5,000,000원 / 1일 10,000,000원"
              maxAmount={3000000}
            />
          </section>

          {/* ── 수신 방식 탭 + 입력 영역 ── */}
          <section className="bg-surface-raised">
            {/* 계좌번호 / 연락처송금 탭 */}
            <div className="px-standard pt-md">
              <TabNav
                items={TRANSFER_TABS}
                activeId={activeTab}
                onTabChange={(id) => setActiveTab(id as TransferInputTab)}
                variant="pill"
                fullWidth
              />
            </div>

            <div className="px-standard py-md">
              {activeTab === 'account' ? (
                <AccountTabContent
                  recipientFilter={recipientFilter}
                  onFilterChange={setRecipientFilter}
                  selectedBank={selectedBank}
                  onBankChange={setSelectedBank}
                  accountNumber={accountNumber}
                  onAccountNumberChange={setAccountNumber}
                />
              ) : (
                <ContactTabContent
                  phoneNumber={phoneNumber}
                  onPhoneNumberChange={setPhoneNumber}
                  recipientName={recipientName}
                  onRecipientNameChange={setRecipientName}
                />
              )}
            </div>
          </section>

          {/* ── 추가 옵션 섹션 ── */}
          <section className="bg-surface px-standard py-md">
            <Stack gap="md">
              {/* 통장표시내용/CMS — 접힘/펼침 */}
              <CollapsibleSection
                defaultExpanded={cmsSectionExpanded}
                headerAlign="left"
                header={
                  <Typography variant="body-sm" color="secondary">
                    통장표시내용/CMS
                  </Typography>
                }
                className="p-0 bg-transparent"
              >
                <CmsFormContent
                  myMemo={myMemo}
                  recipientMemo={recipientMemo}
                  cmsCode={cmsCode}
                  transferMessage={transferMessage}
                  onMyMemoChange={setMyMemo}
                  onRecipientMemoChange={setRecipientMemo}
                  onCmsCodeChange={setCmsCode}
                  onTransferMessageChange={setTransferMessage}
                />
              </CollapsibleSection>

              {/* 예약이체 체크박스 */}
              <Checkbox
                id="scheduled-transfer"
                checked={scheduledTransfer}
                onChange={setScheduledTransfer}
                label="예약이체"
              />

              {/* 꼭 알아두세요! — 기본 접힘 */}
              <CollapsibleSection
                defaultExpanded={false}
                headerAlign="left"
                header={
                  <Typography variant="body-sm" color="muted">
                    꼭 알아두세요!
                  </Typography>
                }
                className="p-0 bg-transparent border-t border-border-subtle pt-md"
              >
                <Typography variant="caption" color="muted" className="leading-relaxed">
                  이체 실행 전 받는 계좌번호와 금액을 반드시 확인하세요.
                  착오송금 발생 시 반환 절차에 시간이 소요될 수 있습니다.
                </Typography>
              </CollapsibleSection>
            </Stack>
          </section>
        </Stack>
      </PageLayout>

      {/* ── 출금 계좌 선택 BottomSheet ── */}
      <AccountSelectPage
        open={accountSelectOpen}
        onClose={() => setAccountSelectOpen(false)}
        activeTab={accountSelectTab}
        onTabChange={setAccountSelectTab}
        accounts={MOCK_ACCOUNTS}
        selectedAccountId={selectedAccountId}
        onAccountSelect={handleAccountSelect}
        otherAccounts={[]}
        onOtherAccountSelect={handleAccountSelect}
        onConnectOtherAccount={() => console.log('다른 금융 계좌 연결')}
      />
    </div>
  );
}
