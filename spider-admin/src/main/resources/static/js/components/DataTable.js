/**
 * DataTable Component
 * JSON 설정 기반 테이블 동적 생성기
 */
window.DataTable = {
    instances: {},

    /**
     * 테이블 렌더링
     * @param {string} container - 컨테이너 선택자
     * @param {object} config - 테이블 설정
     * @param {function} onRowClick - 행 클릭 시 콜백
     */
    render: function(container, config, onRowClick) {
        const $container = $(container);
        if (!$container.length) {
            console.error('DataTable: Container not found:', container);
            return;
        }

        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        this.instances[instanceId] = {
            config,
            onRowClick,
            data: [],
            sortBy: null,
            sortDirection: null
        };

        const tableId = config.id || `${instanceId}_table`;

        let html = `
            <div class="table-responsive">
                <table class="table table-sm table-hover sp-data-grid" id="${tableId}">
                    <colgroup>
        `;

        config.columns.forEach(col => {
            if (col.width && col.width.includes('%')) {
                html += `<col style="width:${col.width}">`;
            } else {
                const colClass = col.width ? this._widthToColClass(col.width) : '';
                html += `<col${colClass ? ` class="${colClass}"` : ''}>`;
            }
        });

        html += '</colgroup><thead class="table-light"><tr>';

        config.columns.forEach(col => {
            const sortClick = col.sortable ? `onclick="DataTable.toggleSort('${container}', '${col.key}')"` : '';

            if (col.sortable) {
                html += `
                    <th data-sort="${col.key}" ${sortClick}>
                        ${col.label}
                    </th>
                `;
            } else {
                html += `<th>${col.label}</th>`;
            }
        });

        html += `
                    </tr></thead>
                    <tbody id="${tableId}Body">
                        <tr>
                            <td colspan="${config.columns.length}" class="text-center text-body-secondary py-4">
                                ${config.emptyMessage || '조회 버튼을 클릭하여 데이터를 불러오세요.'}
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        `;

        $container.html(html);

        // Sort icons are handled by CSS ::after on th[data-sort]

        return this;
    },

    /**
     * 정렬 토글
     * @param {string} container - 컨테이너 선택자
     * @param {string} column - 정렬 컬럼
     */
    toggleSort: function(container, column) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        // 같은 칼럼 클릭: null -> ASC -> DESC -> null
        if (instance.sortBy === column) {
            if (instance.sortDirection === 'ASC') {
                instance.sortDirection = 'DESC';
            } else if (instance.sortDirection === 'DESC') {
                instance.sortBy = null;
                instance.sortDirection = null;
            } else {
                instance.sortDirection = 'ASC';
            }
        } else {
            // 다른 칼럼 클릭: ASC로 시작
            instance.sortBy = column;
            instance.sortDirection = 'ASC';
        }

        this._updateSortIcons(container);

        // 외부에서 정렬 이벤트 처리
        if (instance.config.onSort) {
            instance.config.onSort(instance.sortBy, instance.sortDirection);
        }
    },

    /**
     * 정렬 아이콘 업데이트
     */
    _updateSortIcons: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        const tableId = instance.config.id || `${instanceId}_table`;
        // 모든 정렬 클래스 초기화
        $(`#${tableId} thead th[data-sort]`).removeClass('sort-asc sort-desc');
        // 현재 정렬 칼럼에 클래스 적용
        if (instance.sortBy && instance.sortDirection) {
            const cls = instance.sortDirection === 'ASC' ? 'sort-asc' : 'sort-desc';
            $(`#${tableId} thead th[data-sort="${instance.sortBy}"]`).addClass(cls);
        }
    },

    /**
     * 테이블 데이터 설정
     * @param {string} container - 컨테이너 선택자
     * @param {array} data - 테이블 데이터
     */
    setData: function(container, data) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) {
            console.error('DataTable: Instance not found:', container);
            return;
        }

        instance.data = data || [];
        this._renderRows(container);
    },

    /**
     * 행 렌더링
     */
    _renderRows: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        const config = instance.config;
        const tableId = config.id || `${instanceId}_table`;
        const $tbody = $(`#${tableId}Body`);
        $tbody.empty();

        if (!instance.data || instance.data.length === 0) {
            $tbody.append(`
                <tr>
                    <td colspan="${config.columns.length}" class="text-center text-body-secondary py-4">
                        ${config.noDataMessage || '조회된 데이터가 없습니다.'}
                    </td>
                </tr>
            `);
            return;
        }

        instance.data.forEach((row, index) => {
            const rowClass = instance.onRowClick ? ' class="cursor-pointer"' : '';
            let rowHtml = `<tr data-index="${index}"${rowClass}>`;

            config.columns.forEach(col => {
                let value = this._getNestedValue(row, col.key);
                const alignClass = col.align ? (col.align === 'center' ? 'text-center' : col.align === 'right' || col.align === 'end' ? 'text-end' : 'text-start') : '';
                const cellClass = [alignClass, col.cellClass || ''].filter(Boolean).join(' ');

                // Custom renderer
                if (col.render) {
                    value = col.render(value, row, index);
                } else if (value === null || value === undefined) {
                    value = '';
                }

                rowHtml += `<td${cellClass ? ` class="${cellClass}"` : ''}>${value}</td>`;
            });

            rowHtml += '</tr>';
            $tbody.append(rowHtml);
        });

        // Bind row click event
        if (instance.onRowClick) {
            $tbody.find('tr[data-index]').on('click', function() {
                const idx = $(this).data('index');
                const rowData = instance.data[idx];
                if (rowData) {
                    instance.onRowClick(rowData, idx);
                }
            });
        }
    },

    /**
     * width 값 → col-w-* 클래스 매핑
     */
    _widthToColClass: function(width) {
        const px = parseInt(width);
        if (isNaN(px)) return '';
        const supported = [30,40,50,60,70,80,90,100,110,120,130,140,150,160,180,200,250,300];
        const closest = supported.find(w => w >= px) || supported[supported.length - 1];
        return `col-w-${closest}`;
    },

    /**
     * 중첩 객체 값 조회
     */
    _getNestedValue: function(obj, path) {
        if (!path) return obj;
        return path.split('.').reduce((o, p) => (o ? o[p] : undefined), obj);
    },

    /**
     * 정렬 정보 조회
     * @param {string} container - 컨테이너 선택자
     * @returns {object} - { sortBy, sortDirection }
     */
    getSortInfo: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return { sortBy: null, sortDirection: null };
        return {
            sortBy: instance.sortBy,
            sortDirection: instance.sortDirection
        };
    },

    /**
     * 정렬 정보 설정
     * @param {string} container - 컨테이너 선택자
     * @param {string} sortBy - 정렬 컬럼
     * @param {string} sortDirection - 정렬 방향
     */
    setSortInfo: function(container, sortBy, sortDirection) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        instance.sortBy = sortBy;
        instance.sortDirection = sortDirection;
        this._updateSortIcons(container);
    },

    /**
     * 데이터 조회
     * @param {string} container - 컨테이너 선택자
     * @returns {array} - 현재 데이터
     */
    getData: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        return instance ? instance.data : [];
    },

    /**
     * 특정 행 데이터 조회
     * @param {string} container - 컨테이너 선택자
     * @param {number} index - 행 인덱스
     * @returns {object} - 행 데이터
     */
    getRowData: function(container, index) {
        const data = this.getData(container);
        return data[index];
    },

    /**
     * 행 추가
     * @param {string} container - 컨테이너 선택자
     * @param {object} rowData - 추가할 행 데이터
     * @param {boolean} prepend - 맨 앞에 추가 여부
     */
    addRow: function(container, rowData, prepend) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        if (prepend) {
            instance.data.unshift(rowData);
        } else {
            instance.data.push(rowData);
        }
        this._renderRows(container);
    },

    /**
     * 행 삭제
     * @param {string} container - 컨테이너 선택자
     * @param {number} index - 삭제할 행 인덱스
     */
    removeRow: function(container, index) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        instance.data.splice(index, 1);
        this._renderRows(container);
    },

    /**
     * 선택된 행 하이라이트
     * @param {string} container - 컨테이너 선택자
     * @param {number} index - 행 인덱스
     */
    selectRow: function(container, index) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        const tableId = instance.config.id || `${instanceId}_table`;
        $(`#${tableId}Body tr`).removeClass('selected');
        $(`#${tableId}Body tr[data-index="${index}"]`).addClass('selected');
    },

    /**
     * 테이블 클리어
     * @param {string} container - 컨테이너 선택자
     */
    clear: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        instance.data = [];
        instance.sortBy = null;
        instance.sortDirection = null;
        this._updateSortIcons(container);
        this._renderRows(container);
    }
};
