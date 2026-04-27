package com.example.spiderbatch.job.db2foreign;

/**
 * 외부 전문 연계 실패 예외.
 *
 * <p>TransferItemWriter에서 외부 API 호출 실패(비-2xx 응답, 네트워크 오류) 시 발생한다.
 * Db2ForeignJob Step의 skip 대상으로만 사용하여, 그 외 RuntimeException이 의도치 않게
 * skip되는 것을 방지한다.</p>
 */
public class ExternalTransferException extends RuntimeException {

    public ExternalTransferException(String message) {
        super(message);
    }

    public ExternalTransferException(String message, Throwable cause) {
        super(message, cause);
    }
}
