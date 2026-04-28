package com.example.mockcore.infra;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * 고정길이 바이너리 전문 응답 바이트 배열을 순차적으로 빌드하는 유틸리티.
 */
public class FixedMessageWriter {

    private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

    /** C 타입: 우측 공백 패딩 */
    public void writeC(String val, int len) {
        byte[] src = val != null ? val.getBytes() : new byte[0];
        buf.writeBytes(padRight(src, len, (byte) ' '));
    }

    /** N 타입: 좌측 0 패딩 */
    public void writeN(long val, int len) {
        writeN(String.valueOf(val), len);
    }

    /** N 타입: 좌측 0 패딩 */
    public void writeN(String val, int len) {
        String digits = val != null ? val.replaceAll("[^0-9]", "") : "";
        if (digits.isEmpty()) digits = "0";
        byte[] src = digits.getBytes();
        buf.writeBytes(padLeft(src, len, (byte) '0'));
    }

    /** K 타입: EUC-KR 인코딩 후 우측 공백 패딩 */
    public void writeK(String val, int len) {
        byte[] src;
        try {
            src = val != null ? val.getBytes(Charset.forName("EUC-KR")) : new byte[0];
        } catch (Exception e) {
            src = val != null ? val.getBytes() : new byte[0];
        }
        buf.writeBytes(padRight(src, len, (byte) ' '));
    }

    /** 빌드된 byte[] 반환 */
    public byte[] toBytes() {
        return buf.toByteArray();
    }

    private byte[] padRight(byte[] src, int len, byte pad) {
        byte[] result = new byte[len];
        Arrays.fill(result, pad);
        System.arraycopy(src, 0, result, 0, Math.min(src.length, len));
        return result;
    }

    private byte[] padLeft(byte[] src, int len, byte pad) {
        byte[] result = new byte[len];
        Arrays.fill(result, pad);
        int copyLen = Math.min(src.length, len);
        System.arraycopy(src, src.length - copyLen, result, len - copyLen, copyLen);
        return result;
    }
}
