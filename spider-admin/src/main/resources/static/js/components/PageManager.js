/**
 * PageManager Component
 * 페이지 전체 관리 통합 컴포넌트
 */
window.PageManager = {
    instances: {},

    /**
     * 페이지 초기화
     * @param {object} config - 페이지 설정
     */
    init: function(config) {
        const pageId = config.pageId || 'default';

        this.instances[pageId] = {
            config,
            currentPage: 1,
            totalItems: 0,
            itemsPerPage: config.search?.limit?.default || 20,
            sortBy: null,
            sortDirection: null
        };

        // 컨테이너 ID 설정
        const containers = {
            search: config.containers?.search || '#searchContainer',
            table: config.containers?.table || '#tableContainer',
            pagination: config.containers?.pagination || '#pagination',
            pageInfo: config.containers?.pageInfo || '#pageInfo',
            buttons: config.containers?.buttons || '#actionButtons',
            modal: config.containers?.modal || '#modalContainer'
        };

        this.instances[pageId].containers = containers;

        // 1. 검색 폼 렌더링
        if (config.search) {
            SearchForm.render(containers.search, config.search, () => {
                this.instances[pageId].currentPage = 1;
                this.load(pageId);
            });
        }

        // 2. 테이블 렌더링
        if (config.table) {
            const tableConfig = {
                ...config.table,
                onSort: (sortBy, sortDirection) => {
                    this.instances[pageId].sortBy = sortBy;
                    this.instances[pageId].sortDirection = sortDirection;
                    this.instances[pageId].currentPage = 1;
                    this.load(pageId);
                }
            };

            DataTable.render(containers.table, tableConfig, (row, index) => {
                if (config.onRowClick) {
                    config.onRowClick(row, index);
                } else if (config.modal) {
                    this.openModal(pageId, row);
                }
            });
        }

        // 3. 페이지네이션 초기화
        Pagination.init(containers.pagination, containers.pageInfo, (page) => {
            this.instances[pageId].currentPage = page;
            this.load(pageId);
        });

        // 4. 버튼 바 렌더링
        if (config.buttons) {
            this._renderButtons(pageId, containers.buttons, config.buttons);
        }

        // 5. 모달 초기화
        if (config.modal) {
            const modalConfig = {
                ...config.modal,
                onSave: (data, mode) => {
                    if (config.onSave) {
                        config.onSave(data, mode, () => {
                            Modal.close(containers.modal);
                            this.load(pageId);
                        });
                    } else {
                        this._defaultSave(pageId, data, mode);
                    }
                },
                onDelete: (data) => {
                    if (config.onDelete) {
                        config.onDelete(data, () => {
                            Modal.close(containers.modal);
                            this.load(pageId);
                        });
                    } else {
                        this._defaultDelete(pageId, data);
                    }
                }
            };

            Modal.init(containers.modal, modalConfig);
        }

        // 6. 새로고침 버튼 바인딩
        $('#btnRefresh').off('click').on('click', () => {
            this.load(pageId);
        });

        // 7. 엑셀/출력 버튼 바인딩
        $('#btnExcel').off('click').on('click', () => {
            if (config.onExport) {
                config.onExport();
            } else {
                this.exportExcel(pageId);
            }
        });

        $('#btnPrint').off('click').on('click', () => {
            if (config.onPrint) {
                config.onPrint();
            } else {
                window.print();
            }
        });

        // 8. 자동 로드
        if (config.autoLoad !== false) {
            this.load(pageId);
        }

        return this;
    },

    /**
     * 버튼 바 렌더링
     */
    _renderButtons: function(pageId, container, buttons) {
        const $container = $(container);
        if (!$container.length) return;

        let html = '<div class="bottom-actions">';

        buttons.forEach(btn => {
            const btnClass = btn.primary ? 'btn-primary' : (btn.danger ? 'btn-danger' : 'btn-secondary');
            const icon = btn.icon ? `<i class="${btn.icon}"></i> ` : '';

            html += `
                <button type="button" class="btn-action ${btnClass}" data-action="${btn.action}">
                    ${icon}${btn.label}
                </button>
            `;
        });

        html += '</div>';
        $container.html(html);

        // 버튼 이벤트 바인딩
        $container.find('[data-action]').on('click', (e) => {
            const action = $(e.currentTarget).data('action');
            this._handleButtonAction(pageId, action);
        });
    },

    /**
     * 버튼 액션 처리
     */
    _handleButtonAction: function(pageId, action) {
        const instance = this.instances[pageId];
        if (!instance) return;

        const config = instance.config;

        switch (action) {
            case 'create':
            case 'add':
                this.openModal(pageId);
                break;
            case 'refresh':
                this.load(pageId);
                break;
            case 'export':
                this.exportExcel(pageId);
                break;
            default:
                if (config.onButtonClick) {
                    config.onButtonClick(action);
                }
        }
    },

    /**
     * 데이터 로드
     * @param {string} pageId - 페이지 ID
     */
    load: function(pageId) {
        const instance = this.instances[pageId];
        if (!instance) {
            console.error('PageManager: Instance not found:', pageId);
            return;
        }

        const config = instance.config;
        const containers = instance.containers;

        // 검색 파라미터 수집
        const searchParams = SearchForm.getParams(containers.search);

        // 페이지네이션 파라미터
        const params = {
            ...searchParams,
            page: instance.currentPage,
            size: searchParams.size || instance.itemsPerPage
        };

        // 정렬 파라미터
        if (instance.sortBy) {
            params.sortBy = instance.sortBy;
            params.sortDirection = instance.sortDirection || 'ASC';
        }

        // 커스텀 로드 함수가 있으면 사용
        if (config.onLoad) {
            config.onLoad(params, (data, totalItems) => {
                DataTable.setData(containers.table, data);
                instance.totalItems = totalItems;
                instance.itemsPerPage = params.size;
                Pagination.update(containers.pagination, {
                    currentPage: instance.currentPage,
                    totalItems: totalItems,
                    itemsPerPage: params.size
                });
            });
            return;
        }

        // 기본 AJAX 로드
        $.ajax({
            url: config.apiEndpoint,
            method: 'GET',
            data: params,
            success: (response) => {
                if (response.success && response.data) {
                    const pageData = response.data;
                    const content = pageData.content || pageData;

                    DataTable.setData(containers.table, content);

                    // 페이지네이션 업데이트
                    if (pageData.totalElements !== undefined) {
                        instance.totalItems = pageData.totalElements;
                        instance.itemsPerPage = pageData.size || params.size;
                        Pagination.update(containers.pagination, pageData);
                    } else if (Array.isArray(content)) {
                        instance.totalItems = content.length;
                        Pagination.update(containers.pagination, {
                            currentPage: instance.currentPage,
                            totalItems: content.length,
                            itemsPerPage: params.size
                        });
                    }
                } else {
                    DataTable.setData(containers.table, []);
                    Pagination.reset(containers.pagination);
                }
            },
            error: (xhr) => {
                console.error('Failed to load data:', xhr);
                Toast.error('데이터를 불러오는데 실패했습니다.');
                DataTable.setData(containers.table, []);
                Pagination.reset(containers.pagination);
            }
        });
    },

    /**
     * 모달 열기
     * @param {string} pageId - 페이지 ID
     * @param {object} data - 수정할 데이터 (없으면 생성 모드)
     */
    openModal: function(pageId, data) {
        const instance = this.instances[pageId];
        if (!instance) return;

        const containers = instance.containers;

        if (data) {
            Modal.openForEdit(containers.modal, data);
        } else {
            Modal.openForCreate(containers.modal);
        }
    },

    /**
     * 기본 저장 처리
     */
    _defaultSave: function(pageId, data, mode) {
        const instance = this.instances[pageId];
        if (!instance) return;

        const config = instance.config;
        const containers = instance.containers;

        // 유효성 검사
        if (!Modal.validate(containers.modal)) {
            Toast.warning('필수 항목을 입력해주세요.');
            return;
        }

        const method = mode === 'create' ? 'POST' : 'PUT';
        const url = mode === 'create' ? config.apiEndpoint : `${config.apiEndpoint}/${data[config.idField || 'id']}`;

        $.ajax({
            url: url,
            method: method,
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: (response) => {
                if (response.success) {
                    Toast.success(mode === 'create' ? '등록되었습니다.' : '수정되었습니다.');
                    Modal.close(containers.modal);
                    this.load(pageId);
                } else {
                    Toast.error(response.message || '저장에 실패했습니다.');
                }
            },
            error: (xhr) => {
                console.error('Save failed:', xhr);
                Toast.error('저장에 실패했습니다.');
            }
        });
    },

    /**
     * 기본 삭제 처리
     */
    _defaultDelete: function(pageId, data) {
        const instance = this.instances[pageId];
        if (!instance) return;

        const config = instance.config;
        const containers = instance.containers;

        Toast.confirm('삭제하시겠습니까?', () => {
            $.ajax({
                url: `${config.apiEndpoint}/${data[config.idField || 'id']}`,
                method: 'DELETE',
                success: (response) => {
                    if (response.success) {
                        Toast.success('삭제되었습니다.');
                        Modal.close(containers.modal);
                        this.load(pageId);
                    } else {
                        Toast.error(response.message || '삭제에 실패했습니다.');
                    }
                },
                error: (xhr) => {
                    console.error('Delete failed:', xhr);
                    Toast.error('삭제에 실패했습니다.');
                }
            });
        });
    },

    /**
     * 엑셀 내보내기
     * @param {string} pageId - 페이지 ID
     */
    exportExcel: function(pageId) {
        const instance = this.instances[pageId];
        if (!instance) return;

        const config = instance.config;
        window.location.href = `${config.apiEndpoint}/export`;
    },

    /**
     * 현재 페이지 조회
     * @param {string} pageId - 페이지 ID
     * @returns {number} - 현재 페이지
     */
    getCurrentPage: function(pageId) {
        const instance = this.instances[pageId];
        return instance ? instance.currentPage : 1;
    },

    /**
     * 페이지 이동
     * @param {string} pageId - 페이지 ID
     * @param {number} page - 이동할 페이지
     */
    goToPage: function(pageId, page) {
        const instance = this.instances[pageId];
        if (!instance) return;

        instance.currentPage = page;
        this.load(pageId);
    },

    /**
     * 검색 초기화
     * @param {string} pageId - 페이지 ID
     */
    resetSearch: function(pageId) {
        const instance = this.instances[pageId];
        if (!instance) return;

        SearchForm.reset(instance.containers.search);
        instance.currentPage = 1;
        instance.sortBy = null;
        instance.sortDirection = null;
        DataTable.setSortInfo(instance.containers.table, null, null);
    },

    /**
     * 새로고침
     * @param {string} pageId - 페이지 ID
     */
    refresh: function(pageId) {
        this.load(pageId);
    },

    /**
     * 테이블 데이터 조회
     * @param {string} pageId - 페이지 ID
     * @returns {array} - 테이블 데이터
     */
    getData: function(pageId) {
        const instance = this.instances[pageId];
        if (!instance) return [];
        return DataTable.getData(instance.containers.table);
    },

    /**
     * 인스턴스 조회
     * @param {string} pageId - 페이지 ID
     * @returns {object} - 인스턴스
     */
    getInstance: function(pageId) {
        return this.instances[pageId];
    }
};
