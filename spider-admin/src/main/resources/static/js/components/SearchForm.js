/**
 * SearchForm Component
 * JSON 설정 기반 검색 폼 동적 생성기
 */
window.SearchForm = {
    instances: {},

    /**
     * 검색 폼 렌더링
     * @param {string} container - 컨테이너 선택자
     * @param {object} config - 검색 폼 설정
     * @param {function} onSearch - 검색 버튼 클릭 시 콜백
     */
    render: function(container, config, onSearch) {
        const $container = $(container);
        if (!$container.length) {
            console.error('SearchForm: Container not found:', container);
            return;
        }

        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        this.instances[instanceId] = { config, onSearch };

        // rows 설정이 있으면 다중 행, 없으면 fields로 단일 행
        const rows = config.rows || [config.fields];

        let html = '<div class="card border p-2 mb-3">';

        rows.forEach((rowFields, rowIndex) => {
            html += '<div class="d-flex flex-wrap align-items-center gap-2 mb-1">';

            rowFields.forEach((field) => {
                html += this._renderField(field, instanceId);
            });

            // 마지막 행에 spacer, limit, 조회 버튼 배치
            if (rowIndex === rows.length - 1) {
                // Spacer
                html += '<div class="flex-grow-1"></div>';

                // Limit rows (if configured)
                if (config.showLimit !== false) {
                    const limitConfig = config.limit || {};
                    html += `
                        <select id="${instanceId}_limitRows" class="form-select form-select-sm sp-limit-select">
                            <option value="10" ${limitConfig.default === 10 ? 'selected' : ''}>10</option>
                            <option value="20" ${limitConfig.default === 20 || !limitConfig.default ? 'selected' : ''}>20</option>
                            <option value="50">50</option>
                            <option value="100">100</option>
                            ${limitConfig.showLarge ? '<option value="500">500</option><option value="1000">1000</option>' : ''}
                        </select>
                        <label class="form-label mb-0 small fw-medium">건씩</label>
                    `;
                }

                // Search button
                html += `
                    <button class="btn btn-primary btn-sm" id="${instanceId}_searchBtn" data-testid="searchBtn">
                        <i class="bi bi-search"></i> 조회
                    </button>
                `;
            }

            html += '</div>';
        });

        html += '</div>';

        $container.html(html);

        // Bind search button event
        $(`#${instanceId}_searchBtn`).on('click', () => {
            if (onSearch) onSearch();
        });

        // Bind Enter key on text inputs
        $container.find('input[type="text"]').on('keypress', (e) => {
            if (e.key === 'Enter' && onSearch) onSearch();
        });

        // Initialize dateRange and cascading selects
        const allFields = config.rows ? config.rows.flat() : config.fields;
        this._initDateRanges(container, allFields);
        this._initCascadingSelects(container, allFields);

        return this;
    },

    /**
     * 개별 필드 렌더링
     */
    _renderField: function(field, instanceId) {
        const fieldId = `${instanceId}_${field.id}`;
        let labelHtml = '';
        let inputHtml = '';

        // Label (if exists)
        if (field.label) {
            const labelStyle = field.labelStyle ? ` style="${field.labelStyle}"` : '';
            labelHtml = `<label class="form-label mb-0 small fw-medium text-nowrap"${labelStyle}>${field.label}</label>`;
        }

        switch (field.type) {
            case 'select':
                inputHtml = this._renderSelect(field, fieldId);
                break;
            case 'text':
                inputHtml = this._renderText(field, fieldId);
                break;
            case 'number':
                inputHtml = this._renderNumber(field, fieldId);
                break;
            case 'date':
                inputHtml = this._renderDate(field, fieldId);
                break;
            case 'dateRange':
                inputHtml = this._renderDateRange(field, fieldId);
                break;
            case 'searchInput':
                inputHtml = this._renderSearchInput(field, fieldId);
                break;
            default:
                inputHtml = this._renderText(field, fieldId);
        }

        // 라벨+인풋을 하나의 그룹으로 감싸서 줄바꿈 시 분리 방지
        if (labelHtml) {
            return `<div class="d-flex align-items-center gap-1">${labelHtml}${inputHtml}</div>`;
        }
        return inputHtml;
    },

    _widthToClass: function(width) {
        const px = parseInt(width);
        if (isNaN(px)) return '';
        const supported = [70,80,100,120,140,150,180,200,220];
        const closest = supported.find(w => w >= px) || supported[supported.length - 1];
        return `w-${closest === 100 ? '100px' : closest}`;
    },

    _renderSelect: function(field, fieldId) {
        const width = field.width || '120px';
        const widthClass = field.className || this._widthToClass(width);
        let html = `<select id="${fieldId}" class="form-select form-select-sm ${widthClass}">`;

        if (field.placeholder || field.showAll) {
            html += `<option value="">${field.placeholder || '전체'}</option>`;
        }

        (field.options || []).forEach(opt => {
            if (typeof opt === 'string') {
                html += `<option value="${opt}">${opt}</option>`;
            } else {
                html += `<option value="${opt.value}">${opt.label}</option>`;
            }
        });

        html += '</select>';
        return html;
    },

    _renderText: function(field, fieldId) {
        const width = field.width || '180px';
        const widthClass = field.className || this._widthToClass(width);
        const placeholder = field.placeholder || '';
        return `<input type="text" id="${fieldId}" class="form-control form-control-sm ${widthClass}" placeholder="${placeholder}">`;
    },

    _renderNumber: function(field, fieldId) {
        const width = field.width || '80px';
        const widthClass = field.className || this._widthToClass(width);
        const value = field.value || '';
        return `<input type="number" id="${fieldId}" class="form-control form-control-sm ${widthClass} text-center" value="${value}">`;
    },

    _renderDate: function(field, fieldId) {
        const width = field.width || '140px';
        const widthClass = field.className || this._widthToClass(width);
        return `<input type="date" id="${fieldId}" class="form-control form-control-sm ${widthClass}">`;
    },

    _renderSearchInput: function(field, fieldId) {
        const width = field.width || '180px';
        const widthClass = field.className || this._widthToClass(width);
        const placeholder = field.placeholder || '';
        const onClickFn = field.onSearch || '';
        return `
            <div class="input-group input-group-sm ${widthClass}">
                <input type="text" id="${fieldId}" class="form-control form-control-sm" placeholder="${placeholder}">
                <button type="button" class="btn btn-outline-secondary btn-sm" onclick="${onClickFn}" title="${field.searchTitle || '찾기'}">
                    <i class="bi bi-search"></i>
                </button>
            </div>
        `;
    },

    _renderDateRange: function(field, fieldId) {
        const width = field.width || '140px';
        const widthClass = field.className || this._widthToClass(width);
        const fromId = `${fieldId}_from`;
        const toId = `${fieldId}_to`;
        return `
            <div class="input-group input-group-sm">
                <input type="text" id="${fromId}" class="form-control form-control-sm ${widthClass}"
                       placeholder="시작일" readonly>
                <span class="input-group-text"><i class="bi bi-calendar3"></i></span>
                <input type="text" id="${toId}" class="form-control form-control-sm ${widthClass}"
                       placeholder="종료일" readonly>
                <span class="input-group-text"><i class="bi bi-calendar3"></i></span>
            </div>
        `;
    },

    _initDateRanges: function(container, fields) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const dateRangeFields = fields.filter(f => f.type === 'dateRange');
        if (dateRangeFields.length === 0) return;

        const self = this;
        const initFn = () => {
            dateRangeFields.forEach(field => {
                const fieldId = `${instanceId}_${field.id}`;
                const fromId = `${fieldId}_from`;
                const toId = `${fieldId}_to`;
                const format = field.dateFormat || 'Y-m-d';

                const today = new Date();
                const rangeStart = new Date(today);
                rangeStart.setDate(rangeStart.getDate() - (field.defaultRange || 7));

                const fpFrom = flatpickr(`#${fromId}`, {
                    dateFormat: format,
                    defaultDate: rangeStart,
                    allowInput: false,
                    onChange: function(selectedDates) {
                        if (selectedDates[0]) fpTo.set('minDate', selectedDates[0]);
                    }
                });

                const fpTo = flatpickr(`#${toId}`, {
                    dateFormat: format,
                    defaultDate: today,
                    allowInput: false,
                    onChange: function(selectedDates) {
                        if (selectedDates[0]) fpFrom.set('maxDate', selectedDates[0]);
                    }
                });

                // Store instances for cleanup and reset
                if (!self.instances[instanceId]._flatpickrInstances) {
                    self.instances[instanceId]._flatpickrInstances = {};
                }
                self.instances[instanceId]._flatpickrInstances[field.id] = { from: fpFrom, to: fpTo };
            });
        };

        // Load flatpickr dynamically if not already loaded
        if (typeof flatpickr !== 'undefined') {
            initFn();
        } else {
            const scriptId = 'flatpickr-script';
            document.addEventListener('flatpickr-loaded', initFn, { once: true });

            if (!document.getElementById(scriptId)) {
                // Load CSS
                if (!document.querySelector('link[href*="flatpickr"]')) {
                    const link = document.createElement('link');
                    link.rel = 'stylesheet';
                    link.href = '/vendor/flatpickr/flatpickr.min.css';
                    document.head.appendChild(link);
                }
                // Load JS
                const script = document.createElement('script');
                script.id = scriptId;
                script.src = '/vendor/flatpickr/flatpickr.min.js';
                script.onload = () => {
                    document.dispatchEvent(new CustomEvent('flatpickr-loaded'));
                };
                document.head.appendChild(script);
            }
        }
    },

    _initCascadingSelects: function(container, fields) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const cascadingFields = fields.filter(f => f.dependsOn);
        if (cascadingFields.length === 0) return;

        cascadingFields.forEach(field => {
            const parentFieldId = `${instanceId}_${field.dependsOn}`;
            const childFieldId = `${instanceId}_${field.id}`;

            $(`#${parentFieldId}`).on('change', () => {
                const parentValue = $(`#${parentFieldId}`).val();
                const $child = $(`#${childFieldId}`);

                if (!parentValue) {
                    // Parent cleared — reset child to just first option
                    $child.find('option:not(:first)').remove();
                    $child.val('');
                    // Trigger change on child for chained cascading
                    $child.trigger('change');
                    return;
                }

                const url = field.fetchUrl.replace('{value}', encodeURIComponent(parentValue));
                fetch(url)
                    .then(res => res.json())
                    .then(data => {
                        const options = data.data || data;
                        // Keep first option (placeholder like "전체")
                        $child.find('option:not(:first)').remove();
                        (Array.isArray(options) ? options : []).forEach(opt => {
                            const option = document.createElement('option');
                            if (typeof opt === 'string') {
                                option.value = opt;
                                option.textContent = opt;
                            } else {
                                option.value = opt.value || opt.code || opt.id;
                                option.textContent = opt.label || opt.description || opt.name || option.value;
                            }
                            $child.append(option);
                        });
                        $child.val('');
                        $child.trigger('change');
                    })
                    .catch(err => console.error('SearchForm: cascading fetch error', err));
            });
        });
    },

    /**
     * 검색 파라미터 수집
     * @param {string} container - 컨테이너 선택자
     * @returns {object} - 수집된 파라미터
     */
    getParams: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return {};

        const params = {};
        const allFields = instance.config.rows
            ? instance.config.rows.flat()
            : instance.config.fields;

        allFields.forEach(field => {
            if (field.type === 'dateRange') {
                const fieldId = `${instanceId}_${field.id}`;
                const fromVal = $(`#${fieldId}_from`).val();
                const toVal = $(`#${fieldId}_to`).val();
                if (fromVal) params[field.fromId || `${field.id}From`] = fromVal;
                if (toVal) params[field.toId || `${field.id}To`] = toVal;
                return; // skip normal processing for this field
            }
            const fieldId = `${instanceId}_${field.id}`;
            const $field = $(`#${fieldId}`);
            if ($field.length) {
                const value = $field.val();
                if (value !== '' && value !== null && value !== undefined) {
                    params[field.id] = value;
                }
            }
        });

        // Limit rows
        const $limit = $(`#${instanceId}_limitRows`);
        if ($limit.length) {
            params.size = parseInt($limit.val()) || 20;
        }

        return params;
    },

    /**
     * 검색 필드 값 설정
     * @param {string} container - 컨테이너 선택자
     * @param {string} fieldId - 필드 ID
     * @param {any} value - 설정할 값
     */
    setValue: function(container, fieldId, value) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        $(`#${instanceId}_${fieldId}`).val(value);
    },

    /**
     * 검색 필드 값 조회
     * @param {string} container - 컨테이너 선택자
     * @param {string} fieldId - 필드 ID
     * @returns {any} - 필드 값
     */
    getValue: function(container, fieldId) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        return $(`#${instanceId}_${fieldId}`).val();
    },

    /**
     * Limit rows 값 조회
     * @param {string} container - 컨테이너 선택자
     * @returns {number} - limit rows 값
     */
    getLimit: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        return parseInt($(`#${instanceId}_limitRows`).val()) || 20;
    },

    /**
     * Select 옵션 동적 설정
     * @param {string} container - 컨테이너 선택자
     * @param {string} fieldId - 필드 ID
     * @param {array} options - 옵션 배열
     */
    setOptions: function(container, fieldId, options) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const $select = $(`#${instanceId}_${fieldId}`);
        if (!$select.length) return;

        // 첫 번째 옵션(전체) 유지
        const firstOption = $select.find('option:first').clone();
        $select.empty();
        if (firstOption.length) $select.append(firstOption);

        options.forEach(opt => {
            if (typeof opt === 'string') {
                $select.append(`<option value="${opt}">${opt}</option>`);
            } else {
                $select.append(`<option value="${opt.value}">${opt.label}</option>`);
            }
        });
    },

    /**
     * 검색 폼 초기화
     * @param {string} container - 컨테이너 선택자
     */
    reset: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        const allFields = instance.config.rows
            ? instance.config.rows.flat()
            : instance.config.fields;

        allFields.forEach(field => {
            if (field.type === 'dateRange') {
                const fpInstances = instance._flatpickrInstances;
                if (fpInstances && fpInstances[field.id]) {
                    const { from, to } = fpInstances[field.id];
                    from.clear();
                    to.clear();
                    from.set('maxDate', null);
                    to.set('minDate', null);
                } else {
                    const fieldId = `${instanceId}_${field.id}`;
                    $(`#${fieldId}_from`).val('');
                    $(`#${fieldId}_to`).val('');
                }
                return;
            }
            const fieldId = `${instanceId}_${field.id}`;
            const $field = $(`#${fieldId}`);
            if ($field.length) {
                if (field.type === 'select') {
                    $field.val('');
                } else {
                    $field.val(field.value || '');
                }
            }
        });

        // Reset limit
        $(`#${instanceId}_limitRows`).val(instance.config.limit?.default || 20);
    }
};
