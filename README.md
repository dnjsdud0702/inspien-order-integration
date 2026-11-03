# 토토 쇼핑몰 주문 연계

주문 XML을 API로 받아 `ORDER_TB`에 저장하고, 주기적으로 영수증을 FTP에 올린 뒤 `SHIPMENT_TB`에 운송 정보를 입력한다. 각 주문이 어느 단계까지 처리됐는지, 어디서 왜 실패했는지는 처리 이력 파일로 확인한다.

```text
[주문 수신]  주문 XML → POST /orders → XML 파싱 → ORDER_TB 저장 (STATUS=N)

[후속 처리]  60초마다 STATUS=N 주문 조회 (OrderJob)
                │
                ├─ 영수증 생성 → 로컬 receipts/ 저장 → FTP 업로드
                │
                └─ SHIPMENT_TB 입력 + STATUS=Y   (한 트랜잭션)
```

| 요구사항 | 처리 |
|---|---|
| 주문 XML을 받아 DB에 저장 | `POST /orders` → `ORDER_TB` |
| 주문마다 영수증을 FTP에 업로드 | `OrderJob` → FTP (`주원영_{ORDER_ID}.txt`) |
| 배송 정보를 주기적으로 운송 DB에 입력 | `OrderJob` → `SHIPMENT_TB` |
| 어디까지 처리됐고 어디서 잘못됐는지 확인 | `logs/trace.log` + `STATUS` |

## 구성

| 패키지 | 클래스 | 역할 |
|---|---|---|
| `config` | `Config` | 채용 API 응답의 접속 정보를 휴대폰 번호로 복호화 (AES-128/ECB) |
| | `AppConfig` | `Config`, `DataSource` Bean 등록 |
| | `ConnectionCheck` | 시작할 때 DB·FTP 접속 확인 (조회만 함) |
| `order` | `OrderController` | `POST /orders` 요청을 EUC-KR로 읽어 서비스 호출 |
| | `OrderService` | XML 파싱 → 유효한 주문 저장 → 처리 이력 기록 |
| | `OrderParser` | 주문 XML을 `ORDER_TB` 행 단위로 변환 |
| | `OrderDao` | 주문 저장, 대기 주문 조회, 상태를 `Y`로 변경 |
| | `OrderJob` | 주기적으로 영수증 업로드와 운송 입력 |
| `shipment` | `ShipmentService` | 운송 입력과 `Y` 변경을 한 트랜잭션으로 처리 |
| | `ShipmentDao` | `SHIPMENT_TB` 입력 |
| `ftp` | `FtpUploader` | 영수증 업로드, 이미 있는 파일은 건너뜀 |
| | `ReceiptBackup` | 업로드하는 영수증을 로컬 `receipts/`에 저장 |
| `common` | `IdGenerator` | `ORDER_ID`, `SHIPMENT_ID` 다음 값 계산 |
| | `TraceLog` | 처리 이력을 `logs/trace.log`에 기록 |

## 실행

프로젝트 폴더의 `secret/`에 채용 API 응답(`response.json`)과 휴대폰 번호(`phone.txt`)를 둔다. 이 폴더는 저장소에 올리지 않는다. 다른 위치를 쓰려면 환경변수 `SECRET_DIR`로 지정한다.

```bash
./gradlew bootRun
```

시작하면 접속 확인 결과가 출력된다.

```text
[DB] 접속 성공
[DB] ORDER_TB 내 데이터 66건
[DB] SHIPMENT_TB 내 데이터 66건
[FTP] 접속 성공, 내 파일 66개
```

다른 터미널에서 주문을 보낸다. 본문은 EUC-KR로 읽으며, `Content-Type`은 `application/xml` 또는 `text/xml`이어야 한다.

```bash
curl -X POST -H "Content-Type: application/xml" --data-binary @samples/orders.xml http://127.0.0.1:8080/orders
```

```json
{"saved":["A004","A005","..."],"errors":["USER16/ITEM64: HEADER 없음","..."]}
```

| 응답 | 의미 |
|---|---|
| 200 | 1건 이상 저장. 제외된 데이터는 `errors`에 사유 포함 |
| 400 | XML 형식 오류, 또는 저장할 주문이 하나도 없음 |
| 415 | `Content-Type`이 XML이 아님 |

