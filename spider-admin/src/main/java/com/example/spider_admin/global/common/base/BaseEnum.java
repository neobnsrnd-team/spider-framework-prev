package com.example.spider_admin.global.common.base;

/**
 * <h3>Enum 공통 인터페이스</h3>
 * <p>DB 코드 값과 설명을 가지는 모든 Enum의 기본 인터페이스입니다.</p>
 * <p>TypeHandler에서 일관된 방식으로 Enum을 처리하기 위해 사용됩니다.</p>
 */
public interface BaseEnum {

    /**
     * DB에 저장되는 코드 값을 반환합니다.
     * @return DB 코드 값 (예: "1", "W", "R")
     */
    String getCode();

    /**
     * 코드에 대한 설명을 반환합니다.
     * @return 코드 설명 (예: "정상", "쓰기/읽기")
     */
    String getDescription();
}
