# spider-batch

Spring Batch 기반 배치 오케스트레이션·이력관리·TCP 연동 **Maven 라이브러리**.
<br>내장 프로젝트(`pom.xml`에 의존성 추가)가 `JobProvider` SPI만 구현하면 배치 실행·이력·모니터링 기능이 자동 활성화된다.

## 목차

- [기술 스택](#기술-스택)
- [내장 방법](#내장-방법)
- [SPI 확장점](#spi-확장점)
- [AutoConfiguration 동작 방식](#autoconfiguration-동작-방식)
- [API 엔드포인트](#api-엔드포인트)
- [주요 설정](#주요-설정-applicationyml)
- [Project Structure](#project-structure)
- [빌드 & 설치](#빌드--설치)
- [DB 테이블](#db-테이블)

---

## 기술 스택

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.4 |
| Batch | Spring Batch 5.x |
| ORM | MyBatis 3 |
| Database | Oracle (optional — 내장 프로젝트가 선택) |
| TCP 서버 | spider-link (`SpiderTcpServer`) |
| TCP 모델 | spider-common (`ManagementContext`, `CommandHandler`, `CommandDispatcher`) |
| Build | Maven 3 (Wrapper), 라이브러리 JAR |

## 내장 방법

```xml
<!-- 내장 프로젝트 pom.xml -->
<!-- spider-common·spider-link는 spider-batch의 전이 의존성으로 자동 포함된다 -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>spider-batch</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>

<!-- Oracle JDBC — spider-batch에서 optional 처리한 드라이버를 내장 프로젝트가 명시 포함 -->
<dependency>
    <groupId>com.oracle.database.jdbc</groupId>
    <artifactId>ojdbc11</artifactId>
    <scope>runtime</scope>
</dependency>
```

`spring-boot-starter-batch`가 클래스패스에 있을 때 `SpiderBatchAutoConfiguration`이 자동 활성화된다.

## SPI 확장점

### JobProvider

내장 프로젝트에서 배치 Job을 등록하는 유일한 확장점.

```java
@Configuration
@RequiredArgsConstructor
public class MyJobProvider implements JobProvider {

    @Bean(name = "db2db")
    public Job db2DbJob(JobRepository jobRepository, Step partitionStep) {
        return new JobBuilder("db2db", jobRepository)
                .start(partitionStep)
                .build();
    }

    @Override
    public List<String> getJobNames() {
        // FWK_BATCH_APP.BATCH_APP_FILE_NAME 값과 일치해야 함
        return List.of("db2db");
    }
}
```

Spring Batch 5.x `JobRegistrySmartInitializingSingleton`이 컨텍스트 내 모든 Job Bean을 `JobRegistry`에 자동 등록하므로,
`getJobNames()`에 선언한 이름으로 `BatchExecuteService`에서 조회 가능하다.

### BatchHistoryRecorder

`FWK_BATCH_HIS` 이력 저장 방식을 교체하는 확장점.

```java
@Bean
public BatchHistoryRecorder customRecorder() {
    return new MyCustomBatchHistoryRecorder();   // 기본 Oracle MyBatis 구현 대체
}
```

기본 구현체(`DefaultBatchHistoryRecorder`)는 Oracle MyBatis 매퍼를 사용한다.
내장 프로젝트에서 `BatchHistoryRecorder` Bean을 등록하지 않으면 기본 구현체가 자동 등록된다(`@ConditionalOnMissingBean`).

### AbstractDb2DbJob / AbstractDb2ForeignJob

파티셔닝 기반 병렬 처리 Job의 공통 설정을 템플릿 메서드로 제공하는 추상 클래스.
내장 프로젝트의 `@Configuration`에서 `build*` 메서드를 호출하는 방식으로 사용한다.

```java
public class Db2DbJobConfig extends AbstractDb2DbJob<CardUsage> {
    @Override
    protected String getJobName() { return "db2db"; }

    @Bean("db2db")
    public Job db2DbJob(JobRepository jobRepository, Step db2DbPartitionStep) {
        return buildJob(jobRepository, db2DbPartitionStep);
    }
    // buildPartitionStep(), buildWorkerStep(), buildTaskExecutor() 활용
}
```

재정의 가능한 기본값:

| 메서드 | 기본값 | 설명 |
|--------|--------|------|
| `getPageSize()` | `5` | 페이지당 읽을 건수 |
| `getGridSize()` | `4` | 병렬 파티션(스레드) 수 |
| `getSkipLimit()` | `10` (`AbstractDb2DbJob`) / `5` (`AbstractDb2ForeignJob`) | 최대 스킵 건수 |
| `getPartitionStepName()` | `getJobName() + "PartitionStep"` | 파티션 Step 이름 |

## AutoConfiguration 동작 방식

`SpiderBatchAutoConfiguration`이 `@Import`로 다음 Bean을 등록한다.
(`@ComponentScan` 미사용 — 내장 프로젝트 패키지 충돌 방지)

| Bean | 활성화 조건 |
|------|------------|
| `BatchConfig` | 항상 |
| `TcpServerConfig` | `batch.tcp.enabled=true` (기본값: true) |
| `BatchWasSecurityConfig`, `ApiKeyAuthFilter` | `batch.security.enabled=true` (기본값: true) |
| `BatchExecuteService`, `BatchMonitorService` | 항상 |
| `BatchExecuteController`, `BatchMonitorController` | 항상 |
| `SlackNotificationService`, `EmailNotificationService` | 환경변수 미설정 시 no-op |
| `DefaultBatchHistoryRecorder` | `BatchHistoryRecorder` Bean 없을 때 |

## API 엔드포인트

### 배치 실행

```
POST /api/batch/execute
Authorization: X-API-KEY: {batch.api-key}

{
  "batchAppId": "POC_DB2DB_JOB",
  "batchDate":  "20250101",
  "userId":     "admin",
  "parameters": {}
}
```

### 실행 중 배치 조회

```
GET /api/batch/running
Authorization: X-API-KEY: {batch.api-key}
```

### 배치 강제 종료

```
POST /api/batch/stop/{jobExecutionId}
Authorization: X-API-KEY: {batch.api-key}
```

## 주요 설정 (`application.yml`)

```yaml
batch:
  was:
    instance-id: BT01               # WAS 인스턴스 식별자 (FWK_WAS_EXEC_BATCH와 일치)
  tcp:
    port: 9998                      # Admin → batch-was TCP 포트
    handler-pool-size: 20
    queue-capacity: 100
    enabled: true
  security:
    enabled: true
  file:
    input-dir: ./batch-files/input
    archive-dir: ./batch-files/archive
    error-dir: ./batch-files/error
```

## Project Structure

```
src/main/java/com/example/spiderbatch/
├── config/
│   ├── SpiderBatchAutoConfiguration.java  # AutoConfiguration 진입점
│   ├── BatchConfig.java                   # Spring Batch DataSource, JobRepository 설정
│   ├── BatchConfigurationProperties.java  # @ConfigurationProperties(prefix="batch")
│   └── TcpServerConfig.java               # Admin TCP 서버 (ManagementContext 프로토콜)
├── domain/batch/
│   ├── controller/
│   │   ├── BatchExecuteController.java    # POST /api/batch/execute
│   │   └── BatchMonitorController.java    # GET /api/batch/running, POST /api/batch/stop/{id}
│   ├── service/
│   │   ├── BatchExecuteService.java       # Job 실행 + 이력 기록 오케스트레이션
│   │   ├── BatchMonitorService.java       # 실행 중 Job 조회·강제 종료
│   │   └── DefaultBatchHistoryRecorder.java
│   └── mapper/                            # FWK_BATCH_APP, FWK_BATCH_HIS MyBatis 매퍼
├── global/
│   ├── log/BatchAuditLogger.java          # 배치 실행 감사 로그
│   ├── notification/                      # Slack / Email 알림 서비스
│   └── security/                          # API 키 인증 필터
├── job/
│   ├── AbstractDb2DbJob.java              # DB→DB 파티셔닝 Job 추상 클래스
│   ├── AbstractDb2ForeignJob.java         # DB→외부 시스템 Job 추상 클래스
│   └── common/BatchJobParametersValidator.java
├── spi/
│   ├── JobProvider.java                   # SPI 인터페이스 #1
│   └── BatchHistoryRecorder.java          # SPI 인터페이스 #2
└── tcp/BatchExecCommandHandler.java       # Admin TCP 커맨드 핸들러
```

## 빌드 & 설치

다른 모듈에서 참조하기 전에 반드시 먼저 설치해야 한다. (spider-common과 spider-link가 선행 설치되어 있어야 함)

```bash
# 사전 조건: spider-common → spider-link 순서로 로컬 저장소에 설치되어 있어야 함
cd spider-batch
./mvnw clean install
```

## DB 테이블

| 테이블 | 역할 |
|--------|------|
| `FWK_BATCH_APP` | 배치 앱 등록 정보 (BATCH_APP_FILE_NAME = Job 이름) |
| `FWK_BATCH_HIS` | 배치 실행 이력 (시작·종료·처리건수) |
| `FWK_WAS_EXEC_BATCH` | WAS 인스턴스별 배치 배정 정보 + CRON_TEXT |
| `BATCH_5_*` | Spring Batch 5.x 메타테이블 (initialize-schema: never) |
