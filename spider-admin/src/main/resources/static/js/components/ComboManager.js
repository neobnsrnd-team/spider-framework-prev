/**
 * ComboManager
 * static_code.json에 정의된 정적 코드 데이터를 관리하는 유틸리티
 *
 * [simple] 정적 콤보 (static_code.json 기반)
 *   ComboManager.simple.getList('USE_YN')
 *     → [{ value: 'Y', label: '사용' }, { value: 'N', label: '미사용' }]
 *
 *   ComboManager.simple.getListAutoText('USE_YN')
 *     → [{ value: '', label: '전체' }, { value: 'Y', label: '사용' }, ...]
 *
 *   ComboManager.simple.getText('USE_YN', 'Y')
 *     → '사용'
 *
 *   ComboManager.simple.populateSelect('#selectId', 'USE_YN')
 *   ComboManager.simple.populateSelect('#selectId', 'USE_YN', true, '전체')
 *
 * [db] 동적 콤보 (DB API 기반, 결과 캐싱)
 *   ComboManager.db.getList('FR20003').then(options => { ... })
 *     → Promise<[{ value: 'XX', label: '이름' }, ...]>
 *
 *   ComboManager.db.getListAutoText('FR20003').then(options => { ... })
 *     → Promise<[{ value: '', label: '전체' }, ...]>
 */
window.ComboManager = (function ($) {
    var POST_FIX = '_LIST';
    var comboMap = new Map();

    $.ajax({
        url: '/data/static_code.json',
        async: false,
        dataType: 'json',
        success: function (data) {
            for (var key in data) {
                if (data.hasOwnProperty(key)) {
                    comboMap.set(key, data[key]);
                }
            }
        },
        error: function () {
            console.error('ComboManager: static_code.json 로드 실패');
        }
    });

    function getList(key) {
        var entry = comboMap.get(key + POST_FIX);
        if (!entry) {
            console.warn('ComboManager: key를 찾을 수 없습니다 -', key + POST_FIX);
            return [];
        }
        var arr = entry[key];
        if (!Array.isArray(arr)) return [];
        return arr.map(function (item) {
            return { value: item.VALUE, label: item.NAME };
        });
    }

    function getListAutoText(key, text, value) {
        var list = getList(key).slice();
        list.unshift({
            value: (value !== undefined && value !== null) ? value : '',
            label: text || '전체'
        });
        return list;
    }

    return {
        simple: {
            /**
             * key에 해당하는 LIST 배열을 [{value, label}] 형식으로 반환 (SearchForm 호환)
             * @param {string} key  ex) 'USE_YN'
             * @returns {Array<{value: string, label: string}>}
             */
            getList: getList,

            /**
             * 첫 번째 행에 '전체' 옵션을 추가한 배열 반환
             * @param {string} key
             * @param {string} [text='전체']
             * @param {string} [value='']
             * @returns {Array<{value: string, label: string}>}
             */
            getListAutoText: getListAutoText,

            /**
             * value에 해당하는 label 반환 (테이블 렌더링용)
             * @param {string} key
             * @param {string} val
             * @returns {string}
             */
            getText: function (key, val) {
                var list = getList(key);
                var found = list.find(function (item) { return item.value === val; });
                return found ? found.label : val;
            },

            /**
             * <select> 요소에 옵션을 채운다
             * @param {string} selector
             * @param {string} key
             * @param {boolean} [withAll=false]  첫 번째 행에 '전체' 옵션 포함 여부
             * @param {string}  [allText='전체']
             */
            populateSelect: function (selector, key, withAll, allText) {
                var $select = $(selector);
                if (!$select.length) return;
                $select.empty();
                var list = withAll ? getListAutoText(key, allText) : getList(key);
                list.forEach(function (item) {
                    $select.append('<option value="' + item.value + '">' + item.label + '</option>');
                });
            }
        }
    };
}(jQuery));

/**
 * ComboManager.db
 * DB API를 통해 콤보 데이터를 조회하고 캐싱하는 유틸리티
 * 동일 URL은 최초 1회만 API 호출 후 캐시에서 반환
 *
 * 기본 매핑 함수: ApiResponse<{ code, codeName }[]> → [{value, label}]
 * 다른 응답 구조는 mapper 함수로 직접 변환
 *
 * 사용법:
 *   ComboManager.db.getList('/api/codes/by-group/FR20003')
 *   ComboManager.db.getList('/api/apps', res => res.data.map(a => ({ value: a.appId, label: a.appName })))
 *   ComboManager.db.getListAutoText('/api/codes/by-group/FR20003')
 */
ComboManager.db = (function ($) {
    var cache = new Map();

    var defaultMapper = function (response) {
        if (!response.success || !response.data) return [];
        return response.data
            .sort(function (a, b) {
                return (a.codeName || '').localeCompare(b.codeName || '');
            })
            .map(function (code) {
                return { value: code.code, label: code.codeName };
            });
    };

    function getList(url, mapper) {
        var mapFn = mapper || defaultMapper;
        return new Promise(function (resolve) {
            if (cache.has(url)) {
                resolve(cache.get(url).slice());
                return;
            }
            $.ajax({
                url: url,
                method: 'GET',
                success: function (response) {
                    var list = mapFn(response);
                    cache.set(url, list);
                    resolve(list.slice());
                },
                error: function () {
                    console.error('ComboManager.db: 로드 실패 -', url);
                    resolve([]);
                }
            });
        });
    }

    function getListAutoText(url, mapper, text, value) {
        return getList(url, mapper).then(function (list) {
            var result = list.slice();
            result.unshift({ value: (value != null ? value : ''), label: text || '전체' });
            return result;
        });
    }

    return {
        /**
         * URL로 DB 콤보 목록을 [{value, label}] 형식으로 반환 (캐싱)
         * @param {string} url     API URL  ex) '/api/codes/by-group/FR20003'
         * @param {Function} [mapper]  응답 → [{value, label}] 변환 함수 (기본: code/codeName 매핑)
         * @returns {Promise<Array<{value: string, label: string}>>}
         */
        getList: getList,

        /**
         * 첫 번째 행에 '전체' 옵션을 추가한 배열 반환
         * @param {string} url
         * @param {Function} [mapper]
         * @param {string} [text='전체']
         * @param {string} [value='']
         * @returns {Promise<Array<{value: string, label: string}>>}
         */
        getListAutoText: getListAutoText
    };
}(jQuery));