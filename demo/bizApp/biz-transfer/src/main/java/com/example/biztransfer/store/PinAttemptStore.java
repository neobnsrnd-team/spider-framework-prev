package com.example.biztransfer.store;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 카드별 PIN 실패 횟수를 인메모리로 관리하는 공유 저장소.
 *
 * <p>키 형식: "userId:cardId" — 사용자·카드 복합 키로 독립 관리한다.</p>
 *
 * <p>{@link com.example.biztransfer.handler.TransferImmediatePayHandler}와
 * {@link com.example.biztransfer.handler.TransferResetPinAttemptsHandler}가 동일 인스턴스를 주입받아
 * 실패 횟수를 공유한다. 서버 재시작 시 초기화된다.</p>
 */
@Component
public class PinAttemptStore {

    private final Map<String, Integer> store = new ConcurrentHashMap<>();

    /** 현재 실패 횟수를 반환한다. 기록이 없으면 0. */
    public int get(String key) {
        return store.getOrDefault(key, 0);
    }

    /** 실패 횟수를 1 증가시키고 변경된 값을 반환한다. */
    public int increment(String key) {
        return store.merge(key, 1, Integer::sum);
    }

    /** 해당 키의 실패 횟수를 초기화(삭제)한다. */
    public void reset(String key) {
        store.remove(key);
    }
}