후속 처리 간격은 `application.yaml`의 `job.interval-ms`(기본 60초)이며, 테스트할 때는 실행 옵션으로 줄일 수 있다.

```bash
./gradlew bootRun --args='--job.interval-ms=5000'
```

| 파일 | 내용 |
|---|---|
| `samples/orders.xml` | 채용 API의 `SAMPLE_DATA`를 Base64 디코딩한 원본 (EUC-KR, 63건 저장·7건 제외) |
| `samples/one-order.xml` | 실제 환경 확인용 주문 1건 |

## 처리 이력

### 저장 방식

DB 계정에 테이블 생성 권한이 없어, 처리 이력은 `logs/trace.log`에 한 줄씩 추가한다. 한 줄의 형식은 다음과 같다.

```text
시각 | 대상 | 단계 | 결과 | 상세
```

- **대상:** 주문 ID. 주문이 만들어지기 전의 실패(XML 오류, 검증 실패)는 `-`
- **결과:** `OK` 또는 `FAIL`
- **상세:** 성공이면 처리 결과(파일명, 배송 ID 등), 실패면 오류 메시지

이력 기록에 실패해도 주문 처리는 멈추지 않는다.

### 단계

| 단계 | 기록 위치 | OK | FAIL |
|---|---|---|---|
| `RECEIVE` | `OrderService` | - | XML 형식 오류 |
| `VALIDATE` | `OrderService` | - | HEADER 없음, 필수값 누락 (주문별로 기록) |
| `ORDER_SAVE` | `OrderService` | `ORDER_TB` 저장 완료 (`USER_ID/ITEM_ID`) | DB 저장 실패 |
| `RECEIPT` | `OrderJob` | 로컬 저장과 FTP 업로드 완료 (파일명) | 로컬 저장 또는 FTP 업로드 실패 |
| `SHIPMENT` | `OrderJob` | `SHIPMENT_TB` 입력과 `Y` 변경 완료 (배송 ID) | 운송 입력 실패 (롤백되어 `N` 유지) |
| `JOB` | `OrderJob` | - | 대기 주문 조회나 FTP 접속 실패 |

`SHIPMENT` 결과는 트랜잭션이 커밋된 뒤 `OrderJob`에서 기록한다. 트랜잭션 안에서 성공을 기록하면 커밋이 실패해 롤백될 때 이력과 DB 상태가 어긋나기 때문이다.

### 예시

정상 처리:

```text
2026-11-03T19:54:01 | A004 | ORDER_SAVE | OK | USER09/ITEM45
2026-11-03T19:54:55 | A004 | RECEIPT    | OK | 주원영_A004.txt
2026-11-03T19:54:55 | A004 | SHIPMENT   | OK | A004
```

검증에서 제외된 데이터:

```text
2026-11-03T19:54:01 | - | VALIDATE | FAIL | USER16/ITEM64: HEADER 없음
```

실패 후 자동 재처리:

```text
A067 | ORDER_SAVE | OK   | USER01/ITEM01
A067 | RECEIPT    | FAIL | /receipts
A067 | RECEIPT    | OK   | 주원영_A067.txt
A067 | SHIPMENT   | OK   | A067
```

### 확인 방법

```bash
grep A004 logs/trace.log      # 특정 주문이 어디까지 처리됐는지
grep FAIL logs/trace.log      # 실패한 단계와 사유
```

주문의 현재 상태는 이력과 `STATUS`를 함께 보면 알 수 있다.

| `STATUS` | 마지막 이력 | 상태 |
|---|---|---|
| `Y` | `SHIPMENT OK` | 처리 완료 |
| `N` | `ORDER_SAVE OK` | 다음 주기 대기 |
| `N` | `RECEIPT FAIL` | 영수증 단계 실패, 다음 주기 재처리 |
| `N` | `RECEIPT OK` + `SHIPMENT FAIL` | 배송 단계 실패, 다음 주기 재처리 (영수증은 건너뜀) |

## 오류 재현

### 수신 오류 (데이터가 남지 않음)

```bash
curl -X POST -H "Content-Type: application/xml" --data-binary '<HEADER>' http://127.0.0.1:8080/orders
```

400 응답과 함께 `- | RECEIVE | FAIL | XML 형식이 올바르지 않습니다`가 기록된다.

