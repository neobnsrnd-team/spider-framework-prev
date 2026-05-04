# batch-was

`spider-batch` 라이브러리를 내장한 배치 실행 WAS(Web Application Server).
Quartz 스케줄러와 Redis 분산 락을 통해 멀티 인스턴스 환경에서도 중복 실행 없이 배치를 자동 실행한다.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.4 |
| Batch | spider-batch (Spring Batch 5.x 내장) |
| Scheduler | Quartz 3.x (RAMJobStore) |
| Distributed Lock | Redisson 3.27 (Redis) |
| Database | Oracle (ojdbc11, ucp) |
| Build | Maven 3 (Wrapper), Fat JAR |

## Quick Start

### 1. 사전 조건

```bash
# 1. spider-link 설치
mvn install -f spider-link/pom.xml

# 2. spider-batch 설치
mvn install -f spider-batch/pom.xml
```

### 2. 환경변수 설정

```bash
cp batch-was/.env.example batch-was/.env
# .env에 DB 접속 정보, Redis 연결 정보, 배치 API 키 등 입력
```

주요 환경변수:

| 변수 | 설명 |
|------|------|
| `DB_URL` | Oracle JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | DB 계정 |
| `BATCH_TCP_PORT` | Admin → batch-was TCP 포트 (기본 9998) |
| `BATCH_FILE_INPUT_DIR` | 배치 파일 입력 디렉터리 |
| `REDIS_HOST` / `REDIS_PORT` | Redis 접속 정보 (기본 localhost:6379) |
| `BATCH_API_KEY` | REST API 인증 키 |

### 3. 빌드 & 실행

```bash
cd batch-was
./mvnw clean package
java -jar target/batch-was-0.0.1-SNAPSHOT.jar

# 또는 개발 모드
./mvnw spring-boot:run
```

서버 기동 후 `http://localhost:8081/actuator/health` 접속.

## 구현된 배치 Job

`PocJobProvider`가 등록하는 4개 Job:

| Job 이름 | 구현 클래스 | 처리 방식 | 대상 |
|----------|------------|----------|------|
| `db2db` | `Db2DbJobConfig` | `ColumnRangePartitioner` 기반 날짜 범위 분할 병렬 처리 | `POC_카드사용내역` → `POC_카드사용내역_백업` (MERGE INTO) |
| `file2db` | `File2DbJobConfig` | CSV `FlatFileItemReader` → MERGE UPSERT | `poc-users.csv` → `POC_USER` |
| `fixedLengthFile2db` | `FixedLengthFile2DbJobConfig` | 61자 고정 길이 전문 파일, HDR/TLR 처리, 성공/실패 아카이브 분기 | 고정 길이 파일 → `POC_고정길이거래` |
| `db2foreign` | `Db2ForeignJobConfig` | DB 읽기 → 외부 시스템 HTTP POST | `POC_카드사용내역` → `POST /mock/external/transfer` |

모든 Job은 `FWK_BATCH_APP.BATCH_APP_FILE_NAME`에 동일한 이름으로 등록되어 있어야 한다.

## Quartz 스케줄러 동작 흐름

```
WAS 기동
  └── QuartzAutoRegistrar (ApplicationRunner)
        └── FWK_WAS_EXEC_BATCH JOIN FWK_BATCH_APP 조회
              └── 이 인스턴스(BT01)에 배정된 배치 목록 → Quartz에 CronTrigger 등록

Cron 주기 도래
  └── BatchJobQuartzTrigger (QuartzJobBean)
        ├── Redis 분산 락 획득 시도 (batch:lock:{batchAppId})
        │     ├── 획득 성공 → BatchExecuteService.execute()
        │     └── 획득 실패 (다른 인스턴스 실행 중) → 스킵
        └── finally: 락 반드시 해제
```

**Quartz job-store-type: memory** (RAMJobStore) — 재기동 시 스케줄이 초기화되므로 `QuartzAutoRegistrar`가 매 기동 시 DB를 조회하여 재등록한다.

## Redis 분산 락

`RedisDistributedLockService`가 Redisson `RLock`을 사용하여 멀티 인스턴스 중복 실행을 방지한다.

