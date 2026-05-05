/**
 * Message Browse Modal Component
 *
 * 전문 조회/변경을 위한 재사용 가능한 모달 컴포넌트.
 * TrxDetailModal과 함께 사용하여 전문 매핑 변경 기능 제공.
 *
 * 사용법:
 *   // 1. HTML에 모달 fragment 포함
 *   <div th:replace="~{modals/message-browse-modal :: modal}"></div>
 *
 *   // 2. JS import
 *   <script th:src="@{/js/components/MessageBrowseModal.js}"></script>
 *
 *   // 3. TrxDetailModal에서 자동 호출됨
 *   MessageBrowseModal.open('message', trxId, orgId, ioType);
 *
 * 의존성: TrxDetailModal, HtmlUtils
 */
(function () {
    'use strict';

    if (window.MessageBrowseModal && typeof window.MessageBrowseModal.open === 'function') {
        return;
    }

    // ==================== Message Browse Modal ====================
    window.MessageBrowseModal = {
        mode: 'message',
        currentTrxId: null,
        currentOrgId: null,
        currentIoType: null,
        currentPage: 1,
        pageSize: 20,
        selectedMessageId: null,
        useOrgFilter: true,
        orgFilterFallbackTried: false,

        open(mode, trxId, orgId, ioType) {
            this.mode = mode;
            this.currentTrxId = trxId;
            this.currentOrgId = orgId;
            this.currentIoType = ioType;
            this.currentPage = 1;
            this.selectedMessageId = null;
            this.useOrgFilter = true;
            this.orgFilterFallbackTried = false;

            document.getElementById('messageBrowseMode').value = mode;
            document.getElementById('messageBrowseTrxId').value = trxId;
            document.getElementById('messageBrowseOrgId').value = orgId;
            document.getElementById('messageBrowseIoType').value = ioType;

            const title = mode === 'stdMessage' ? '기존전문 조회' : '전문 조회';
            document.getElementById('messageBrowseTitle').textContent = title;

            document.getElementById('messageBrowseSearchValue').value = '';
            this.show();
            this.load();
        },

        show() {
            bootstrap.Modal.getOrCreateInstance(document.getElementById('messageBrowseModal')).show();
        },

        close() {
            bootstrap.Modal.getInstance(document.getElementById('messageBrowseModal'))?.hide();
        },

        search() {
            this.currentPage = 1;
            this.useOrgFilter = true;
            this.orgFilterFallbackTried = false;
            this.load();
        },

        load(page) {
            if (page) this.currentPage = page;

            const searchField = document.getElementById('messageBrowseSearchField').value;
            const searchValue = document.getElementById('messageBrowseSearchValue').value.trim();

            const params = new URLSearchParams({
                page: this.currentPage,
                size: this.pageSize,
                searchField: searchField,
                searchValue: searchValue,
                ioTypeFilter: this.currentIoType
            });
            if (this.useOrgFilter && this.currentOrgId) {
                params.set('orgIdFilter', this.currentOrgId);
            }

            fetch(`/api/trx-messages/browse?${params}`, {
                credentials: 'same-origin'
            })
                .then(res => res.json())
                .then(res => {
                    if (!res.success) throw new Error(res.message || '조회 실패');
                    const content = res.data.content || [];
                    if (content.length === 0 && this.useOrgFilter && this.currentOrgId && !this.orgFilterFallbackTried) {
                        this.orgFilterFallbackTried = true;
                        this.useOrgFilter = false;
                        this.load(this.currentPage);
                        return;
                    }
                    this.renderTable(content);
                    this.renderPagination(res.data);
                })
                .catch(err => {
                    console.error('[MessageBrowseModal.load]', err);
                    this.renderTable([]);
                });
        },

        renderTable(messages) {
            const tbody = document.getElementById('messageBrowseTableBody');
            if (!tbody) return;

            if (messages.length === 0) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="4" class="text-center py-4 text-body-secondary">
                            조회된 전문이 없습니다.
                        </td>
                    </tr>
                `;
                return;
            }

            const rows = messages.map(msg => `
                <tr data-message-id="${HtmlUtils.escape(msg.messageId)}" onclick="MessageBrowseModal.selectMessage('${HtmlUtils.escape(msg.messageId)}')">
                    <td>${HtmlUtils.escape(msg.orgId)}</td>
                    <td>${HtmlUtils.escape(msg.messageId)}</td>
                    <td>${HtmlUtils.escape(msg.headerYn || '')}</td>
                    <td>${HtmlUtils.escape(msg.messageName || '')}</td>
                </tr>
            `).join('');

            tbody.innerHTML = rows;
        },

        selectMessage(messageId) {
            document.querySelectorAll('#messageBrowseTableBody tr').forEach(tr => {
                tr.classList.remove('selected');
            });

            const row = document.querySelector(`#messageBrowseTableBody tr[data-message-id="${messageId}"]`);
            if (row) {
                row.classList.add('selected');
                this.selectedMessageId = messageId;
                this.confirmChange();
            }
        },

        confirmChange() {
            if (!this.selectedMessageId) {
                alert('전문을 선택하세요.');
                return;
            }

            this.close();
            MessageConfirmModal.open(
                this.mode,
                this.currentTrxId,
                this.currentOrgId,
                this.currentIoType,
                this.selectedMessageId
            );
        },

        renderPagination(pageData) {
            const container = document.getElementById('messageBrowsePagination');
            if (!container) return;

            const totalPages = pageData.totalPages || 0;
            const currentPage = pageData.currentPage || 1;

            const maxButtons = 5;
            let startPage = Math.max(1, currentPage - Math.floor(maxButtons / 2));
            let endPage = Math.min(totalPages, startPage + maxButtons - 1);

            if (endPage - startPage + 1 < maxButtons) {
                startPage = Math.max(1, endPage - maxButtons + 1);
            }

            let html = `
                <ul class="pagination pagination-sm">
                    <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
                        <a class="page-link" href="#" onclick="MessageBrowseModal.load(1); return false;">
                            <i class="bi bi-chevron-double-left"></i>
                        </a>
                    </li>
                    <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
                        <a class="page-link" href="#" onclick="MessageBrowseModal.load(${currentPage - 1}); return false;">
                            <i class="bi bi-chevron-left"></i>
                        </a>
                    </li>
            `;

            for (let i = startPage; i <= endPage; i++) {
                html += `
                    <li class="page-item ${i === currentPage ? 'active' : ''}">
                        <a class="page-link" href="#" onclick="MessageBrowseModal.load(${i}); return false;">${i}</a>
                    </li>
                `;
            }

            html += `
                    <li class="page-item ${currentPage === totalPages || totalPages === 0 ? 'disabled' : ''}">
                        <a class="page-link" href="#" onclick="MessageBrowseModal.load(${currentPage + 1}); return false;">
                            <i class="bi bi-chevron-right"></i>
                        </a>
                    </li>
                    <li class="page-item ${currentPage === totalPages || totalPages === 0 ? 'disabled' : ''}">
                        <a class="page-link" href="#" onclick="MessageBrowseModal.load(${totalPages}); return false;">
                            <i class="bi bi-chevron-double-right"></i>
                        </a>
                    </li>
                </ul>
            `;

            container.innerHTML = html;
        }
    };

    // ==================== Message Confirm Modal ====================
    window.MessageConfirmModal = {
        mode: null,
        trxId: null,
        orgId: null,
        ioType: null,
        messageId: null,

        open(mode, trxId, orgId, ioType, messageId) {
            this.mode = mode;
            this.trxId = trxId;
            this.orgId = orgId;
            this.ioType = ioType;
            this.messageId = messageId;

            this.show();
        },

        show() {
            bootstrap.Modal.getOrCreateInstance(document.getElementById('messageConfirmModal')).show();
        },

        close() {
            bootstrap.Modal.getInstance(document.getElementById('messageConfirmModal'))?.hide();
        },

        confirm() {
            let fieldName;
            if (this.ioType === 'I') {
                fieldName = this.mode === 'stdMessage' ? 'stdResMessageId' : 'resMessageId';
            } else {
                fieldName = this.mode === 'stdMessage' ? 'stdMessageId' : 'messageId';
            }
            const payload = {
                trxId: this.trxId,
                orgId: this.orgId,
                ioType: this.ioType,
                [fieldName]: this.messageId
            };

            fetch(`/api/trx-messages/${encodeURIComponent(this.trxId)}/${encodeURIComponent(this.orgId)}/${encodeURIComponent(this.ioType)}/message`, {
                method: 'PUT',
                credentials: 'same-origin',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify(payload)
            })
                .then(res => res.json())
                .then(res => {
                    if (!res.success) throw new Error(res.message || '변경 실패');
                    alert('변경되었습니다.');
                    this.close();

                    if (window.TrxDetailModal && window.TrxDetailModal.currentTrxId) {
                        window.TrxDetailModal.loadMessages(window.TrxDetailModal.currentTrxId);
                    }
                })
                .catch(err => {
                    console.error('[MessageConfirmModal.confirm]', err);
                    alert('전문 변경 중 오류가 발생했습니다.');
                });
        }
    };
})();
