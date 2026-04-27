package com.example.mockcore.infra;

import java.nio.charset.Charset;

/**
 * 고정길이 바이너리 전문 바이트 배열에서 필드값을 순차적으로 읽는 유틸리티.
 *
 * <p>pos가 커서 역할을 하며, 각 read/skip 호출마다 해당 길이만큼 전진한다.</p>
 */
public class FixedMessageReader {

    private final byte[] data;
    private int pos;

    public FixedMessageReader(byte[] data) {
        this.data = data;
        this.pos  = 0;
    }

    /** C 타입: 지정 길이를 읽어 앞뒤 공백 제거 후 반환 */
    public String readC(int len) {
        String val = new String(data, pos, safe(len));
        pos += len;
        return val.trim();
    }

    /** N 타입: 지정 길이를 읽어 trim 후 반환 (앞 0 포함 원본 문자열) */
    public String readN(int len) {
        return readC(len);
    }

    /** K 타입: 지정 길이를 EUC-KR로 디코딩 후 앞뒤 공백 제거 반환 */
    public String readK(int len) {
        int actual = safe(len);
        String val = new String(data, pos, actual, Charset.forName("EUC-KR"));
        pos += len;
        return val.trim();
    }

    /** 커서를 len 바이트 전진 (읽지 않고 건너뜀) */
    public void skip(int len) {
        pos += len;
    }

    private int safe(int len) {
        return Math.min(len, data.length - pos);
    }
}
