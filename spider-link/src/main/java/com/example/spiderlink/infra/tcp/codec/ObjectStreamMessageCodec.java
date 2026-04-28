package com.example.spiderlink.infra.tcp.codec;

import com.example.spidercommon.infra.tcp.model.ManagementContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Java ObjectStream 프로토콜 코덱.
 *
 * <p>Admin ↔ spider-batch 구간에서 사용한다.
 * 역직렬화 공격 방어를 위해 ManagementContext 외 클래스는 화이트리스트 필터로 차단한다.</p>
 *
 * <p>REQ/RES 모두 {@link ManagementContext}를 사용한다.
 * Admin의 {@code TcpClient.sendObject()}가 ManagementContext로 캐스팅하므로
 * 응답 타입도 ManagementContext여야 역직렬화가 성공한다.</p>
 */
public class ObjectStreamMessageCodec implements MessageCodec<ManagementContext, ManagementContext> {

    /** 역직렬화 허용 클래스 화이트리스트 — ManagementContext, String, java.util.** 외 차단 */
    private static final String DESERIALIZATION_FILTER =
            "com.example.spidercommon.infra.tcp.model.ManagementContext;java.lang.String;java.util.**;!*";

    /**
     * ObjectInputStream으로 ManagementContext를 역직렬화한다.
     * 보안 필터를 적용하여 허용된 클래스만 역직렬화한다.
     */
    @Override
    public ManagementContext decode(InputStream in) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(in);
        ois.setObjectInputFilter(ObjectInputFilter.Config.createFilter(DESERIALIZATION_FILTER));
        try {
            return (ManagementContext) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("ManagementContext 역직렬화 실패: " + e.getMessage(), e);
        }
    }

    /** ObjectOutputStream으로 ManagementContext를 직렬화하여 전송한다. */
    @Override
    public void encode(OutputStream out, ManagementContext response) throws IOException {
        // OOS 생성 시 스트림 헤더가 즉시 전송된다.
        // Admin 측 OIS가 이 헤더를 읽어야 역직렬화를 시작할 수 있으므로 flush를 보장한다.
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(response);
        oos.flush();
    }
}
