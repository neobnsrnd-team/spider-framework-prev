/**
 * Transaction Detail Modal Component
 *
 * 거래 상세 정보 + 전문흐름(Message Flow)을 표시하는 재사용 가능한 모달 컴포넌트.
 * app-mapping, trx 등 여러 페이지에서 공통 사용.
 *
 * 사용법:
 *   // 1. HTML에 모달 fragment 포함
 *   <div th:replace="~{modals/trx-detail-modal :: modal}"></div>
 *   <div th:replace="~{modals/message-browse-modal :: modal}"></div>
 *
 *   // 2. JS import
 *   <script th:src="@{/js/components/TrxDetailModal.js}"></script>
 *   <script th:src="@{/js/components/MessageBrowseModal.js}"></script>
 *
 *   // 3. 모달 열기
 *   TrxDetailModal.open(trxId, orgId, {
 *       onRefresh: () => MyList.load(MyList.currentPage)
 *   });
 *
 * 의존성: WasSelectReloadModal, MessageBrowseModal, HtmlUtils
 */
(function () {
    'use strict';

    if (window.TrxDetailModal && typeof window.TrxDetailModal.open === 'function') {
        return;
    }

    function formatOperMode(value) {
        if (value === null || value === undefined) return 'WAS';
        const raw = String(value).trim();
        if (!raw) return 'WAS';
        const upper = raw.toUpperCase();
        if (raw === '1') return '신규 운영';
        if (upper === 'D' || raw === '개발' || upper === 'DEV') return '개발';
        if (upper === 'R' || raw === '운영' || upper === 'PRD') return '운영';
        if (upper === 'T' || raw === '테스트' || upper === 'TEST') return '테스트';
        return raw;
    }

    window.TrxDetailModal = {
        currentTrxId: null,
        currentOrgId: null,
        currentMessages: [],
        pendingBizGroupId: null,
        _onRefresh: null,
        _mode: 'detail',

        open(trxId, orgId, options) {
            if (!trxId) {
                alert('거래 정보가 없습니다.');
                return;
            }

            this._mode = 'detail';
            this.currentTrxId = trxId;
            this.currentOrgId = orgId || null;
            this._onRefresh = (options && options.onRefresh) || null;
            this._applyDetailMode();
            this.show();
            this.loadBizGroupOptions();
            this.loadTrxInfo(trxId).then(() => this.loadMessages(trxId));
        },

        create(options) {
            this._mode = 'create';
            this.currentTrxId = null;
            this.currentOrgId = null;
            this.currentMessages = [];
            this._onRefresh = (options && options.onRefresh) || null;
            this._applyCreateMode();
            this.show();
            this.loadBizGroupOptions();
        },

        _applyCreateMode() {
            // 헤더 변경
            document.querySelector('#trxDetailModal .modal-header .modal-title').textContent = '거래 등록';

            // 폼 초기화 + 편집 가능
            var trxId = document.getElementById('trxDetailTrxId');
            trxId.value = '';
            trxId.readOnly = false;
            trxId.classList.add('editable');

            var trxName = document.getElementById('trxDetailTrxName');
            trxName.value = '';
            trxName.readOnly = false;
            trxName.classList.add('editable');

            var trxDesc = document.getElementById('trxDetailTrxDesc');
            trxDesc.value = '';
            trxDesc.readOnly = false;
            trxDesc.classList.remove('bg-readonly');
            trxDesc.classList.add('bg-surface');

            var trxType = document.getElementById('trxDetailTrxType');
            trxType.value = '1';
            trxType.disabled = false;

            document.getElementById('trxDetailBizGroupId').value = '';
            this.pendingBizGroupId = null;

            // 거래시간설정 초기화
            document.getElementById('trxDetailBizdayOnly').checked = true;
            document.getElementById('trxDetailBizdayStart').value = '';
            document.getElementById('trxDetailBizdayEnd').value = '';
            document.getElementById('trxDetailHolidayStart').value = '';
            document.getElementById('trxDetailHolidayEnd').value = '';

            // trx-message 전용 필드 숨기기
            document.querySelectorAll('.trx-msg-field').forEach(function (el) { el.classList.add('d-hidden'); });
            document.querySelectorAll('.trx-msg-row').forEach(function (el) { el.classList.add('d-hidden'); });

            // 거래설명 colspan 확장 (타임아웃 숨겨진 만큼)
            var descTd = document.getElementById('trxDetailTrxDesc').closest('td');
            if (descTd) descTd.colSpan = 5;

            // 업무분류 colspan 확장
            var bizTd = document.getElementById('trxDetailBizGroupId').closest('td');
            if (bizTd) bizTd.colSpan = 5;

            // 전문흐름 섹션 숨기기
            document.getElementById('trxDetailFlowSection').classList.add('d-hidden');

            // 버튼 전환: 등록 모드
            document.getElementById('btnTrxDetailDelete').classList.add('d-hidden');
            document.getElementById('btnTrxDetailSave').classList.add('d-hidden');
            document.getElementById('btnTrxDetailCreate').classList.remove('d-hidden');

            // hidden 필드 초기화
            this.clearMessageFields();
        },

        _applyDetailMode() {
            // 헤더 변경
            document.querySelector('#trxDetailModal .modal-header .modal-title').textContent = '거래수정';

            // 필드 읽기 전용
            var trxId = document.getElementById('trxDetailTrxId');
            trxId.readOnly = true;
            trxId.classList.remove('editable');

            var trxName = document.getElementById('trxDetailTrxName');
            trxName.readOnly = true;
            trxName.classList.remove('editable');

            var trxDesc = document.getElementById('trxDetailTrxDesc');
            trxDesc.readOnly = true;
            trxDesc.classList.remove('bg-surface');
            trxDesc.classList.add('bg-readonly');

            document.getElementById('trxDetailTrxType').disabled = true;

            // trx-message 전용 필드 표시
            document.querySelectorAll('.trx-msg-field').forEach(function (el) { el.classList.remove('d-hidden'); });
            document.querySelectorAll('.trx-msg-row').forEach(function (el) { el.classList.remove('d-hidden'); });

            // colspan 복원
            var descTd = document.getElementById('trxDetailTrxDesc').closest('td');
            if (descTd) descTd.colSpan = 3;

            var bizTd = document.getElementById('trxDetailBizGroupId').closest('td');
            if (bizTd) bizTd.colSpan = 1;

            // 전문흐름 섹션 표시
            document.getElementById('trxDetailFlowSection').classList.remove('d-hidden');

            // 버튼 전환: 상세 모드
            document.getElementById('btnTrxDetailDelete').classList.remove('d-hidden');
            document.getElementById('btnTrxDetailSave').classList.remove('d-hidden');
            document.getElementById('btnTrxDetailCreate').classList.add('d-hidden');
        },

        loadBizGroupOptions() {
            fetch('/api/trx/options/biz-groups', {credentials: 'same-origin'})
                .then(res => res.json())
                .then(res => {
                    if (!res.success) return;
                    const select = document.getElementById('trxDetailBizGroupId');
                    if (!select) return;
                    select.innerHTML = '<option value="">선택 안 함</option>';
                    (res.data || []).forEach(id => {
                        const opt = document.createElement('option');
                        opt.value = id;
                        opt.textContent = id;
                        select.appendChild(opt);
                    });
                    if (this.pendingBizGroupId !== null) {
                        select.value = this.pendingBizGroupId;
                    }
                })
                .catch(err => console.error('[TrxDetailModal.loadBizGroupOptions]', err));
        },

        getTrxTypeLabel(code) {
            const map = {
                '1': 'ONLINE',
                '2': 'BATCH',
                '3': 'ASYNC',
                '4': 'ASYNC 비온라인',
                '5': '전문변환',
                '6': 'POS',
                '7': 'FHM',
                '8': 'RETRY'
            };
            return map[code] || code || '';
        },

        show() {
            bootstrap.Modal.getOrCreateInstance(document.getElementById('trxDetailModal')).show();
        },

        close() {
            var instance = bootstrap.Modal.getInstance(document.getElementById('trxDetailModal'));
            if (instance) instance.hide();
            this.currentTrxId = null;
            this.currentOrgId = null;
            this.pendingBizGroupId = null;
        },

        loadTrxInfo(trxId) {
            const url = `/api/trx/${encodeURIComponent(trxId)}`;
            return fetch(url, {credentials: 'same-origin'})
                .then(res => {
                    if (!res.ok) {
                        return res.json().then(errData => {
                            throw new Error(errData.message || `HTTP ${res.status}`);
                        }).catch(() => {
                            throw new Error(`HTTP ${res.status}`);
                        });
                    }
                    return res.json();
                })
                .then(res => {
                    if (!res.success) throw new Error(res.message || '조회 실패');
                    this.renderTrxInfo(res.data);
                })
                .catch(err => {
                    console.error('[TrxDetailModal.loadTrxInfo]', err);
                    this.renderTrxInfo({
                        trxId: trxId,
                        trxName: '',
                        trxDesc: '',
                        trxType: '',
                        retryTrxYn: 'N',
                        bizGroupId: '',
                        bizdayTrxYn: 'N',
                        bizdayTrxStartTime: '',
                        bizdayTrxEndTime: '',
                        holidayTrxYn: 'N',
                        holidayTrxStartTime: '',
                        holidayTrxEndTime: ''
                    });
                });
        },

        renderTrxInfo(trx) {
            document.getElementById('trxDetailTrxId').value = trx.trxId || '';
            document.getElementById('trxDetailTrxName').value = trx.trxName || '';
            document.getElementById('trxDetailTrxDesc').value = trx.trxDesc || '';

            const trxTypeSelect = document.getElementById('trxDetailTrxType');
            trxTypeSelect.value = trx.trxType || '';

            document.getElementById('trxDetailAutoManual').value = trx.retryTrxYn || 'N';

            this.pendingBizGroupId = trx.bizGroupId || '';
            document.getElementById('trxDetailBizGroupId').value = trx.bizGroupId || '';

            const bizdayRadio = document.getElementById('trxDetailBizdayOnly');
            const holidayRadio = document.getElementById('trxDetailHolidayAlso');
            if (trx.holidayTrxYn === 'Y') {
                holidayRadio.checked = true;
            } else {
                bizdayRadio.checked = true;
            }
            document.getElementById('trxDetailBizdayStart').value = trx.bizdayTrxStartTime || '';
            document.getElementById('trxDetailBizdayEnd').value = trx.bizdayTrxEndTime || '';
            document.getElementById('trxDetailHolidayStart').value = trx.holidayTrxStartTime || '';
            document.getElementById('trxDetailHolidayEnd').value = trx.holidayTrxEndTime || '';

            this.clearMessageFields();
        },

        clearMessageFields() {
            document.getElementById('trxDetailTimeout').value = '210';
            document.getElementById('trxDetailHexLogYn').value = '';
            const orgSelect = document.getElementById('trxDetailOrgId');
            orgSelect.innerHTML = '<option value="">선택</option>';
            document.getElementById('trxDetailMultiResType').value = '';
            document.getElementById('trxDetailResTypeFieldId').value = '';
            document.getElementById('trxDetailCurrentIoType').value = '';
            document.getElementById('trxDetailCurrentOrgId').value = '';
        },

        loadMessages(trxId) {
            const urlI = `/api/trx-messages/by-trx/${encodeURIComponent(trxId)}/I`;
            const urlO = `/api/trx-messages/by-trx/${encodeURIComponent(trxId)}/O`;

            const promises = [
                fetch(urlI, {credentials: 'same-origin'})
                    .then(res => {
                        if (!res.ok) {
                            return res.json().then(() => []).catch(() => []);
                        }
                        return res.json();
                    })
                    .then(res => res.success ? res.data : [])
                    .catch(() => []),
                fetch(urlO, {credentials: 'same-origin'})
                    .then(res => {
                        if (!res.ok) {
                            return res.json().then(() => []).catch(() => []);
                        }
                        return res.json();
                    })
                    .then(res => res.success ? res.data : [])
                    .catch(() => [])
            ];

            Promise.all(promises)
                .then(([iMessages, oMessages]) => {
                    const messages = [];

                    if (Array.isArray(iMessages)) {
                        iMessages.forEach(msg => {
                            messages.push({...msg, ioType: 'I', ioTypeName: '응답'});
                        });
                    }

                    if (Array.isArray(oMessages)) {
                        oMessages.forEach(msg => {
                            messages.push({...msg, ioType: 'O', ioTypeName: '요청'});
                        });
                    }

                    this.currentMessages = messages;
                    this.fillMessageFields(messages);
                    this.renderMessages(messages);
                })
                .catch(err => {
                    console.error('[TrxDetailModal.loadMessages]', err);
                    this.currentMessages = [];
                    this.renderMessages([]);
                });
        },

        fillMessageFields(messages) {
            const orgId = this.currentOrgId;
            const msgO = messages.find(m => m.ioType === 'O' && m.orgId === orgId)
                || messages.find(m => m.ioType === 'O');

            if (msgO) {
                document.getElementById('trxDetailTimeout').value = msgO.timeoutSec == null ? '210' : String(msgO.timeoutSec);
                document.getElementById('trxDetailHexLogYn').value = msgO.hexLogYn || '';
                document.getElementById('trxDetailMultiResType').value = msgO.multiResType || '';
                document.getElementById('trxDetailResTypeFieldId').value = msgO.resTypeFieldId || '';

                const orgSelect = document.getElementById('trxDetailOrgId');
                orgSelect.innerHTML = '<option value="">선택</option>';
                if (msgO.orgId) {
                    const opt = document.createElement('option');
                    opt.value = msgO.orgId;
                    opt.textContent = msgO.orgId;
                    opt.selected = true;
                    orgSelect.appendChild(opt);
                }

                document.getElementById('trxDetailCurrentIoType').value = 'O';
                document.getElementById('trxDetailCurrentOrgId').value = msgO.orgId || '';
            }
        },

        renderMessages(messages) {
            const container = document.getElementById('trxDetailMessagesContainer');
            const emptyState = document.getElementById('trxDetailEmptyState');
            emptyState.classList.add('d-hidden');

            const orgId = this.currentOrgId;
            const msgO = messages
                ? (messages.find(m => m.ioType === 'O' && m.orgId === orgId) || messages.find(m => m.ioType === 'O'))
                : null;
            const msgI = messages
                ? (messages.find(m => m.ioType === 'I' && m.orgId === orgId) || messages.find(m => m.ioType === 'I'))
                : null;

            const msgOStd = messages
                ? messages.find(m => m.ioType === 'O' && m.orgId === 'STD')
                : null;
            const msgIStd = messages
                ? messages.find(m => m.ioType === 'I' && m.orgId === 'STD')
                : null;

            const trxLabel = document.getElementById('trxDetailTrxName').value || this.currentTrxId || '';

            const resolveMessageInfo = (msg, ioType, isStd) => {
                if (isStd) {
                    if (!msg || msg.orgId !== 'STD') return null;
                } else if (!msg) {
                    return null;
                }
                return ioType === 'O'
                    ? (msg.messageName || msg.messageId || null)
                    : (msg.resMessageName || msg.resMessageId || null);
            };

            const renderCard = (msg, ioTypeLabel, ioType, isStd) => {
                const info = resolveMessageInfo(msg, ioType, isStd);
                const title = info
                    ? HtmlUtils.escape(info)
                    : (isStd ? `[표준] ${trxLabel}${ioTypeLabel} (미등록)` : `${ioTypeLabel} (미등록)`);
                const headerClass = info ? '' : ' unregistered';
                const changeAction = isStd ? 'openStdMessageChange' : 'openMessageChange';
                const changeLabel = (!info && isStd) ? '기존전문등록' : '전문변경';
                const showReload = !!info;

                return `
                    <div class="trx-message-card">
                        <div class="trx-message-card-header${headerClass}">${title}</div>
                        <div class="trx-message-card-actions">
                            ${showReload ? `<button type="button" class="btn btn-primary" onclick="TrxDetailModal.openWasReload('${ioType}', ${isStd})">Reload</button>` : ''}
                            <button type="button" class="btn btn-secondary" onclick="TrxDetailModal.${changeAction}('${ioType}')">${changeLabel}</button>
                        </div>
                    </div>`;
            };

            const renderFlowRow = (msg, msgStd, ioTypeLabel, ioType, isRequest) => {
                const arrowDir = isRequest ? '&#x25B6;' : '&#x25C0;';
                const arrowDots = isRequest
                    ? `&#x2022; &#x2022; &#x2022; ${arrowDir}`
                    : `${arrowDir} &#x2022; &#x2022; &#x2022;`;
                const sysArrow = isRequest ? '&#x2022; &#x25B6;' : '&#x25C0; &#x2022;';

                return `
                    <div class="trx-message-row">
                        ${renderCard(msg, ioTypeLabel, ioType, false)}
                        <div class="trx-flow-arrow">
                            <button type="button" class="arrow-label" onclick="TrxDetailModal.openMessageChange('${ioType}')">${ioTypeLabel}매핑등록</button>
                            <span class="arrow-dots">${arrowDots}</span>
                        </div>
                        ${renderCard(msgStd, ioTypeLabel, ioType, true)}
                        <div class="trx-flow-sys-arrow"><span class="arrow-dots">${sysArrow}</span></div>
                    </div>`;
            };

            const orgLabel = HtmlUtils.escape(this.currentOrgId || '외부시스템');
            container.innerHTML = `
                <div class="trx-flow-system">
                    <div class="trx-flow-system-icon trx-flow-system-icon-primary"><i class="bi bi-hdd-rack"></i></div>
                    <div class="trx-flow-system-label">${orgLabel}</div>
                </div>
                <div class="trx-flow-cards">
                    ${renderFlowRow(msgO, msgOStd, '요청', 'O', true)}
                    ${renderFlowRow(msgI, msgIStd, '응답', 'I', false)}
                </div>
                <div class="trx-flow-system">
                    <div class="trx-flow-system-icon"><i class="bi bi-hdd-rack"></i></div>
                    <div class="trx-flow-system-label">Local<br>System</div>
                </div>
            `;
        },

        toggleInfoZoom() {
            const infoTable = document.getElementById('trxDetailInfoTable');
            const timeTable = document.getElementById('trxDetailTimeTable');
            const isHidden = infoTable && infoTable.classList.contains('d-hidden');
            if (infoTable) infoTable.classList.toggle('d-hidden', !isHidden);
            if (timeTable) timeTable.classList.toggle('d-hidden', !isHidden);
        },

        refreshMessages() {
            if (this.currentTrxId) {
                this.loadMessages(this.currentTrxId);
            }
        },

        openWasReload(ioType, useStdOrg) {
            if (!this.currentTrxId) {
                alert('거래 정보가 없습니다.');
                return;
            }
            let message;
            if (useStdOrg) {
                message = this.currentMessages.find(m => m.ioType === ioType && m.orgId === 'STD')
                    || this.currentMessages.find(m => m.ioType === ioType);
            } else {
                message = this.currentMessages.find(m => m.ioType === ioType && m.orgId === this.currentOrgId)
                    || this.currentMessages.find(m => m.ioType === ioType);
            }
            if (!message || !message.orgId) {
                alert('전문 정보가 없습니다.');
                return;
            }
            const trxId = this.currentTrxId;
            const orgId = message.orgId;
            WasSelectReloadModal.open({
                title: `${trxId} 전문 정보 Reload`,
                onExecute: function (instanceIds) {
                    $.ajax({
                        url: API_BASE_URL + '/reload/execute',
                        method: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify({
                            reloadType: 'trx',
                            instanceIds: instanceIds,
                            additionalParams: {trxId: trxId, orgId: orgId, ioType: ioType}
                        }),
                        success: function (response) {
                            if (response.success && response.data) {
                                WasSelectReloadModal.showResults(response.data.results);
                            } else {
                                WasSelectReloadModal.resetButton();
                                alert(response.message || 'Reload 실행에 실패했습니다.');
                            }
                        },
                        error: function (xhr) {
                            WasSelectReloadModal.resetButton();
                            alert(xhr.responseJSON?.message || 'Reload 실행 중 오류가 발생했습니다.');
                        }
                    });
                }
            });
        },

        openMessageChange(ioType) {
            if (!this.currentTrxId) {
                alert('거래 정보가 없습니다.');
                return;
            }
            const message = this.currentMessages.find(m => m.ioType === ioType && m.orgId === this.currentOrgId)
                || this.currentMessages.find(m => m.ioType === ioType);
            const orgId = message ? message.orgId : (this.currentOrgId || '');
            MessageBrowseModal.open('message', this.currentTrxId, orgId, ioType);
        },

        openStdMessageChange(ioType) {
            if (!this.currentTrxId) {
                alert('거래 정보가 없습니다.');
                return;
            }
            const message = this.currentMessages.find(m => m.ioType === ioType && m.orgId === 'STD');
            const orgId = message ? message.orgId : 'STD';
            MessageBrowseModal.open('stdMessage', this.currentTrxId, orgId, ioType);
        },

        reloadAllMessages() {
            if (!this.currentTrxId) {
                alert('거래 정보가 없습니다.');
                return;
            }
            const message = this.currentMessages.find(m => m.orgId === this.currentOrgId) || this.currentMessages[0];
            const orgId = message ? message.orgId : (this.currentOrgId || '');
            if (!orgId) {
                alert('전문 정보가 없습니다.');
                return;
            }
            const trxId = this.currentTrxId;
            WasSelectReloadModal.open({
                title: `${trxId} 전문 정보 Reload`,
                onExecute: function (instanceIds) {
                    $.ajax({
                        url: API_BASE_URL + '/reload/execute',
                        method: 'POST',
                        contentType: 'application/json',
                        data: JSON.stringify({
                            reloadType: 'trx',
                            instanceIds: instanceIds,
                            additionalParams: {trxId: trxId, orgId: orgId}
                        }),
                        success: function (response) {
                            if (response.success && response.data) {
                                WasSelectReloadModal.showResults(response.data.results);
                            } else {
                                WasSelectReloadModal.resetButton();
                                alert(response.message || 'Reload 실행에 실패했습니다.');
                            }
                        },
                        error: function (xhr) {
                            WasSelectReloadModal.resetButton();
                            alert(xhr.responseJSON?.message || 'Reload 실행 중 오류가 발생했습니다.');
                        }
                    });
                }
            });
        },

        saveMessageInfo() {
            const trxId = this.currentTrxId;
            const ioType = document.getElementById('trxDetailCurrentIoType').value;
            const orgId = document.getElementById('trxDetailCurrentOrgId').value;

            if (!trxId || !ioType || !orgId) {
                alert('저장할 전문 정보를 선택해주세요. (거래ID, IO타입, 기관ID가 필요합니다)');
                return;
            }

            const timeoutValue = document.getElementById('trxDetailTimeout').value.trim();
            const hexLogValue = document.getElementById('trxDetailHexLogYn').value.trim();
            const multiResTypeValue = document.getElementById('trxDetailMultiResType').value.trim();
            const resTypeFieldValue = document.getElementById('trxDetailResTypeFieldId').value.trim();

            const payload = {
                timeoutSec: timeoutValue ? parseInt(timeoutValue) : 210,
                hexLogYn: hexLogValue || null,
                multiResType: multiResTypeValue || null,
                resTypeFieldId: resTypeFieldValue || null
            };

            fetch(`/api/trx-messages/${encodeURIComponent(trxId)}/${encodeURIComponent(orgId)}/${encodeURIComponent(ioType)}/message`, {
                method: 'PUT',
                credentials: 'same-origin',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            })
                .then(res => res.json())
                .then(res => {
                    if (!res.success) throw new Error(res.message || '저장 실패');
                    alert('저장되었습니다.');
                    this.loadMessages(trxId);
                })
                .catch(err => {
                    console.error('[TrxDetailModal.saveMessageInfo]', err);
                    alert('저장 중 오류가 발생했습니다: ' + err.message);
                });
        },

        deleteTrx() {
            if (!this.currentTrxId) {
                alert('거래 정보가 없습니다.');
                return;
            }
            if (!confirm('거래를 삭제하시겠습니까?')) return;

            fetch(`/api/trx/${encodeURIComponent(this.currentTrxId)}`, {
                method: 'DELETE',
                credentials: 'same-origin'
            })
                .then(res => res.json())
                .then(res => {
                    if (!res.success) throw new Error(res.message || '삭제 실패');
                    alert('삭제되었습니다.');
                    this.close();
                    if (this._onRefresh) this._onRefresh();
                })
                .catch(err => {
                    console.error('[TrxDetailModal.deleteTrx]', err);
                    alert('삭제 중 오류가 발생했습니다: ' + err.message);
                });
        },

        toggleFlowZoom() {
            const modal = document.getElementById('trxDetailModal');
            if (modal) modal.classList.toggle('flow-zoom');
        },

        generateSource() {
            if (!this.currentTrxId) {
                alert('거래 정보가 없습니다.');
                return;
            }
            const orgId = this.currentOrgId || '';
            const url = API_BASE_URL + '/code-templates/generate?trxId='
                + encodeURIComponent(this.currentTrxId)
                + (orgId ? '&orgId=' + encodeURIComponent(orgId) : '');
            window.location.href = url;
        },

        createTrx() {
            const trxId = document.getElementById('trxDetailTrxId').value.trim();
            if (!trxId) {
                alert('거래코드를 입력하세요.');
                document.getElementById('trxDetailTrxId').focus();
                return;
            }
            if (!/^[a-zA-Z0-9_-]+$/.test(trxId)) {
                alert('거래코드는 영문, 숫자, 언더스코어(_), 하이픈(-)만 사용 가능합니다.');
                document.getElementById('trxDetailTrxId').focus();
                return;
            }

            const bizdayOnly = document.getElementById('trxDetailBizdayOnly').checked;
            const payload = {
                trxId: trxId,
                trxName: document.getElementById('trxDetailTrxName').value.trim(),
                trxDesc: document.getElementById('trxDetailTrxDesc').value.trim(),
                trxType: document.getElementById('trxDetailTrxType').value,
                bizGroupId: document.getElementById('trxDetailBizGroupId').value,
                retryTrxYn: 'N',
                maxRetryCount: 0,
                bizdayTrxYn: 'Y',
                bizdayTrxStartTime: document.getElementById('trxDetailBizdayStart').value.trim(),
                bizdayTrxEndTime: document.getElementById('trxDetailBizdayEnd').value.trim(),
                holidayTrxYn: bizdayOnly ? 'N' : 'Y',
                holidayTrxStartTime: document.getElementById('trxDetailHolidayStart').value.trim(),
                holidayTrxEndTime: document.getElementById('trxDetailHolidayEnd').value.trim()
            };

            fetch('/api/trx', {
                method: 'POST',
                credentials: 'same-origin',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            })
                .then(function (res) { return res.json(); })
                .then(function (res) {
                    if (!res.success) throw new Error(res.message || '등록 실패');
                    alert('거래가 등록되었습니다.');
                    TrxDetailModal.close();
                    if (TrxDetailModal._onRefresh) TrxDetailModal._onRefresh();
                })
                .catch(function (err) {
                    console.error('[TrxDetailModal.createTrx]', err);
                    alert('등록 중 오류가 발생했습니다: ' + err.message);
                });
        }
    };

    // formatOperMode is available as a module-private utility
    // Expose for potential external use
    window.TrxDetailModal._formatOperMode = formatOperMode;
})();
