/**
 * Pagination Component
 * 페이지네이션 동적 생성기
 */
window.Pagination = {
    instances: {},

    /**
     * 페이지네이션 초기화
     * @param {string} container - 컨테이너 선택자 (pagination ul)
     * @param {string} infoContainer - 페이지 정보 컨테이너 선택자
     * @param {function} onPageChange - 페이지 변경 시 콜백
     */
    init: function(container, infoContainer, onPageChange) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        this.instances[instanceId] = {
            container,
            infoContainer,
            onPageChange,
            currentPage: 1,
            totalItems: 0,
            itemsPerPage: 20,
            maxButtons: 10
        };
        return this;
    },

    /**
     * 페이지네이션 업데이트
     * @param {string} container - 컨테이너 선택자
     * @param {object} pageData - 페이지 데이터 { currentPage, totalItems, itemsPerPage } 또는 API 응답
     */
    update: function(container, pageData) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        let instance = this.instances[instanceId];

        // 자동 초기화
        if (!instance) {
            this.init(container, '#pageInfo', null);
            instance = this.instances[instanceId];
        }

        // API 응답 형식 지원 (Spring Page 형식)
        if (pageData.totalElements !== undefined) {
            instance.currentPage = pageData.currentPage !== undefined
                ? pageData.currentPage : (pageData.number || 0) + 1;
            instance.totalItems = pageData.totalElements;
            instance.itemsPerPage = pageData.size || 20;
        } else {
            instance.currentPage = pageData.currentPage || 1;
            instance.totalItems = pageData.totalItems || 0;
            instance.itemsPerPage = pageData.itemsPerPage || 20;
        }

        this._render(container);
    },

    /**
     * 페이지네이션 렌더링
     */
    _render: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        const $pagination = $(instance.container);
        const totalPages = Math.ceil(instance.totalItems / instance.itemsPerPage) || 1;
        const currentPage = instance.currentPage;

        $pagination.empty();

        // First & Previous
        $pagination.append(`
            <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="1">
                    <i class="bi bi-chevron-double-left"></i>
                </a>
            </li>
            <li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${currentPage - 1}">
                    <i class="bi bi-chevron-left"></i>
                </a>
            </li>
        `);

        // Page numbers
        const maxButtons = instance.maxButtons;
        let startPage = Math.max(1, currentPage - Math.floor(maxButtons / 2));
        let endPage = Math.min(totalPages, startPage + maxButtons - 1);

        if (endPage - startPage + 1 < maxButtons) {
            startPage = Math.max(1, endPage - maxButtons + 1);
        }

        for (let i = startPage; i <= endPage; i++) {
            $pagination.append(`
                <li class="page-item ${i === currentPage ? 'active' : ''}">
                    <a class="page-link" href="#" data-page="${i}">${i}</a>
                </li>
            `);
        }

        // Next & Last
        $pagination.append(`
            <li class="page-item ${currentPage === totalPages || totalPages === 0 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${currentPage + 1}">
                    <i class="bi bi-chevron-right"></i>
                </a>
            </li>
            <li class="page-item ${currentPage === totalPages || totalPages === 0 ? 'disabled' : ''}">
                <a class="page-link" href="#" data-page="${totalPages}">
                    <i class="bi bi-chevron-double-right"></i>
                </a>
            </li>
        `);

        // Bind click events
        $pagination.find('a.page-link').on('click', (e) => {
            e.preventDefault();
            const $target = $(e.currentTarget);
            if ($target.parent().hasClass('disabled')) return;

            const page = parseInt($target.data('page'));
            if (page && page !== currentPage && page >= 1 && page <= totalPages) {
                this.goToPage(container, page);
            }
        });

        // Update info
        this._updateInfo(container);
    },

    /**
     * 페이지 정보 업데이트
     */
    _updateInfo: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance || !instance.infoContainer) return;

        const start = instance.totalItems === 0 ? 0 : (instance.currentPage - 1) * instance.itemsPerPage + 1;
        const end = Math.min(instance.currentPage * instance.itemsPerPage, instance.totalItems);

        $(instance.infoContainer).text(`${start} - ${end} of ${instance.totalItems} items`);
    },

    /**
     * 페이지 이동
     * @param {string} container - 컨테이너 선택자
     * @param {number} page - 이동할 페이지
     */
    goToPage: function(container, page) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return;

        const totalPages = Math.ceil(instance.totalItems / instance.itemsPerPage);
        if (page < 1 || page > totalPages) return;

        instance.currentPage = page;

        if (instance.onPageChange) {
            instance.onPageChange(page);
        }
    },

    /**
     * 현재 페이지 조회
     * @param {string} container - 컨테이너 선택자
     * @returns {number} - 현재 페이지
     */
    getCurrentPage: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        return instance ? instance.currentPage : 1;
    },

    /**
     * 현재 페이지 설정
     * @param {string} container - 컨테이너 선택자
     * @param {number} page - 페이지 번호
     */
    setCurrentPage: function(container, page) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (instance) {
            instance.currentPage = page;
        }
    },

    /**
     * 페이지당 아이템 수 설정
     * @param {string} container - 컨테이너 선택자
     * @param {number} itemsPerPage - 페이지당 아이템 수
     */
    setItemsPerPage: function(container, itemsPerPage) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (instance) {
            instance.itemsPerPage = itemsPerPage;
        }
    },

    /**
     * 총 아이템 수 조회
     * @param {string} container - 컨테이너 선택자
     * @returns {number} - 총 아이템 수
     */
    getTotalItems: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        return instance ? instance.totalItems : 0;
    },

    /**
     * 총 페이지 수 조회
     * @param {string} container - 컨테이너 선택자
     * @returns {number} - 총 페이지 수
     */
    getTotalPages: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (!instance) return 0;
        return Math.ceil(instance.totalItems / instance.itemsPerPage);
    },

    /**
     * 첫 페이지로 이동
     * @param {string} container - 컨테이너 선택자
     */
    goToFirst: function(container) {
        this.goToPage(container, 1);
    },

    /**
     * 마지막 페이지로 이동
     * @param {string} container - 컨테이너 선택자
     */
    goToLast: function(container) {
        this.goToPage(container, this.getTotalPages(container));
    },

    /**
     * 다음 페이지로 이동
     * @param {string} container - 컨테이너 선택자
     */
    goToNext: function(container) {
        this.goToPage(container, this.getCurrentPage(container) + 1);
    },

    /**
     * 이전 페이지로 이동
     * @param {string} container - 컨테이너 선택자
     */
    goToPrev: function(container) {
        this.goToPage(container, this.getCurrentPage(container) - 1);
    },

    /**
     * 초기화
     * @param {string} container - 컨테이너 선택자
     */
    reset: function(container) {
        const instanceId = container.replace(/[^a-zA-Z0-9]/g, '_');
        const instance = this.instances[instanceId];
        if (instance) {
            instance.currentPage = 1;
            instance.totalItems = 0;
            this._render(container);
        }
    }
};
