package com.example.spideradmin.infra.tcp.server;

import com.example.spideradmin.infra.tcp.handler.CommandDispatcher;
import com.example.spideradmin.infra.tcp.model.JsonCommandRequest;
import com.example.spideradmin.infra.tcp.model.JsonCommandResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin TCP 서버에 연결된 클라이언트 1건을 처리하는 Runnable.
 *
 * <p>4바이트 길이 프리픽스 + UTF-8 JSON 형식으로 JsonCommandRequest를 수신하고,
 * CommandDispatcher에 위임한 뒤 JsonCommandResponse를 동일 형식으로 반환한다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class TcpClientHandler implements Runnable {

    private final Socket socket;
    private final CommandDispatcher commandDispatcher;
    private final ObjectMapper objectMapper;

    @Override
    public void run() {
        try (socket) {
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(60_000);

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            int length = dis.readInt();
            byte[] bytes = new byte[length];
            dis.readFully(bytes);

            JsonCommandRequest request = objectMapper.readValue(bytes, JsonCommandRequest.class);
            log.info("[TcpClientHandler] 수신: command={}, requestId={}", request.getCommand(), request.getRequestId());

            Object result;
            try {
                result = commandDispatcher.dispatch(request.getCommand(), request);
            } catch (Exception e) {
                // IllegalArgumentException뿐 아니라 일반 RuntimeException까지 포괄한다.
                // 하나의 커맨드 처리 실패가 전체 서버 루프를 망가뜨리지 않도록 응답으로 변환한다.
                log.warn(
                        "[TcpClientHandler] 커맨드 처리 중 예외: command={}, error={}",
                        request.getCommand(),
                        e.getMessage(),
                        e);
                result = JsonCommandResponse.builder()
                        .command(request.getCommand())
                        .success(false)
                        .error(e.getMessage())
                        .build();
            }

            byte[] responseBytes = objectMapper.writeValueAsBytes(result);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            dos.writeInt(responseBytes.length);
            dos.write(responseBytes);
            dos.flush();

            log.info("[TcpClientHandler] 응답 전송 완료: command={}", request.getCommand());
        } catch (IOException e) {
            log.warn("[TcpClientHandler] 처리 중 오류: {}", e.getMessage());
        }
    }
}