- 락 키: `batch:lock:{batchAppId}`
- `tryLock(waitTime=0)`: 즉시 시도, 이미 락 있으면 false 반환 (대기 없음)
- leaseTime 미지정 → Redisson Watchdog 활성화 (30초마다 자동 TTL 갱신)
- 비정상 종료 시 Watchdog 중단 → 기본 TTL(30초) 후 자동 해제

## API 엔드포인트

spider-batch 라이브러리가 제공하는 엔드포인트:

```
POST /api/batch/execute       # 배치 즉시 실행
GET  /api/batch/running       # 실행 중 배치 조회
POST /api/batch/stop/{id}     # 배치 강제 종료
GET  /actuator/health         # 헬스체크
GET  /actuator/prometheus     # Prometheus 메트릭
```

모든 `/api/batch/**` 요청은 `X-API-KEY` 헤더 인증이 필요하다.

## Project Structure

```
src/main/java/com/example/spiderbatch/
├── BatchWasApplication.java
├── job/
│   ├── PocJobProvider.java                     # JobProvider SPI 구현체
│   ├── common/                                 # 공통 도메인 모델 (CardUsage, PocUser 등)
│   ├── db2db/
│   │   ├── ColumnRangePartitioner.java         # 날짜 범위 기반 파티셔너
│   │   └── Db2DbJobConfig.java
│   ├── file2db/
│   │   ├── File2DbJobConfig.java
│   │   ├── FileArchiveTasklet.java             # 처리 완료 파일 이동 Tasklet
│   │   └── FixedLengthFile2DbJobConfig.java
│   ├── db2foreign/
│   │   ├── Db2ForeignJobConfig.java
│   │   ├── TransferItemWriter.java             # 외부 HTTP POST 처리
│   │   ├── MockExternalController.java         # 외부 시스템 Mock (테스트용)
│   │   └── ExternalTransferException.java
│   └── listener/BatchNotificationListener.java # Job 시작/완료 Slack/Email 알림
├── lock/
│   └── RedisDistributedLockService.java        # Redisson 분산 락
├── scheduler/
│   ├── BatchJobQuartzTrigger.java              # Quartz Job 구현체
│   ├── QuartzAutoRegistrar.java                # 기동 시 Cron 자동 등록
│   └── SchedulerManagementService.java         # 스케줄러 관리
└── tcp/
    └── ScheduleCommandHandler.java             # Admin TCP 커맨드 처리 (스케줄 변경)
```

## 주요 설정 (`application.yml`)

| 설정 키 | 기본값 | 설명 |
|--------|--------|------|
| `server.port` | `8081` | HTTP 포트 |
| `batch.was.instance-id` | `BT01` | WAS 인스턴스 ID (`FWK_WAS_EXEC_BATCH`와 일치) |
| `batch.tcp.port` | `${BATCH_TCP_PORT:9998}` | Admin TCP 포트 |
| `batch.tcp.handler-pool-size` | `20` | TCP 핸들러 스레드 풀 |
| `batch.file.input-dir` | `./batch-files/input` | 배치 파일 입력 경로 |
| `spring.batch.job.enabled` | `false` | 기동 시 자동 실행 방지 |
| `spring.quartz.job-store-type` | `memory` | RAMJobStore |

## DB 테이블

| 테이블 | 역할 |
|--------|------|
| `FWK_BATCH_APP` | 배치 앱 등록 (Job 이름, CRON_TEXT) |
| `FWK_BATCH_HIS` | 배치 실행 이력 (seq, 상태, 처리건수) |
| `FWK_WAS_EXEC_BATCH` | WAS 인스턴스-배치 배정 관계 |
| `BATCH_5_*` | Spring Batch 5.x 메타테이블 |
| `POC_카드사용내역` | db2db / db2foreign Job 소스 테이블 |
| `POC_카드사용내역_백업` | db2db Job 타깃 테이블 |
| `POC_USER` | file2db Job 타깃 테이블 |
| `POC_고정길이거래` | fixedLengthFile2db Job 타깃 테이블 |
