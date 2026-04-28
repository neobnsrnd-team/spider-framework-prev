package com.example.spiderlink.domain.messageinstance;

import com.example.spiderlink.domain.messageinstance.dto.MessageInstanceInsertRequest;
import com.example.spiderlink.infra.tcp.model.CommandRequest;
import com.example.spiderlink.infra.tcp.model.HasCommand;
import com.example.spiderlink.infra.tcp.model.JsonCommandRequest;
import com.example.spiderlink.infra.tcp.model.JsonCommandResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * spider-link 전문 거래 이력 기록기.
 *
 * <p>SpiderTcpServer(서버 수신·응답)와 TcpClient(클라이언트 송신·수신) 양방향에서
 * {@link MessageInstanceInsertRequest}를 생성하여 {@link MessageLogQueue}에 적재한다.
 * 실제 DB INSERT는 큐 컨슈머 스레드가 비동기로 처리하므로 TCP 처리 스레드가 블로킹되지 않는다.</p>
 *
 * <p>DB 기록 실패 시 경고 로그만 출력하고 비즈니스 로직에 영향을 주지 않는다.</p>
 *
 * <p>{@link com.example.spiderlink.config.SpiderLinkAutoConfiguration}에 의해
 * JdbcTemplate 빈이 존재하는 경우에만 자동으로 빈으로 등록된다.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class MessageInstanceRecorder {

    private static final DateTimeFormatter DTIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MessageLogQueue queue;
    private final ObjectMapper objectMapper;
    /** spring.application.name — ORG_ID 및 INSTANCE_ID 구성에 사용 */
    private final String appName;

    /**
     * SpiderTcpServer 인바운드 요청 기록 (IO_TYPE=I, REQ_RES_TYPE=REQ).
     *
     * @param trxId   거래 ID (UUID)
     * @param request 수신된 요청 객체
     * @param port    서버 포트
     */
    public void recordServerRequest(String trxId, Object request, int port) {
        String command = command(request);
        String trackingNo = trackingNo(request, trxId);
        enqueue(trxId, "I", "REQ", command, trackingNo, userId(request), toJson(request), true, appName, port);
    }

    /**
     * SpiderTcpServer 아웃바운드 응답 기록 (IO_TYPE=O, REQ_RES_TYPE=RES).
     *
     * @param trxId    거래 ID (UUID)
     * @param request  원본 요청 객체 (커맨드·추적번호 참조)
     * @param response 전송할 응답 객체
     * @param port     서버 포트
     */
    public void recordServerResponse(String trxId, Object request, Object response, int port) {
        String command = command(request);
        String trackingNo = trackingNo(request, trxId);
        boolean success = response instanceof JsonCommandResponse r ? r.isSuccess() : true;
        enqueue(trxId, "O", "RES", command, trackingNo, userId(request), toJson(response), success, appName, port);
    }

    /**
     * HTTP 인바운드 요청 기록 (IO_TYPE=I, REQ_RES_TYPE=REQ).
     *
     * <p>Front → biz-channel HTTP 수신 시점에 호출한다.
     * {@code requestId}는 이 시점에 생성된 UUID를 사용하여 후속 TCP 구간과 동일한
     * {@code TRX_TRACKING_NO}로 연결된다.</p>
     *
     * @param requestId 거래 추적 UUID (HTTP 수신 시 생성)
     * @param uri       요청 URI (예: /api/auth/login)
     * @param data      요청 바디 JSON 문자열
     * @param port      서버 포트
     * @param userId    요청 사용자 ID (JWT 미인증 구간은 "GUEST")
     */
    public void recordHttpRequest(String requestId, String uri, String data, int port, String userId) {
        enqueueHttp(requestId, "I", "REQ", uri, data, true, port, userId);
    }

    /**
     * HTTP 아웃바운드 응답 기록 (IO_TYPE=O, REQ_RES_TYPE=RES).
     *
     * <p>biz-channel → Front HTTP 응답 시점에 호출한다.</p>
     *
     * @param requestId  거래 추적 UUID (요청 시 생성한 값과 동일)
     * @param uri        요청 URI
     * @param data       응답 바디 JSON 문자열
     * @param success    HTTP 응답 성공 여부 (2xx → true)
     * @param port       서버 포트
     * @param userId     요청 사용자 ID
     */
    public void recordHttpResponse(String requestId, String uri, String data, boolean success, int port, String userId) {
        enqueueHttp(requestId, "O", "RES", uri, data, success, port, userId);
    }

    /**
     * TcpClient 아웃바운드 요청 기록 (IO_TYPE=O, REQ_RES_TYPE=REQ).
     *
     * @param trxId   거래 ID (UUID)
     * @param request 전송할 요청 객체
     * @param host    대상 호스트
     * @param port    대상 포트
     */
    public void recordClientRequest(String trxId, JsonCommandRequest request, String host, int port) {
        enqueue(trxId, "O", "REQ", request.getCommand(), request.getRequestId(),
                userId(request), toJson(request), true, host, port);
    }

    /**
     * TcpClient 인바운드 응답 기록 (IO_TYPE=I, REQ_RES_TYPE=RES).
     *
     * @param trxId    거래 ID (UUID)
     * @param request  원본 요청 객체
     * @param response 수신된 응답 객체
     * @param host     대상 호스트
     * @param port     대상 포트
     */
    public void recordClientResponse(String trxId, JsonCommandRequest request,
                                     JsonCommandResponse response, String host, int port) {
        enqueue(trxId, "I", "RES", request.getCommand(), request.getRequestId(),
                userId(request), toJson(response), response.isSuccess(), host, port);
    }

    /** TCP 전문 이력 DTO 생성 후 큐에 적재 */
    private void enqueue(String trxId, String ioType, String reqResType,
                         String command, String trackingNo, String userId, String data,
                         boolean success, String host, int port) {
        String dtime = LocalDateTime.now().format(DTIME_FMT);
        queue.put(MessageInstanceInsertRequest.builder()
                .trxId(trxId)
                .orgId(appName)
                .ioType(ioType)
                .reqResType(reqResType)
                .messageId(command)
                .trxTrackingNo(trackingNo)
                .userId(userId)
                .logDtime(dtime)
                .lastLogDtime(dtime)
                .lastRtCode(success ? "SUCCESS" : "FAIL")
                .instanceId(host + ":" + port)
                .retryTrxYn("N")
                .messageData(data)
                .trxDtime(dtime)
                .channelType("TCP")
                .uri(command)
                .successYn(success ? "Y" : "N")
                .build());
    }

    /** HTTP 전문 이력 DTO 생성 후 큐에 적재 */
    private void enqueueHttp(String requestId, String ioType, String reqResType,
                             String uri, String data, boolean success, int port, String userId) {
        String dtime = LocalDateTime.now().format(DTIME_FMT);
        queue.put(MessageInstanceInsertRequest.builder()
                .trxId(requestId)
                .orgId(appName)
                .ioType(ioType)
                .reqResType(reqResType)
                .messageId(uri)
                .trxTrackingNo(requestId)
                .userId(userId)
                .logDtime(dtime)
                .lastLogDtime(dtime)
                .lastRtCode(success ? "SUCCESS" : "FAIL")
                .instanceId(appName + ":" + port)
                .retryTrxYn("N")
                .messageData(data)
                .trxDtime(dtime)
                .channelType("HTTP")
                .uri(uri)
                .successYn(success ? "Y" : "N")
                .build());
    }

    private String command(Object request) {
        return request instanceof HasCommand h ? h.getCommand() : "UNKNOWN";
    }

    private String trackingNo(Object request, String fallback) {
        return request instanceof CommandRequest<?> cr ? cr.getRequestId() : fallback;
    }

    /** 요청 payload에서 userId를 추출한다. 없으면 "SYSTEM"을 반환한다. */
    private String userId(Object request) {
        if (request instanceof JsonCommandRequest jr && jr.getPayload() != null) {
            Object val = jr.getPayload().get("userId");
            if (val instanceof String s && !s.isBlank()) {
                return s;
            }
        }
        return "SYSTEM";
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj != null ? obj.toString() : "null";
        }
    }
}
