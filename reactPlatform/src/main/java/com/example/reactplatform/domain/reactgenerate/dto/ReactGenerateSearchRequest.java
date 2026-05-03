package com.example.reactplatform.domain.reactgenerate.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * React 코드 생성 이력 목록 검색 조건 DTO.
 *
 * <p>날짜 범위는 yyyyMMdd 형식으로 입력받으며, {@link #getFromDtime()}/{@link #getToDtime()}을 통해
 * DB 저장 형식(yyyyMMddHHmmss)으로 변환되어 MyBatis XML에서 사용된다.
 */
@Getter
@Setter
public class ReactGenerateSearchRequest {

    /** 화면 제목 부분 일치 검색. null이면 전체 조회. */
    private String title;

    /** 브랜드 정확 일치 (BrandType enum name). null이면 전체 조회. */
    private String brand;

    /** 서비스 도메인 정확 일치 (DomainType enum name). null이면 전체 조회. */
    private String domain;

    /** 화면 분류 정확 일치 (AUTH / MAIN / LIST 등). null이면 전체 조회. */
    private String category;

    /** 컴포넌트명 부분 일치 검색. null이면 전체 조회. */
    private String componentName;

    /** 상태 필터 (GENERATED / PENDING_APPROVAL / APPROVED / FAILED). null이면 전체 조회. */
    private String status;

    /** Code ID 부분 일치 검색. null이면 전체 조회. */
    private String codeId;

    /** 생성자 ID 부분 일치 검색. null이면 전체 조회. */
    private String createUserId;

    /** 검색 시작일 (yyyyMMdd). null이면 제한 없음. */
    private String fromDate;

    /** 검색 종료일 (yyyyMMdd). null이면 제한 없음. */
    private String toDate;

    /**
     * DB 비교용 시작 일시를 반환한다 (yyyyMMddHHmmss).
     * fromDate가 null이면 null을 반환하여 XML 조건에서 제외된다.
     *
     * @return yyyyMMdd000000 형식, fromDate가 null이면 null
     */
    public String getFromDtime() {
        return fromDate != null && !fromDate.isBlank() ? fromDate + "000000" : null;
    }

    /**
     * DB 비교용 종료 일시를 반환한다 (yyyyMMddHHmmss).
     * toDate가 null이면 null을 반환하여 XML 조건에서 제외된다.
     *
     * @return yyyyMMdd235959 형식, toDate가 null이면 null
     */
    public String getToDtime() {
        return toDate != null && !toDate.isBlank() ? toDate + "235959" : null;
    }

    /** 현재 페이지 번호 (1-based). */
    private int page = 1;

    /** 페이지당 표시 건수. 기본값 10. */
    private int size = 10;

    /**
     * Oracle ROWNUM 페이지네이션에서 시작 행 번호(0-based)를 반환한다.
     * 외부 쿼리의 {@code WHERE RNUM > offset} 조건에 사용한다.
     *
     * @return (page - 1) * size
     */
    public int getOffset() {
        return (page - 1) * size;
    }

    /**
     * Oracle ROWNUM 페이지네이션에서 끝 행 번호를 반환한다.
     * 내부 쿼리의 {@code WHERE ROWNUM <= endRow} 조건에 사용한다.
     *
     * @return page * size
     */
    public int getEndRow() {
        return page * size;
    }
}
