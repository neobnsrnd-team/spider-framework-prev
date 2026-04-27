package com.example.spiderbatch.domain.batch.mapper;

import com.example.spiderbatch.domain.batch.dto.BatchAppInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * FWK_BATCH_APP 조회 Mapper.
 *
 * <p>배치 실행 시 BATCH_APP_FILE_NAME(= JobRegistry Bean 이름)을 조회하거나,
 * 모니터링 시 배치 이름과 CRON 표현식을 조회하는 용도로 사용한다.</p>
 */
@Mapper
public interface BatchAppMapper {

    /**
     * batchAppId로 배치 파일명(= Job Bean 이름) 조회.
     *
     * @param batchAppId 배치 APP ID
     * @return BATCH_APP_FILE_NAME (JobRegistry에 등록된 Job 이름), 없으면 null
     */
    String selectBatchAppFileName(@Param("batchAppId") String batchAppId);

    /**
     * batchAppId로 배치 앱 기본 정보(이름 + CRON 표현식) 조회.
     * 실행 중인 배치 조회 시 표시 정보를 보강하기 위해 사용한다.
     *
     * @param batchAppId 배치 APP ID
     * @return BATCH_APP_NAME, CRON_TEXT를 담은 BatchAppInfo, 없으면 null
     */
    BatchAppInfo selectBatchAppInfo(@Param("batchAppId") String batchAppId);
}
