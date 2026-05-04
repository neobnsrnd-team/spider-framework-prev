package com.example.spiderlink.infra.tcp.server;

import com.example.spiderlink.domain.messageinstance.MessageInstanceRecorder;
import com.example.spiderlink.infra.tcp.codec.MessageCodec;
import com.example.spidercommon.infra.tcp.handler.CommandDispatcher;
import com.example.spidercommon.infra.tcp.model.HasCommand;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.lang.Nullable;

/**
 * codec + dispatcher 주입형 공통 TCP 서버 인프라.
 *
 * <p>Spring {@code @Bean}으로 생성하여 {@code ApplicationRunner}로 자동 기동한다.
 * 프로토콜 직렬화는 {@link MessageCodec} 구현체에 위임하고,
 * 커맨드 처리는 {@link CommandDispatcher}에 위임한다.</p>
 *
 * <p>{@link MessageInstanceRecorder}를 주입하면 수신 요청과 전송 응답을
 * {@code FWK_MESSAGE_INSTANCE} 테이블에 자동 기록한다. null이면 기록하지 않는다.</p>
 *
 * <pre>{@code
 * // 기록 없이 사용 (기존 방식)
 * return new SpiderTcpServer<>(port, handlerPoolSize, queueCapacity, codec, dispatcher);
 *
 * // 기록 포함 사용
 * return new SpiderTcpServer<>(port, handlerPoolSize, queueCapacity, codec, dispatcher, recorder);
 * }</pre>
 *
 * @param <REQ> 요청 타입 — {@link HasCommand}를 구현해야 커맨드 디스패치 가능
 * @param <RES> 응답 타입
 */
@Slf4j
public class SpiderTcpServer<REQ extends HasCommand, RES> implements ApplicationRunner {

    private final int port;
    private final int handlerPoolSize;
    /** 대기 큐 용량 — 초과 시 RejectedExecutionException 발생, 소켓을 즉시 닫아 클라이언트에 신호 */
    private final int queueCapacity;
    private final MessageCodec<REQ, RES> codec;
    private final CommandDispatcher<REQ, RES> dispatcher;
    /** 전문 이력 기록기 — null이면 DB 기록 생략 */
    @Nullable
    private final MessageInstanceRecorder recorder;

    /** accept 루프를 깨우기 위해 @PreDestroy에서 닫을 ServerSocket 참조 */
    private volatile ServerSocket serverSocket;

    /** 클라이언트 요청 처리 스레드 풀 */
    private ExecutorService handlerPool;

    /** 핸들러 스레드 번호 생성용 카운터 */
    private final AtomicInteger handlerCount = new AtomicInteger(0);

    /** DB 기록 없이 생성하는 기본 생성자 */
    public SpiderTcpServer(int port, int handlerPoolSize, int queueCapacity,
                           MessageCodec<REQ, RES> codec, CommandDispatcher<REQ, RES> dispatcher) {
        this(port, handlerPoolSize, queueCapacity, codec, dispatcher, null);
    }

    /**
     * DB 기록을 포함하는 생성자.
     *
     * @param recorder 전문 이력 기록기 (null이면 기록 생략)
     */
    public SpiderTcpServer(int port, int handlerPoolSize, int queueCapacity,
                           MessageCodec<REQ, RES> codec, CommandDispatcher<REQ, RES> dispatcher,
                           @Nullable MessageInstanceRecorder recorder) {
        this.port = port;
        this.handlerPoolSize = handlerPoolSize;
        this.queueCapacity = queueCapacity;
        this.codec = codec;
        this.dispatcher = dispatcher;
        this.recorder = recorder;
    }

