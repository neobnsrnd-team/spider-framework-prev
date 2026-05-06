package com.example.spiderlink.domain.gateway.mapper;

import com.example.spiderlink.domain.gateway.dto.GatewayConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * FWK_GATEWAY 조회 전용 Mapper.
 *
 * <p>GatewayLoader가 기동 시 FWK_GATEWAY에서 게이트웨이 설정을 읽어
 * SpiderTcpServer를 동적으로 생성하는 데 사용한다.</p>
 */
@Mapper
public interface GatewayMapper {

    /**
     * GW_ID로 게이트웨이 설정을 조회한다.
     *
     * @param gwId 게이트웨이 ID
     * @return 게이트웨이 설정, 미등록이면 null
     */
    GatewayConfig selectGateway(@Param("gwId") String gwId);
}
