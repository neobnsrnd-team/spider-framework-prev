/**
 * PrintUtil - 목록 데이터 인쇄 유틸리티
 *
 * DOM 캡처 방식이 아닌 원본 데이터를 기반으로 깔끔한 인쇄 테이블을 생성한다.
 *
 * 사용법:
 *   PrintUtil.print({
 *     title: '코드 그룹 목록',
 *     columns: [
 *       { key: 'codeGroupId', label: '코드그룹ID' },
 *       { key: 'codeGroupName', label: '코드그룹명' },
 *     ],
 *     data: DataTable.getData('#tableContainer')
 *   });
 */
window.PrintUtil = {

    /**
     * @param {object} options
     * @param {string}   options.title   - 인쇄 페이지 제목
     * @param {Array}    options.columns - [{ key, label }] 형식의 컬럼 정의
     * @param {Array}    options.data    - 인쇄할 데이터 배열
     */
    print: function({ title, columns, data }) {
        if (!data || data.length === 0) {
            Toast.warning('출력할 데이터가 없습니다.');
            return;
        }

        const headerCells = columns
            .map(col => `<th>${col.label}</th>`)
            .join('');

        const bodyRows = data
            .map(row => {
                const cells = columns
                    .map(col => `<td>${row[col.key] ?? ''}</td>`)
                    .join('');
                return `<tr>${cells}</tr>`;
            })
            .join('');

        const printWindow = window.open('', '_blank', 'width=1000,height=700');
        if (!printWindow) {
            Toast.warning('팝업이 차단되었습니다. 브라우저 팝업 차단을 해제해주세요.');
            return;
        }
        printWindow.document.write(
            '<!DOCTYPE html>' +
            '<html><head>' +
            '<meta charset="UTF-8">' +
            '<title>' + title + '</title>' +
            '<style>' +
            'body { font-family: \'Malgun Gothic\', sans-serif; font-size: 12px; margin: 20px; }' +
            'h3 { font-size: 14px; margin-bottom: 10px; }' +
            'table { width: 100%; border-collapse: collapse; }' +
            'th, td { border: 1px solid #ccc; padding: 5px 8px; text-align: center; }' +
            'th { background: #f0f0f0; font-weight: bold; }' +
            '</style>' +
            '</head><body>' +
            '<h3>' + title + '</h3>' +
            '<table>' +
            '<thead><tr>' + headerCells + '</tr></thead>' +
            '<tbody>' + bodyRows + '</tbody>' +
            '</table>' +
            '</body></html>'
        );
        printWindow.document.close();
        printWindow.focus();

        // afterprint 이벤트로 안전하게 닫기 (인쇄 다이얼로그 닫힌 후 실행)
        printWindow.addEventListener('afterprint', function() {
            printWindow.close();
        });
        printWindow.print();
    }
};