    @Override
    public void run(ApplicationArguments args) {
        // ArrayBlockingQueue로 큐 상한을 지정해 OOM 방지.
        // 큐가 가득 차면 RejectedExecutionException → startServer에서 소켓을 즉시 닫아 클라이언트에 신호.
        handlerPool = new ThreadPoolExecutor(
                handlerPoolSize, handlerPoolSize,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                r -> {
                    // non-daemon 스레드: JVM 종료 시 진행 중인 요청이 완료될 때까지 대기
                    Thread t = new Thread(r, "spider-tcp-handler-" + port + "-" + handlerCount.incrementAndGet());
                    t.setDaemon(false);
                    return t;
                });
        Thread serverThread = new Thread(this::startServer, "spider-tcp-server-" + port);
        // accept 루프 스레드는 daemon: shutdown은 @PreDestroy에서 socket close로 처리
        serverThread.setDaemon(true);
        serverThread.start();
        log.info("[SpiderTcpServer:{}] TCP 서버 시작 (handlerPoolSize={})", port, handlerPoolSize);
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            log.info("[SpiderTcpServer:{}] 포트 {} 에서 대기 중", port, port);
            while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                try {
                    handlerPool.submit(() -> handleClient(clientSocket));
                } catch (RejectedExecutionException e) {
                    // 풀 포화 또는 shutdown 중 — 소켓을 즉시 닫아 리소스 누수 방지
                    log.error("[SpiderTcpServer:{}] 핸들러 풀 포화, 소켓 닫음: {}", port, e.getMessage());
                    try { clientSocket.close(); } catch (IOException ignored) {}
                }
            }
        } catch (IOException e) {
            if (serverSocket != null && serverSocket.isClosed()) {
                log.info("[SpiderTcpServer:{}] ServerSocket 종료됨 (정상 shutdown)", port);
            } else {
                log.error("[SpiderTcpServer:{}] 서버 소켓 오류: {}", port, e.getMessage(), e);
            }
        }
    }

    /**
     * 개별 클라이언트 연결을 처리한다.
     * codec으로 요청을 역직렬화하고, dispatcher로 핸들러에 위임한 뒤, 응답을 직렬화하여 반환한다.
     * recorder가 설정된 경우 요청·응답을 FWK_MESSAGE_INSTANCE에 기록한다.
     */
    private void handleClient(Socket socket) {
        String trxId = UUID.randomUUID().toString();
        try (socket) {
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(60_000);

            REQ request = codec.decode(socket.getInputStream());
            if (request == null) {
                log.warn("[SpiderTcpServer:{}] 수신된 요청이 null입니다. 소켓을 닫습니다.", port);
                return;
            }
            log.info("[SpiderTcpServer:{}] 수신: command={}", port, request.getCommand());

            if (recorder != null) {
                recorder.recordServerRequest(trxId, request, port);
            }

            RES response = dispatcher.dispatch(request);
            codec.encode(socket.getOutputStream(), response);

            if (recorder != null) {
                recorder.recordServerResponse(trxId, request, response, port);
            }

            log.info("[SpiderTcpServer:{}] 응답 전송 완료: command={}", port, request.getCommand());
        } catch (IOException e) {
            log.error("[SpiderTcpServer:{}] 클라이언트 처리 실패: {}", port, e.getMessage(), e);
        } catch (Exception e) {
            // dispatcher/codec에서 RuntimeException 발생 시 — 소켓을 닫아 클라이언트가 EOF로 인지하도록 함
            log.error("[SpiderTcpServer:{}] 핸들러 처리 중 예외 발생: {}", port, e.getMessage(), e);
        }
    }

    /**
     * Spring 종료 시 ServerSocket을 닫아 accept 루프를 빠져나오게 한다.
     * 핸들러 풀은 graceful shutdown: 진행 중인 요청은 30초 내 완료를 기다린다.
     */
    @PreDestroy
    public void shutdown() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try { serverSocket.close(); } catch (IOException ignored) {}
            log.info("[SpiderTcpServer:{}] TCP 서버 소켓 닫음 (shutdown)", port);
        }
        if (handlerPool != null) {
            handlerPool.shutdown();
            try {
                if (!handlerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    handlerPool.shutdownNow();
                    log.warn("[SpiderTcpServer:{}] 핸들러 풀 강제 종료 (30초 초과)", port);
                }
            } catch (InterruptedException e) {
                handlerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