### 영수증 단계 실패와 재처리

로컬 저장 폴더를 쓰기 권한이 없는 경로로 지정하면 `RECEIPT` 단계가 실패한다.

```bash
./gradlew bootRun --args='--job.interval-ms=5000 --receipt.backup-dir=/receipts'
```

```bash
curl -X POST -H "Content-Type: application/xml" --data-binary @samples/one-order.xml http://127.0.0.1:8080/orders
```

이력에 `RECEIPT FAIL`이 남고, FTP 업로드와 운송 입력은 진행되지 않으며 주문은 `N`으로 남는다. 서버를 끄고 원래 설정으로 다시 실행하면 다음 주기에 같은 주문이 자동으로 처리된다.

```bash
./gradlew bootRun --args='--job.interval-ms=5000'
```

## 시연 조회

```sql
SELECT * FROM ORDER_TB
WHERE APPLICANT_KEY = :key AND CREATE_TIME >= TRUNC(SYSDATE);

SELECT * FROM SHIPMENT_TB
WHERE APPLICANT_KEY = :key AND CREATE_DATE >= TRUNC(SYSDATE);
```

FTP 업로드 경로에서 `주원영_{ORDER_ID}.txt` 파일을 확인한다. FTP 계정은 다운로드가 불가능하므로, 업로드한 내용은 로컬 `receipts/`의 같은 이름 파일로 확인한다.

## 설계 판단

- **HEADER와 ITEM 연결:** 메시지에 루트 요소가 없고, ITEM이 바로 위 HEADER의 고객이 아닌 경우가 있다. 루트로 감싸 읽은 뒤 `USER_ID`로 연결하고, HEADER가 없거나 필수값이 빠진 ITEM은 저장하지 않고 사유를 남긴다.
- **문자 인코딩:** 제공된 주문 XML이 EUC-KR이라 요청 본문을 EUC-KR로 읽는다. `Content-Type`이 폼 형식이면 Spring이 본문을 폼 파라미터로 해석해 XML이 사라지므로, XML 형식만 받고 나머지는 415로 거부한다.
- **비동기 처리:** FTP나 운송 DB 장애가 주문 수신을 막지 않도록, API는 저장까지만 처리하고 후속 작업은 스케줄러가 맡는다.
- **상태 관리:** `STATUS`는 체크 제약조건으로 `N`, `Y`만 허용된다. `N`은 후속 처리 대기, `Y`는 영수증 업로드와 운송 입력 완료를 뜻한다. 중간 단계는 처리 이력으로 구분한다.
- **처리 순서:** 영수증 업로드가 성공해야 운송을 입력한다. 영수증이 실패하면 주문은 `N`으로 남아 다음 주기에 처음부터 다시 처리된다.
- **중복 방지:** FTP는 덮어쓰기와 삭제가 안 되므로, 접속할 때 파일 목록을 읽어 이미 있는 파일명은 업로드를 건너뛴다. 운송 입력과 `Y` 변경은 한 트랜잭션이라, 중간에 실패해도 배송이 중복 입력되지 않는다.
- **계층 분리:** DAO는 테이블별로 나누고(`OrderDao`, `ShipmentDao`), 두 테이블을 묶는 트랜잭션은 `ShipmentService`에서 처리한다.
- **ID 생성:** 시퀀스를 만들 권한이 없어 `MAX + 1`로 만든다. 동시 요청이 같은 ID를 받지 않도록 커밋까지 `synchronized` 안에서 처리한다.
- **접속 정보 보호:** 복호화에 필요한 `secret/`은 저장소에서 제외하고, 접속 정보는 로그에 출력하지 않는다.

## 한계

- 같은 메시지를 다시 보내면 새 주문으로 저장된다. 메시지에 중복을 판단할 주문번호가 없다.
- ID를 `MAX + 1`로 만들기 때문에 프로세스는 하나만 실행해야 한다.
- 업로드가 중간에 끊겨 불완전한 파일이 남으면 다음 주기에 이미 있는 파일로 판단한다.
- 처리 이력이 서버의 파일에만 남아, 서버를 옮기면 함께 옮겨야 한다. 운영 환경이라면 이력 테이블과 주문별 조회 API를 둔다.
