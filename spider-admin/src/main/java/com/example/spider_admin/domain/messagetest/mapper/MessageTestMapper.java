package com.example.spider_admin.domain.messagetest.mapper;

import com.example.spider_admin.domain.messagetest.dto.MessageTestCreateRequest;
import com.example.spider_admin.domain.messagetest.dto.MessageTestResponse;
import com.example.spider_admin.domain.messagetest.dto.MessageTestUpdateRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 전문 테스트 Command Mapper (CRUD)
 * - insert / update / delete / 단건 조회
 */
@Mapper
public interface MessageTestMapper {

    /**
     * 단건 조회 (PK) - Response DTO 반환
     */
    MessageTestResponse selectResponseByTestSno(Long testSno);

    /**
     * 테스트 케이스 생성
     */
    void insertMessageTest(
            @Param("dto") MessageTestCreateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 테스트 케이스 수정
     */
    void updateMessageTest(
            @Param("dto") MessageTestUpdateRequest dto,
            @Param("lastUpdateDtime") String lastUpdateDtime,
            @Param("lastUpdateUserId") String lastUpdateUserId);

    /**
     * 테스트 케이스 삭제
     */
    void deleteByTestSno(Long testSno);

    /**
     * 사용자별 테스트 케이스 목록 조회
     */
    List<MessageTestResponse> findByUserId(@Param("userId") String userId);

    /**
     * 메시지ID로 테스트 케이스 조회 (모든 사용자)
     *
     * @param messageId 메시지 ID (필수)
     * @param headerYn 헤더 여부 필터 (Y/N, null이면 전체 조회)
     */
    List<MessageTestResponse> findByMessageId(@Param("messageId") String messageId, @Param("headerYn") String headerYn);

    /**
     * 메시지ID로 테스트 케이스 조회 with 검색 조건 (모든 사용자)
     *
     * @param messageId 메시지 ID (필수)
     * @param headerYn 헤더 여부 필터 (Y/N, null이면 전체 조회)
     * @param testName 테스트명 검색어 (부분 일치, null이면 검색하지 않음)
     * @param testData 테스트 데이터 검색어 (부분 일치, null이면 검색하지 않음)
     * @param userId 등록자 ID 검색어 (부분 일치, null이면 검색하지 않음)
     * @return 검색 조건에 맞는 테스트 케이스 목록
     */
    List<MessageTestResponse> findByMessageIdWithFilters(
            @Param("messageId") String messageId,
            @Param("headerYn") String headerYn,
            @Param("testName") String testName,
            @Param("testData") String testData,
            @Param("userId") String userId);
}
