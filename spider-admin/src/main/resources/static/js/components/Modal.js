/**
 * Modal Component
 * JSON 설정 기반 모달 동적 생성기 — Bootstrap 5 Modal API 활용
 *
 * 지원 필드 타입: text, textarea, select, checkbox, number, hidden, readonly, table
 *
 * table 타입 — 동적 행 추가/삭제가 가능한 인라인 테이블 필드:
 *   { id: 'entries', type: 'table', label: '항목', addLabel: '행 추가',
 *     columns: [
 *       { id: 'key', label: 'Key', width: '220px', placeholder: 'key', className: 'entry-key' }
 *     ] }
 *   setData/getData 시 해당 필드는 배열([{key:'a'}, ...])로 처리된다.
 */
window.Modal = {
    instances: {},

    /**
     * 모달 초기화
     * @param {string} container - 컨테이너 선택자
     * @param {object} config - 모달 설정
     */
    init: function(container, config) {
        const $container = $(container);
        if (!$container.length) {
            console.error('Modal: Container not found:', container);
            return;
        }

        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const modalId = config.id || `${instanceId}_modal`;

        this.instances[instanceId] = {
            config,
            modalId,
            container,
            mode: 'create',
            data: null
        };

        // 모달 HTML 생성
        const html = this._generateModalHtml(config, modalId, instanceId);
        $container.html(html);

        // 이벤트 바인딩
        this._bindEvents(container);

        return this;
    },

    /**
     * 모달 HTML 생성 — Bootstrap 5 Modal 구조
     */
    _widthToSizeClass: function(width) {
        const px = parseInt(width);
        if (isNaN(px)) return '';
        if (px <= 560) return 'sp-modal-sm';
        if (px <= 720) return 'sp-modal-md';
        if (px <= 900) return 'sp-modal-lg';
        if (px <= 1200) return 'sp-modal-xl';
        return 'sp-modal-xxl';
    },

    _generateModalHtml: function(config, modalId, instanceId) {
        const width = config.width || '500px';
        const sizeClass = this._widthToSizeClass(width);

        let html = `
            <div class="modal fade" id="${modalId}" tabindex="-1" aria-hidden="true"
                 data-bs-backdrop="static" data-bs-keyboard="false">
                <div class="modal-dialog modal-dialog-centered ${sizeClass}">
                    <div class="modal-content">
                        <div class="modal-header py-2">
                            <h6 class="modal-title">${config.title || '상세'}</h6>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
        `;

        // 필드 렌더링
        if (config.fields) {
            html += this._renderFields(config.fields, instanceId);
        }

        html += `
                        </div>
                        <div class="modal-footer py-2">
        `;

        // 버튼 렌더링
        if (config.buttons !== false) {
            if (config.showDelete !== false) {
                html += `
                    <button type="button" class="btn btn-danger btn-sm me-auto" data-action="delete">
                        <i class="bi bi-trash"></i> 삭제
                    </button>
                `;
            }
            html += `
                <button type="button" class="btn btn-primary btn-sm" data-action="save">
                    <i class="bi bi-floppy"></i> 저장
                </button>
                <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">
                    <i class="bi bi-x-lg"></i> 닫기
                </button>
            `;
        }

        html += `
                        </div>
                    </div>
                </div>
            </div>
        `;

        return html;
    },

    /**
     * 필드 렌더링
     */
    _renderFields: function(fields, instanceId) {
        let html = '<div class="modal-form">';

        fields.forEach(field => {
            if (field.row) {
                // 행 그룹
                html += '<div class="row g-3">';
                field.row.forEach(f => {
                    html += this._renderField(f, instanceId);
                });
                html += '</div>';
            } else {
                // 단일 필드
                html += this._renderField(field, instanceId);
            }
        });

        html += '</div>';
        return html;
    },

    /**
     * 개별 필드 렌더링
     */
    _renderField: function(field, instanceId) {
        const fieldId = `${instanceId}_${field.id}`;
        const required = field.required ? '<span class="text-danger">*</span>' : '';
        const colClass = (field.fullWidth || field.type === 'table') ? 'col-12' : 'col-md-6';

        let html = `<div class="${colClass} mb-2">`;
        html += `<label for="${fieldId}" class="form-label small mb-1">${field.label || ''}${required}</label>`;

        switch (field.type) {
            case 'textarea':
                const rows = field.rows || 3;
                html += `<textarea id="${fieldId}" class="form-control form-control-sm" rows="${rows}"${field.maxLength ? ` maxlength="${field.maxLength}"` : ''}></textarea>`;
                break;

            case 'select':
                html += `<select id="${fieldId}" class="form-select form-select-sm">`;
                if (field.placeholder) {
                    html += `<option value="">${field.placeholder}</option>`;
                }
                (field.options || []).forEach(opt => {
                    if (typeof opt === 'string') {
                        html += `<option value="${opt}">${opt}</option>`;
                    } else {
                        html += `<option value="${opt.value}">${opt.label}</option>`;
                    }
                });
                html += '</select>';
                break;

            case 'checkbox':
                html += `<div class="form-check"><input type="checkbox" id="${fieldId}" class="form-check-input"></div>`;
                break;

            case 'number':
                html += `<input type="number" id="${fieldId}" class="form-control form-control-sm"${field.min !== undefined ? ` min="${field.min}"` : ''}${field.max !== undefined ? ` max="${field.max}"` : ''}>`;
                break;

            case 'hidden':
                html += `<input type="hidden" id="${fieldId}">`;
                break;

            case 'readonly':
                html += `<input type="text" id="${fieldId}" class="form-control form-control-sm" readonly>`;
                break;

            case 'table':
                html += this._renderTableField(field, fieldId);
                break;

            default: // text
                html += `<input type="text" id="${fieldId}" class="form-control form-control-sm"${field.maxLength ? ` maxlength="${field.maxLength}"` : ''}${field.placeholder ? ` placeholder="${field.placeholder}"` : ''}>`;
        }

        html += '</div>';
        return html;
    },

    // ─── table 필드 ──────────────────────────────────────────

    /**
     * table 필드 구조 렌더링 (toolbar + thead + 빈 tbody)
     */
    _renderTableField: function(field, tbodyId) {
        const addLabel = field.addLabel || '행 추가';
        let html = '';

        // 행 추가 버튼
        html += `<div class="d-flex justify-content-end mb-1" data-table-toolbar="${field.id}">`;
        html += `<button type="button" class="btn btn-xs btn-primary" data-table-add="${field.id}">`;
        html += `<i class="bi bi-plus-lg"></i> ${addLabel}</button></div>`;

        // 테이블
        html += '<table class="table table-sm sp-data-grid"><thead><tr>';
        (field.columns || []).forEach(col => {
            const style = col.width ? ` style="width:${col.width}"` : '';
            html += `<th${style}>${col.label}</th>`;
        });
        html += '<th class="col-w-60" data-table-delete-header>삭제</th>';
        html += `</tr></thead><tbody id="${tbodyId}"></tbody></table>`;

        return html;
    },

    /**
     * table 행 1개 렌더링
     * @param {object} field - table 필드 설정
     * @param {object} [rowData] - 행 데이터 (없으면 빈 행)
     * @returns {string} - <tr> HTML
     */
    _renderTableRow: function(field, rowData) {
        let html = '<tr>';
        (field.columns || []).forEach(col => {
            const cls = col.className ? ` ${col.className}` : '';
            const val = rowData ? HtmlUtils.escape(rowData[col.id] || '') : '';
            const ph = col.placeholder ? ` placeholder="${col.placeholder}"` : '';
            html += `<td><input type="text" class="form-control form-control-sm${cls}" data-col="${col.id}" value="${val}"${ph}></td>`;
        });
        html += '<td class="text-center" data-table-delete-cell>';
        html += '<button type="button" class="btn btn-xs btn-outline-danger" data-table-row-delete>×</button>';
        html += '</td></tr>';
        return html;
    },

    // ─── 이벤트 ──────────────────────────────────────────────

    /**
     * 이벤트 바인딩
     */
    _bindEvents: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        const $modal = $(`#${instance.modalId}`);

        // 저장 버튼
        $modal.find('[data-action="save"]').on('click', () => {
            if (instance.config.onSave) {
                const data = this.getData(container);
                instance.config.onSave(data, instance.mode);
            }
        });

        // 삭제 버튼
        $modal.find('[data-action="delete"]').on('click', () => {
            if (instance.config.onDelete) {
                const data = this.getData(container);
                instance.config.onDelete(data);
            }
        });

        // table: 행 추가 (이벤트 위임)
        $modal.on('click', '[data-table-add]', (e) => {
            const fieldId = $(e.currentTarget).data('table-add');
            const field = this._flattenFields(instance.config.fields).find(f => f.id === fieldId);
            if (!field) return;
            const tbodyId = `${instanceId}_${fieldId}`;
            const $tbody = $(`#${tbodyId}`);
            $tbody.append(this._renderTableRow(field));
            $tbody.find('tr:last input:first').focus();
        });

        // table: 행 삭제 (이벤트 위임)
        $modal.on('click', '[data-table-row-delete]', (e) => {
            $(e.currentTarget).closest('tr').remove();
        });

        // Bootstrap hidden.bs.modal 이벤트로 닫기 콜백
        $modal[0].addEventListener('hidden.bs.modal', () => {
            if (instance.config.onClose) {
                instance.config.onClose();
            }
        });

        // Bootstrap shown.bs.modal 이벤트로 포커스
        $modal[0].addEventListener('shown.bs.modal', () => {
            $modal.find('input:not([readonly]):not([type="hidden"]):first, select:first, textarea:first').focus();
            if (instance.config.onOpen) {
                instance.config.onOpen(instance.mode, instance.data);
            }
        });
    },

    /**
     * 모달 열기 (생성 모드)
     * @param {string} container - 컨테이너 선택자
     * @param {object} initialData - 초기 데이터 (선택)
     */
    openForCreate: function(container, initialData) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        instance.mode = 'create';
        instance.data = initialData || {};

        // 폼 초기화
        this.reset(container);

        // 초기 데이터 설정
        if (initialData) {
            this.setData(container, initialData);
        }

        // readonly 필드 처리
        this._updateReadonlyFields(container, 'create');

        // 삭제 버튼 숨김
        $(`#${instance.modalId} [data-action="delete"]`).hide();

        this._show(container);
    },

    /**
     * 모달 열기 (수정 모드)
     * @param {string} container - 컨테이너 선택자
     * @param {object} data - 수정할 데이터
     */
    openForEdit: function(container, data) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        instance.mode = 'edit';
        instance.data = data;

        // 데이터 설정
        this.setData(container, data);

        // readonly 필드 처리
        this._updateReadonlyFields(container, 'edit');

        // 삭제 버튼 표시
        $(`#${instance.modalId} [data-action="delete"]`).show();

        this._show(container);
    },

    /**
     * readonly 필드 업데이트
     */
    _updateReadonlyFields: function(container, mode) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        const fields = this._flattenFields(instance.config.fields);
        fields.forEach(field => {
            const isReadonly = field.readonly === mode || field.readonly === true;

            if (field.type === 'table') {
                const $modal = $(`#${instance.modalId}`);
                // toolbar (행 추가 버튼) — d-flex !important 때문에 toggle 대신 d-none 사용
                $modal.find(`[data-table-toolbar="${field.id}"]`).toggleClass('d-none', isReadonly);
                // 삭제 컬럼 헤더 + 셀
                $modal.find(`[data-table-delete-header]`).toggleClass('d-none', isReadonly);
                const tbodyId = `${instanceId}_${field.id}`;
                $(`#${tbodyId}`).find('[data-table-delete-cell]').toggleClass('d-none', isReadonly);
                // 입력 필드
                $(`#${tbodyId}`).find('input').prop('readonly', isReadonly);
                return;
            }

            const $field = $(`#${instanceId}_${field.id}`);
            if (isReadonly) {
                $field.prop('readonly', true).addClass('readonly');
            } else {
                $field.prop('readonly', false).removeClass('readonly');
            }
        });
    },

    /**
     * 필드 배열 평탄화
     */
    _flattenFields: function(fields) {
        const result = [];
        fields.forEach(field => {
            if (field.row) {
                result.push(...field.row);
            } else {
                result.push(field);
            }
        });
        return result;
    },

    /**
     * 모달 표시 — Bootstrap Modal API
     */
    _show: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        const modalEl = document.getElementById(instance.modalId);
        bootstrap.Modal.getOrCreateInstance(modalEl).show();
    },

    /**
     * 모달 닫기
     * @param {string} container - 컨테이너 선택자
     */
    close: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        const modalEl = document.getElementById(instance.modalId);
        const bsModal = bootstrap.Modal.getInstance(modalEl);
        if (bsModal) bsModal.hide();
    },

    /**
     * 모달 제목 변경
     * @param {string} container - 컨테이너 선택자
     * @param {string} title - 새 제목
     */
    setTitle: function(container, title) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        $(`#${instance.modalId} .modal-title`).text(title);
    },

    /**
     * 모달 데이터 설정
     * @param {string} container - 컨테이너 선택자
     * @param {object} data - 설정할 데이터
     */
    setData: function(container, data) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        const fields = this._flattenFields(instance.config.fields);
        fields.forEach(field => {
            const value = data[field.id];

            if (field.type === 'table') {
                // 배열 데이터 → 행 렌더링
                const rows = Array.isArray(value) ? value : [];
                const $tbody = $(`#${instanceId}_${field.id}`);
                $tbody.empty();
                rows.forEach(rowData => {
                    $tbody.append(this._renderTableRow(field, rowData));
                });
                return;
            }

            const $field = $(`#${instanceId}_${field.id}`);
            if (field.type === 'checkbox') {
                $field.prop('checked', value === true || value === 'Y' || value === 1);
            } else if (value !== undefined && value !== null) {
                $field.val(value);
            }
        });
    },

    /**
     * 모달 데이터 조회
     * @param {string} container - 컨테이너 선택자
     * @returns {object} - 폼 데이터
     */
    getData: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return {};

        const data = {};
        const fields = this._flattenFields(instance.config.fields);

        fields.forEach(field => {
            if (field.type === 'table') {
                // 행 데이터 수집
                const rows = [];
                $(`#${instanceId}_${field.id} tr`).each(function() {
                    const row = {};
                    (field.columns || []).forEach(col => {
                        row[col.id] = $(this).find(`[data-col="${col.id}"]`).val() || '';
                    });
                    rows.push(row);
                });
                data[field.id] = rows;
                return;
            }

            const $field = $(`#${instanceId}_${field.id}`);
            if (field.type === 'checkbox') {
                data[field.id] = $field.is(':checked') ? 'Y' : 'N';
            } else {
                data[field.id] = $field.val();
            }
        });

        return data;
    },

    /**
     * 모달 폼 초기화
     * @param {string} container - 컨테이너 선택자
     */
    reset: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        const fields = this._flattenFields(instance.config.fields);
        fields.forEach(field => {
            if (field.type === 'table') {
                $(`#${instanceId}_${field.id}`).empty();
                return;
            }

            const $field = $(`#${instanceId}_${field.id}`);

            if (field.type === 'checkbox') {
                $field.prop('checked', field.default || false);
            } else if (field.type === 'select') {
                $field.val(field.default || '');
            } else {
                $field.val(field.default || '');
            }
        });
    },

    /**
     * 유효성 검사
     * @param {string} container - 컨테이너 선택자
     * @returns {boolean} - 유효 여부
     */
    validate: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return false;

        let isValid = true;
        const fields = this._flattenFields(instance.config.fields);

        fields.forEach(field => {
            if (field.type === 'table') return; // table 필드는 validate 제외

            const $field = $(`#${instanceId}_${field.id}`);
            const value = $field.val();

            // 필수 필드 검사
            if (field.required && (!value || value.trim() === '')) {
                isValid = false;
                $field.addClass('is-invalid');
            } else {
                $field.removeClass('is-invalid');
            }
        });

        return isValid;
    },

    /**
     * 특정 필드 값 조회
     * @param {string} container - 컨테이너 선택자
     * @param {string} fieldId - 필드 ID
     * @returns {any} - 필드 값
     */
    getValue: function(container, fieldId) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        return $(`#${instanceId}_${fieldId}`).val();
    },

    /**
     * 특정 필드 값 설정
     * @param {string} container - 컨테이너 선택자
     * @param {string} fieldId - 필드 ID
     * @param {any} value - 설정할 값
     */
    setValue: function(container, fieldId, value) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        $(`#${instanceId}_${fieldId}`).val(value);
    },

    /**
     * 모달 모드 조회
     * @param {string} container - 컨테이너 선택자
     * @returns {string} - 'create' 또는 'edit'
     */
    getMode: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        return instance ? instance.mode : null;
    },

    /**
     * 모달 표시 여부
     * @param {string} container - 컨테이너 선택자
     * @returns {boolean} - 표시 여부
     */
    isVisible: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return false;
        const modalEl = document.getElementById(instance.modalId);
        return modalEl && modalEl.classList.contains('show');
    }
};
