package com.example.spider_admin.infra.tcp.server;

import com.example.spider_admin.infra.tcp.handler.CommandDispatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Admin TCP 서버 (포트 9999).
 *
 * <p>ApplicationRunner로 Spring Boot 기동 시 자동 시작된다.
 * 클라이언트 요청은 고정 크기 스레드 풀(기본 20)로 처리한다.
 * 추후 이슈 #93에서 WebFlux Reactor Netty 이벤트 루프로 전환 예정.</p>
 *
 * <p>수신 프로토콜: 4바이트 길이 프리픽스 + UTF-8 JSON (JsonCommandRequest)</p>
 *
 * <p>현재 호출자: biz-channel → Admin 전문 통신 (구현 진행 중).
 * 이슈 #92(Kafka) 구현 시 Kafka consumer가 Admin TCP로 커맨드를 중계할 예정.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TcpServer implements ApplicationRunner {

    @Value("${tcp.server.port:9999}")
    private int tcpPort;

    /** 핸들러 스레드 풀 크기 — 동시 접속 급증 시 무제한 스레드 생성 방지 (기본값 20) */
    @Value("${tcp.server.handler-pool-size:20}")
    private int handlerPoolSize;

    private final CommandDispatcher commandDispatcher;
    private final ObjectMapper objectMapper;

    /** accept 루프를 깨우기 위해 @PreDestroy에서 닫을 ServerSocket 참조 */
    private volatile ServerSocket serverSocket;

    /** 클라이언트 요청 처리 스레드 풀 */
    private ExecutorService handlerPool;

    /** 핸들러 스레드 번호 생성용 카운터 */
    private final AtomicInteger handlerCount = new AtomicInteger(0);

    @Override
    public void run(ApplicationArguments args) {
        // 고정 크기 스레드 풀 생성: non-daemon 스레드로 JVM 종료 시 진행 중인 요청 완료 보장
        handlerPool = Executors.newFixedThreadPool(handlerPoolSize, r -> {
            Thread t = new Thread(r, "admin-tcp-handler-" + handlerCount.incrementAndGet());
            t.setDaemon(false);
            return t;
        });
        Thread serverThread = new Thread(this::startServer, "admin-tcp-server");
        // 서버 accept 루프 스레드는 daemon 유지 (shutdown은 @PreDestroy에서 socket close로 처리)
        serverThread.setDaemon(true);
        serverThread.start();
        log.info("[TcpServer] Admin TCP 서버 스레드 시작 (port={}, handlerPoolSize={})", tcpPort, handlerPoolSize);
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(tcpPort);
            log.info("[TcpServer] 포트 {} 에서 대기 중", tcpPort);
            while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                try {
                    // 스레드 풀에 핸들러 제출: 풀 포화 시 RejectedExecutionException으로 소켓 누수 방지
                    handlerPool.submit(new TcpClientHandler(clientSocket, commandDispatcher, objectMapper));
                } catch (RejectedExecutionException e) {
                    // 풀 포화(모든 스레드 사용 중) 또는 shutdown 중인 경우 소켓을 즉시 닫아 리소스 누수 방지
                    log.error("[TcpServer] 핸들러 풀 포화, 소켓 닫음: {}", e.getMessage());
                    try {
                        clientSocket.close();
                    } catch (IOException ignored) {
                        // close 실패는 무시 (리소스 정리 단계)
                    }
                }
            }
        } catch (IOException e) {
            // serverSocket.close()로 accept()가 SocketException을 던지는 경우는 정상 종료로 처리
            if (serverSocket != null && serverSocket.isClosed()) {
                log.info("[TcpServer] ServerSocket 종료됨 (정상 shutdown)");
            } else {
                log.error("[TcpServer] 서버 소켓 오류: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Spring 종료 시 ServerSocket을 닫아 accept 루프를 빠져나오게 한다.
     * 스레드 풀은 graceful shutdown: 진행 중인 요청은 30초 내 완료를 기다린다.
     */
    @PreDestroy
    public void shutdown() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // close 실패는 무시 (이미 종료 단계)
            }
            log.info("[TcpServer] TCP 서버 소켓 닫음 (shutdown)");
        }
        if (handlerPool != null) {
            handlerPool.shutdown();
            try {
                if (!handlerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    handlerPool.shutdownNow();
                    log.warn("[TcpServer] 핸들러 풀 강제 종료 (30초 초과)");
                }
            } catch (InterruptedException e) {
                handlerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
