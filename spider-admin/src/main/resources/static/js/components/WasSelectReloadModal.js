/**
 * WAS Reload Modal Component
 *
 * WAS 인스턴스를 선택하여 Reload를 실행하는 재사용 가능한 모달 컴포넌트.
 *
 * 사용법:
 *   // 1. HTML에 모달 fragment 포함
 *   <div th:replace="~{modals/was-reload-modal :: modal}"></div>
 *
 *   // 2. 초기화 (WAS 그룹/인스턴스 데이터 로드)
 *   WasSelectReloadModal.init();
 *
 *   // 3. 모달 열기
 *   WasSelectReloadModal.open({
 *       title: 'XML Property Reload',   // 선택: 모달 제목 커스터마이즈
 *       onExecute: function(instanceIds) {
 *           // Reload API 호출 후 결과 표시
 *           $.ajax({
 *               url: '/api/reload/execute',
 *               method: 'POST',
 *               contentType: 'application/json',
 *               data: JSON.stringify({ reloadType: 'xml_property', instanceIds }),
 *               success: (response) => {
 *                   if (response.success && response.data) {
 *                       WasSelectReloadModal.showResults(response.data.results);
 *                   } else {
 *                       WasSelectReloadModal.resetButton();
 *                       alert(response.message || 'Reload 실행에 실패했습니다.');
 *                   }
 *               },
 *               error: (xhr) => {
 *                   WasSelectReloadModal.resetButton();
 *                   alert(xhr.responseJSON?.message || 'Reload 실행 중 오류가 발생했습니다.');
 *               }
 *           });
 *       }
 *   });
 */
