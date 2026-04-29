package com.example.spiderlink.infra.tcp.codec;

import com.example.spidercommon.infra.tcp.model.JsonCommandRequest;
import com.example.spidercommon.infra.tcp.model.JsonCommandResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import lombok.RequiredArgsConstructor;

/**
 * JSON TCP 프로토콜 코덱 (4바이트 길이 프리픽스 + UTF-8 JSON).
 *
 * <p>Admin ↔ demo/backend 구간에서 사용한다.</p>
 */
@RequiredArgsConstructor
public class JsonMessageCodec implements MessageCodec<JsonCommandRequest, JsonCommandResponse> {

    /** 수신 메시지 최대 허용 크기 (1 MB) — 초과 시 비정상 요청으로 간주 */
    private static final int MAX_MSG_LEN = 1024 * 1024;

    private final ObjectMapper objectMapper;

    @Override
    public JsonCommandRequest decode(InputStream in) throws IOException {
        DataInputStream dis = new DataInputStream(in);
        int length = dis.readInt();
        if (length < 0 || length > MAX_MSG_LEN) {
            throw new IOException("수신된 메시지 길이가 허용 범위를 초과합니다: " + length);
        }
        byte[] bytes = new byte[length];
        dis.readFully(bytes);
        return objectMapper.readValue(bytes, JsonCommandRequest.class);
    }

    @Override
    public void encode(OutputStream out, JsonCommandResponse response) throws IOException {
        byte[] bytes = objectMapper.writeValueAsBytes(response);
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeInt(bytes.length);
        dos.write(bytes);
        dos.flush();
    }
}
