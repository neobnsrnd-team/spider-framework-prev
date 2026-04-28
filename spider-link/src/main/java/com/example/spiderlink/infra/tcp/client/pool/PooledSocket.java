package com.example.spiderlink.infra.tcp.client.pool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * 소켓 풀에서 관리되는 Socket 래퍼.
 *
 * <p>Socket과 DataInputStream/DataOutputStream을 함께 보유하여 풀 반납 후에도
 * 스트림을 재사용할 수 있도록 한다. 마지막 사용 시각을 기록하여
 * {@link SocketPool}이 유효기간({@code maxIdleMs})을 판단하는 데 사용된다.</p>
 */
public class PooledSocket {

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    /** 마지막 사용 시각 (ms) — 풀 반납 시 갱신 */
    private volatile long lastUsed = System.currentTimeMillis();
    /** 오류 발생 시 true로 설정 — borrow 시 유효성 검사에서 탈락 */
    private volatile boolean invalid = false;

    public PooledSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new DataOutputStream(socket.getOutputStream());
        this.in = new DataInputStream(socket.getInputStream());
    }

    /**
     * 소켓이 유효한지 확인한다.
     *
     * @param maxIdleMs 최대 유휴 허용 시간 (ms)
     * @return 유효 여부
     */
    public boolean isValid(long maxIdleMs) {
        if (invalid || socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown()) {
            return false;
        }
        return (System.currentTimeMillis() - lastUsed) < maxIdleMs;
    }

    /** 마지막 사용 시각을 현재 시각으로 갱신한다 */
    public void touch() {
        lastUsed = System.currentTimeMillis();
    }

    /** 소켓을 무효화하고 닫는다 — IOException은 무시 */
    public void invalidate() {
        invalid = true;
        try {
            socket.close();
        } catch (Exception ignored) {}
    }

    public DataInputStream getIn() { return in; }
    public DataOutputStream getOut() { return out; }
    public long getLastUsed() { return lastUsed; }
}
