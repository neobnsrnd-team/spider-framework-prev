package com.example.mockcore.infra;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 고정길이 바이너리 프로토콜 기반 TCP 서버.
 *
 * <p>프레이밍: 4바이트 길이 프리픽스(int, big-endian) + 고정길이 바이너리 바이트열.
 * spider-link TcpClient.send() 와 동일한 프레이밍을 사용하므로 별도 프로토콜 변환 불필요.</p>
 *
 * <p>요청 바이트의 첫 20바이트를 커맨드명으로 인식하고 등록된 {@link LegacyCoreHandler}에 위임한다.</p>
 */
@Slf4j
@Component
public class LegacyTcpServer implements ApplicationRunner {

    /** REQ 전문에서 커맨드 필드 길이 (COMMAND: C,20) */
    private static final int COMMAND_FIELD_LEN = 20;

    /** 수신 메시지 최대 허용 크기 (1 MB) */
    private static final int MAX_MSG_LEN = 1024 * 1024;

    @Value("${tcp.server.port:19300}")
    private int port;

    @Value("${tcp.server.handler-pool-size:10}")
    private int poolSize;

    private final List<LegacyCoreHandler> handlers;

    private volatile Map<String, LegacyCoreHandler> handlerMap;
    private volatile ServerSocket serverSocket;
    private ExecutorService threadPool;

    public LegacyTcpServer(List<LegacyCoreHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        handlerMap = handlers.stream()
                .collect(Collectors.toMap(LegacyCoreHandler::getCommand, Function.identity()));
        threadPool = Executors.newFixedThreadPool(poolSize);
        serverSocket = new ServerSocket(port);
        log.info("[LegacyTcpServer] TCP 서버 시작: port={}, handlers={}", port, handlerMap.keySet());

        Thread acceptThread = new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket client = serverSocket.accept();
                    threadPool.submit(() -> handleClient(client));
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        log.error("[LegacyTcpServer] accept 오류", e);
                    }
                }
            }
        }, "legacy-tcp-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    @PreDestroy
    public void stop() {
        log.info("[LegacyTcpServer] TCP 서버 종료 중...");
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                threadPool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("[LegacyTcpServer] TCP 서버 종료 완료");
    }

    private void handleClient(Socket client) {
        try (client;
             DataInputStream dis = new DataInputStream(client.getInputStream());
             DataOutputStream dos = new DataOutputStream(client.getOutputStream())) {

            int len = dis.readInt();
            if (len < 0 || len > MAX_MSG_LEN) {
                log.warn("[LegacyTcpServer] 비정상 메시지 길이: {}", len);
                return;
            }
            byte[] reqBytes = new byte[len];
            dis.readFully(reqBytes);

            String command = extractCommand(reqBytes);
            log.debug("[LegacyTcpServer] 요청 수신: command={}", command);

            LegacyCoreHandler handler = handlerMap.get(command);
            byte[] resBytes = handler != null
                    ? handler.handle(reqBytes)
                    : buildUnknownError(command);

            dos.writeInt(resBytes.length);
            dos.write(resBytes);
            dos.flush();

        } catch (IOException e) {
            log.debug("[LegacyTcpServer] 클라이언트 처리 오류: {}", e.getMessage());
        }
    }

    /** 요청 바이트 앞 COMMAND_FIELD_LEN 바이트를 trim하여 커맨드명 추출 */
    private String extractCommand(byte[] reqBytes) {
        int copyLen = Math.min(COMMAND_FIELD_LEN, reqBytes.length);
        return new String(Arrays.copyOf(reqBytes, copyLen)).trim();
    }

    /** 미등록 커맨드에 대한 최소 오류 응답 (SUCCESS=N + ERROR_MSG) */
    private byte[] buildUnknownError(String command) {
        log.warn("[LegacyTcpServer] 미등록 커맨드: {}", command);
        FixedMessageWriter writer = new FixedMessageWriter();
        writer.writeC("N", 1);
        writer.writeK("등록되지 않은 커맨드입니다: " + command, 200);
        return writer.toBytes();
    }
}