window.WasSelectReloadModal = {
    wasGroups: [],
    instances: [],
    _showingResults: false,
    _onExecute: null,

    // -----------------------------------------------------------------------
    // 초기화
    // -----------------------------------------------------------------------

    init() {
        this._loadWasGroups();
        this._bindEvents();
    },

    _bindEvents() {
        // 닫기 / 취소
        document.getElementById('btnCloseWasReload').addEventListener('click', () => this.close());
        document.getElementById('btnCancelWasReload').addEventListener('click', () => this.close());

        // 실행 / 결과 후 닫기
        document.getElementById('btnConfirmWasReload').addEventListener('click', () => this._onConfirm());

        // 그룹 필터 변경 → 백엔드에서 인스턴스 재조회
        document.getElementById('wasReloadGroupSelect').addEventListener('change', () => this._loadInstances());

        // 에러 상세 모달 닫기
        document.getElementById('btnCloseWasReloadError').addEventListener('click', () => this.closeError());
    },

    // -----------------------------------------------------------------------
    // 데이터 로드
    // -----------------------------------------------------------------------

    _loadWasGroups() {
        $.ajax({
            url: API_BASE_URL + '/reload/groups',
            method: 'GET',
            success: (response) => {
                if (response.success && response.data) {
                    this.wasGroups = response.data;
                }
                this._loadInstances();
            },
            error: () => {
                this._loadInstances();
            }
        });
    },

    _loadInstances() {
        const wasGroupId = document.getElementById('wasReloadGroupSelect').value;
        const url = wasGroupId
            ? API_BASE_URL + '/reload/instances?wasGroupId=' + encodeURIComponent(wasGroupId)
            : API_BASE_URL + '/reload/instances';

        $.ajax({
            url: url,
            method: 'GET',
            success: (response) => {
                if (response.success && response.data) {
                    this.instances = response.data;
                } else {
                    this.instances = [];
                }
                this._renderCards();
            },
            error: (xhr) => {
                console.error('WAS 인스턴스 조회 실패:', xhr);
                Toast.error('WAS 인스턴스 목록을 불러오는데 실패했습니다.');
                this.instances = [];
                this._renderCards();
            }
        });
    },

    // -----------------------------------------------------------------------
    // 모달 열기 / 닫기
    // -----------------------------------------------------------------------

    /**
     * 모달 열기
     * @param {object} options
     * @param {string}   [options.title]     - 모달 제목 (기본: 'Reload 대상 WAS 선택')
     * @param {function} options.onExecute   - function(instanceIds) : 선택된 인스턴스 ID 배열 전달
     */
    open(options) {
        this._onExecute = options.onExecute || null;
        this._showingResults = false;
        this._lastResults = [];

        // 모달 제목
        document.getElementById('wasReloadModalTitle').textContent =
            options.title || 'Reload 대상 WAS 선택';

        // 선택 단계로 초기화
        document.getElementById('wasReloadSelectPhase').classList.remove('d-hidden');
        document.getElementById('wasReloadResultPhase').classList.add('d-hidden');

        const cancelBtn = document.getElementById('btnCancelWasReload');
        const confirmBtn = document.getElementById('btnConfirmWasReload');
        cancelBtn.classList.remove('d-hidden');
        confirmBtn.disabled = false;
        confirmBtn.innerHTML = 'Reload 실행';

        // 그룹 셀렉트 렌더링
        const sel = document.getElementById('wasReloadGroupSelect');
        sel.innerHTML = '<option value="">전체</option>';
        this.wasGroups.forEach(g => {
            const opt = document.createElement('option');
            opt.value = g.wasGroupId;
            opt.textContent = g.wasGroupName || g.wasGroupId;
            sel.appendChild(opt);
        });

        this._renderCards();
        bootstrap.Modal.getOrCreateInstance(document.getElementById('wasReloadModal')).show();
    },

    close() {
        var instance = bootstrap.Modal.getInstance(document.getElementById('wasReloadModal'));
        if (instance) instance.hide();
    },

    // -----------------------------------------------------------------------
    // WAS 카드 렌더링
    // -----------------------------------------------------------------------

    _renderCards() {
        const grid = document.getElementById('wasReloadCardGrid');

        if (!this.instances.length) {
            grid.innerHTML = '<div class="text-body-secondary fs-sm p-2">WAS 인스턴스가 없습니다</div>';
            return;
        }

        grid.innerHTML = this.instances.map(inst => {
            const typeClass = this._typeClass(inst.instanceType);
            const typeLabel = this._typeLabel(inst.instanceType);
            const escapedId = HtmlUtils.escape(inst.instanceId);
            return `<div class="sp-select-card">
                <div class="sp-select-card-icon ${typeClass}">${typeLabel}</div>
                <div class="sp-select-card-checkbox">
                    <input type="checkbox" value="${escapedId}" class="was-reload-checkbox">
                </div>
                <div class="sp-select-card-name">${escapedId}</div>
            </div>`;
        }).join('');
    },

    // -----------------------------------------------------------------------
    // 실행 / 결과
    // -----------------------------------------------------------------------

    _onConfirm() {
        // 결과 단계에서 버튼 클릭 → 닫기
        if (this._showingResults) {
            this.close();
            return;
        }

        const boxes = document.querySelectorAll('#wasReloadCardGrid .was-reload-checkbox:checked');
        const instanceIds = Array.from(boxes).map(cb => cb.value);
        if (!instanceIds.length) {
            Toast.error('Reload할 WAS 인스턴스를 선택해주세요.');
            return;
        }

        const btn = document.getElementById('btnConfirmWasReload');
        btn.disabled = true;
        btn.innerHTML = '<i class="bi bi-arrow-repeat sp-spin"></i> Reload 중...';

        if (this._onExecute) {
            this._onExecute(instanceIds);
        }
    },

    /**
     * Reload 결과 표시 (호출측에서 API 응답 후 호출)
     * @param {Array} results - [{instanceId, success, errorMessage}, ...]
     */
    showResults(results) {
        this._showingResults = true;
        this._lastResults = results;

        const tbody = document.getElementById('wasReloadResultBody');
        tbody.innerHTML = results.map(r => {
            const errorLink = (!r.success && r.errorMessage)
                ? `<a href="javascript:void(0)" class="was-reload-error-link" data-instance-id="${HtmlUtils.escape(r.instanceId)}">상세</a>`
                : '';
            return `<tr>
                <td class="text-start">${HtmlUtils.escape(r.instanceId)}</td>
                <td class="text-center fw-semibold ${r.success ? 'text-primary' : 'text-danger'}">${r.success ? '성공' : '실패'}</td>
                <td class="fs-xs text-body-secondary">${errorLink}</td>
            </tr>`;
        }).join('');

        // 에러 상세 링크 이벤트 위임
        tbody.querySelectorAll('.was-reload-error-link').forEach(link => {
            link.addEventListener('click', (e) => {
                this.showErrorDetail(e.currentTarget.dataset.instanceId);
            });
        });

        // 결과 단계로 전환
        document.getElementById('wasReloadSelectPhase').classList.add('d-hidden');
        document.getElementById('wasReloadResultPhase').classList.remove('d-hidden');

        // 버튼: 취소 숨기고, 확인 → 닫기
        document.getElementById('btnCancelWasReload').classList.add('d-hidden');
        const btn = document.getElementById('btnConfirmWasReload');
        btn.disabled = false;
        btn.innerHTML = '닫기';
    },

    /**
     * 버튼 상태 복원 (API 에러 시 호출측에서 사용)
     */
    resetButton() {
        const btn = document.getElementById('btnConfirmWasReload');
        btn.disabled = false;
        btn.innerHTML = 'Reload 실행';
    },

    // -----------------------------------------------------------------------
    // 에러 상세 모달
    // -----------------------------------------------------------------------

    /**
     * 특정 WAS 인스턴스의 에러 상세 모달 표시
     * @param {string} instanceId - 에러 상세를 볼 WAS 인스턴스 ID
     */
    showErrorDetail(instanceId) {
        const errors = this._lastResults.filter(r => !r.success && r.instanceId === instanceId);
        const tbody = document.getElementById('wasReloadErrorBody');
        tbody.innerHTML = errors.map(r => `
            <tr>
                <td>${HtmlUtils.escape(r.instanceId)}</td>
                <td class="text-start">${HtmlUtils.escape(r.errorMessage || '')}</td>
            </tr>`
        ).join('') || '<tr><td colspan="2" class="text-center py-4 text-body-secondary">조회된 오류가 없습니다.</td></tr>';

        bootstrap.Modal.getOrCreateInstance(document.getElementById('wasReloadErrorModal')).show();
    },

    closeError() {
        var instance = bootstrap.Modal.getInstance(document.getElementById('wasReloadErrorModal'));
        if (instance) instance.hide();
    },

    // -----------------------------------------------------------------------
    // 유틸
    // -----------------------------------------------------------------------

    _typeClass(type) {
        if (type === '1') return 'type-web';
        if (type === '2') return 'type-ap';
        if (type === '3') return 'type-hub';
        return 'type-web';
    },

    _typeLabel(type) {
        if (type === '1') return 'Web';
        if (type === '2') return 'AP';
        if (type === '3') return '통합';
        return 'Web';
    },

};
