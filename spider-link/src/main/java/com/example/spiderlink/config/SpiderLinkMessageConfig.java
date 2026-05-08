package com.example.spiderlink.config;

import com.example.spiderlink.infra.tcp.parser.FixedLengthParser;
import com.example.spiderlink.infra.tcp.parser.MessageStructureCache;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * spider-link 전문 구조 로딩 설정.
 *
 * <p>MyBatis가 클래스패스에 있을 때만 활성화된다.
 * {@link MessageStructureCache}이 의존하는 {@code MessageMetaMapper}를 스캔하고,
 * {@link MessageStructureCache} / {@link FixedLengthParser} 빈을 컨텍스트에 등록한다.</p>
 *
 * <p>소비 모듈이 {@code com.example.spiderlink} 패키지를 직접 스캔하지 않아도
 * AutoConfiguration 체인을 통해 이 설정이 자동 적용된다.</p>
 */
@Configuration
@ConditionalOnClass(name = "org.mybatis.spring.annotation.MapperScan")
@MapperScan("com.example.spiderlink.domain.message.mapper")
@Import({MessageStructureCache.class, FixedLengthParser.class})
public class SpiderLinkMessageConfig {
}
