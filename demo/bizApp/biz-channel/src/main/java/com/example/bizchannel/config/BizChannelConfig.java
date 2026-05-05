package com.example.bizchannel.config;

import com.example.bizchannel.web.filter.JwtAuthFilter;
import com.example.bizchannel.web.interceptor.HttpLoggingInterceptor;
import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.example.spiderlink.infra.tcp.client.TcpClient;
import com.example.spiderlink.infra.tcp.codec.JsonMessageCodec;
import com.example.spidercommon.infra.tcp.handler.CommandDispatcher;
import com.example.spidercommon.infra.tcp.handler.CommandHandler;
import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import com.example.spiderlink.infra.tcp.server.SpiderTcpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 채널AP 공통 설정 클래스.
 *
 * <p>CORS 정책, JWT 필터, TCP 연결 정보(AP 서버 간, Admin), Admin 수신용 내장 TCP 서버 등록을 담당한다.</p>
 *
 * <pre>{@code
 *   // application.yml 설정 예시
 *   biz.auth.host: localhost
 *   biz.auth.port: 19100
 *   biz.transfer.host: localhost
 *   biz.transfer.port: 19200
 *   admin.secret: admin-secret
 *   admin.tcp.host: localhost        # biz-channel → Admin 방향 (기동 시 공지 상태 복원)
 *   admin.tcp.port: 9999
 *   tcp.server.port: 19400          # Admin이 공지 커맨드를 전송하는 내장 TCP 포트
 * }</pre>
 */
@Getter
@Configuration
public class BizChannelConfig implements WebMvcConfigurer {

    /** 인증AP(biz-auth) 접속 호스트 */
    @Value("${biz.auth.host:localhost}")
    private String authHost;

    /** 인증AP(biz-auth) TCP 포트 */
    @Value("${biz.auth.port:19100}")
    private int authPort;

    /** 이체AP(biz-transfer) 접속 호스트 */
    @Value("${biz.transfer.host:localhost}")
    private String transferHost;

    /** 이체AP(biz-transfer) TCP 포트 */
    @Value("${biz.transfer.port:19200}")
    private int transferPort;

    /** 공지 관리 API 보호용 어드민 시크릿 키 */
    @Value("${admin.secret:admin-secret}")
    private String adminSecret;

    /** biz-channel → Admin TCP 서버 접속 호스트 (기동 시 공지 상태 복원용) */
    @Value("${admin.tcp.host:localhost}")
    private String adminTcpHost;

    /** biz-channel → Admin TCP 서버 포트 (기동 시 공지 상태 복원용, 기본값: 9999) */
    @Value("${admin.tcp.port:9999}")
    private int adminTcpPort;

    /** Admin → biz-channel 인바운드 TCP 포트 (기본값: 19400) */
    @Value("${tcp.server.port:19400}")
    private int tcpServerPort;

    /** 요청 처리 스레드 풀 크기 (기본값: 5) */
    @Value("${tcp.server.handler-pool-size:5}")
    private int tcpHandlerPoolSize;

    /** 요청 대기 큐 최대 크기 (기본값: 20) */
    @Value("${tcp.server.queue-capacity:20}")
    private int tcpQueueCapacity;

    private final HttpLoggingInterceptor httpLoggingInterceptor;

    public BizChannelConfig(HttpLoggingInterceptor httpLoggingInterceptor) {
        this.httpLoggingInterceptor = httpLoggingInterceptor;
    }

    /** /api/** 경로 HTTP 거래 로그 인터셉터 등록 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(httpLoggingInterceptor).addPathPatterns("/api/**");
    }

    /**
     * spider-link TcpClient 빈 등록.
     *
     * <p>{@link TcpClient} 는 {@code com.example.spiderlink} 패키지에 선언되어 있어
     * 컴포넌트 스캔 범위에 포함되지 않으므로 명시적으로 등록한다.
     * {@link MessageInstanceRecorder} 가 존재하면 주입하여 전문 이력을 기록한다.</p>
     *
     * @param objectMapper Jackson ObjectMapper
     * @param recorder     전문 이력 기록기 (JdbcTemplate 빈이 없으면 empty)
     * @return TcpClient 인스턴스
     */
    @Bean
    public TcpClient tcpClient(ObjectMapper objectMapper,
                                Optional<MessageInstanceRecorder> recorder) {
        return new TcpClient(objectMapper, recorder.orElse(null));
    }

    /**
     * Admin 공지 커맨드 수신용 내장 TCP 서버 빈.
     *
     * <p>Admin이 NOTICE_SYNC / NOTICE_END 커맨드를 전송하면
     * {@link com.example.bizchannel.domain.notice.NoticeSyncCommandHandler}가 처리하여
     * {@link com.example.bizchannel.domain.notice.NoticeManager}를 통해 SSE 브로드캐스트한다.</p>
     *
     * @param objectMapper JSON 직렬화·역직렬화에 사용할 ObjectMapper
     * @param handlers     스프링 컨텍스트에 등록된 모든 CommandHandler 구현체 목록
     * @param recorder     전문 이력 기록기 (JdbcTemplate 빈이 없으면 empty)
     * @return {@code tcp.server.port}에서 수신 대기하는 SpiderTcpServer 인스턴스
     */
    @Bean
    public SpiderTcpServer<JsonCommandRequest, JsonCommandResponse> bizChannelTcpServer(
            ObjectMapper objectMapper,
            List<CommandHandler<JsonCommandRequest, JsonCommandResponse>> handlers,
            Optional<MessageInstanceRecorder> recorder) {

        CommandDispatcher<JsonCommandRequest, JsonCommandResponse> dispatcher =
                new CommandDispatcher<>(handlers);

        return new SpiderTcpServer<>(tcpServerPort, tcpHandlerPoolSize, tcpQueueCapacity,
                new JsonMessageCodec(objectMapper), dispatcher, recorder.orElse(null));
    }

    /**
     * CORS 전역 설정 빈.
     *
     * <p>React 개발 서버(localhost:3000 등) 에서의 쿠키 포함 요청을 허용하기 위해
     * {@code allowedOriginPattern} 으로 {@code http://localhost:*} 를 허용하고,
     * {@code allowCredentials=true} 를 설정한다.</p>
     *
     * @return 전체 경로({@code /**})에 CORS 정책을 적용한 {@link CorsConfigurationSource}
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 자격증명(쿠키) 포함 요청을 허용하므로 와일드카드 오리진 대신 패턴을 사용
        config.addAllowedOriginPattern("http://localhost:*");
        config.setAllowCredentials(true);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * CORS 필터 빈.
     *
     * <p>Spring Security 없는 순수 Spring MVC 환경에서는 {@link CorsConfigurationSource}만으로는
     * CORS가 적용되지 않는다. {@link CorsFilter}로 래핑하여 서블릿 필터 체인에 등록한다.</p>
     *
     * @return CORS 정책을 적용한 서블릿 필터
     */
    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }

    /**
     * JWT 인증 필터 서블릿 등록 빈.
     *
     * <p>{@link JwtAuthFilter} 를 서블릿 필터 체인에 등록하고,
     * {@code /api/*} 경로에만 적용한다.</p>
     *
     * @param jwtAuthFilter 스프링이 주입한 JWT 인증 필터 인스턴스
     * @return 필터 등록 정보 빈
     */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtFilterRegistration(JwtAuthFilter jwtAuthFilter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthFilter);
        registration.addUrlPatterns("/api/*");
        // 다른 필터보다 먼저 실행되도록 낮은 order 값 지정
        registration.setOrder(1);
        return registration;
    }
}
