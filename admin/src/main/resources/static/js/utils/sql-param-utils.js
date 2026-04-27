/**
 * @file sql-param-utils.js
 * @description SQL 문자열에서 바인딩 파라미터를 감지하는 유틸리티.
 *              JDBC '?' 방식과 MyBatis '#{varName}' 방식을 모두 지원한다.
 *              문자열 리터럴 및 주석 내부의 파라미터는 제외한다.
 */
window.SqlParamUtils = window.SqlParamUtils || {};

(function (SqlParamUtils) {
  /**
   * SQL에서 바인딩 파라미터를 감지한 결과 타입.
   * @typedef {Object} ParamDetectResult
   * @property {'jdbc'|'mybatis'|'none'} type  - 감지된 파라미터 방식
   * @property {number}                  count - 파라미터 총 개수
   * @property {string[]}                names - MyBatis 방식일 때 변수명 목록 (jdbc면 빈 배열)
   */

  /**
   * SQL 문자열에서 주석과 문자열 리터럴을 제거한 순수 SQL을 반환한다.
   *
   * 제거 대상:
   * - 블록 주석: /* ... *\/
   * - 한 줄 주석: -- ...
   * - 문자열 리터럴: '...' (이스케이프된 '' 포함)
   *
   * @param {string} sql
   * @returns {string}
   */
  function stripCommentsAndLiterals(sql) {
    let result = "";
    let i = 0;
    const len = sql.length;

    while (i < len) {
      // 블록 주석 /* ... */
      if (sql[i] === "/" && sql[i + 1] === "*") {
        i += 2;
        while (i < len && !(sql[i] === "*" && sql[i + 1] === "/")) {
          i++;
        }
        i += 2; // */ 건너뜀
        continue;
      }

      // 한 줄 주석 -- ...
      if (sql[i] === "-" && sql[i + 1] === "-") {
        i += 2;
        while (i < len && sql[i] !== "\n" && sql[i] !== "\r") {
          i++;
        }
        continue;
      }

      // 문자열 리터럴 '...' ('' 이스케이프 처리)
      if (sql[i] === "'") {
        i++;
        while (i < len) {
          if (sql[i] === "'" && sql[i + 1] === "'") {
            i += 2; // 이스케이프된 '' 건너뜀
          } else if (sql[i] === "'") {
            i++; // 닫는 따옴표
            break;
          } else {
            i++;
          }
        }
        continue;
      }

      result += sql[i];
      i++;
    }

    return result;
  }

  /**
   * SQL 문자열에서 바인딩 파라미터를 감지한다.
   *
   * - MyBatis #{varName} 가 하나라도 있으면 'mybatis' 방식으로 판단
   * - 그 외 JDBC '?' 가 있으면 'jdbc' 방식으로 판단
   * - 둘 다 없으면 'none'
   *
   * @param {string} sql - 분석할 SQL 문자열
   * @returns {ParamDetectResult}
   */
  SqlParamUtils.detect = function (sql) {
    if (!sql || !sql.trim()) {
      return { type: "none", count: 0, names: [] };
    }

    const clean = stripCommentsAndLiterals(sql);

    // MyBatis #{varName} 감지 — #{} 빈 중괄호(변수명 없음)도 파라미터로 인식
    const mybatisMatches = clean.match(/#\{([^}]*)\}/g);
    if (mybatisMatches && mybatisMatches.length > 0) {
      // 중복 제거 없이 순서대로 이름 추출 (같은 변수가 여러 번 나올 수 있음)
      const names = mybatisMatches.map((m) =>
        m.replace(/^#\{/, "").replace(/\}$/, "").trim(),
      );
      return { type: "mybatis", count: names.length, names };
    }

    // JDBC ? 감지
    const jdbcCount = (clean.match(/\?/g) || []).length;
    if (jdbcCount > 0) {
      return { type: "jdbc", count: jdbcCount, names: [] };
    }

    return { type: "none", count: 0, names: [] };
  };

  /**
   * 파라미터 개수만 빠르게 반환한다.
   *
   * @param {string} sql
   * @returns {number}
   */
  SqlParamUtils.count = function (sql) {
    return SqlParamUtils.detect(sql).count;
  };
})(window.SqlParamUtils);
