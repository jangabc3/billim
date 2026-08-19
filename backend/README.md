# 빌림 백엔드 — 로컬 실행 방법

## 1. DB 실행 (PostgreSQL + PostGIS)

Docker Desktop이 켜져 있는 상태에서:

```bash
docker compose up -d
```

정상이면 `docker ps`에 `billim-db` 컨테이너가 보임.

## 2. 프로젝트 실행

IntelliJ에서 이 폴더를 Gradle 프로젝트로 열거나, 터미널에서:

```bash
./gradlew bootRun
```

(윈도우는 `gradlew.bat bootRun`. gradlew 실행파일이 없다면 IntelliJ로 열었을 때 자동 생성됨)

## 3. 확인

브라우저나 Postman으로:

```
GET http://localhost:8080/api/v1/resources
```

`data.sql`에 넣어둔 시드 데이터 3건(응봉공원 다목적구장, 서울역사박물관 강당, 충전 전동드릴)이 JSON으로 나오면 성공.

## 다음 단계

- 이 시드 데이터를 지우고, `SeoulReservationAdapter`가 실제 API 응답을 파싱해서 DB에 저장하는 배치/스케줄러로 교체
- `/api/v1/resources`에 `category`, `gu`, `freeOnly` 같은 쿼리 파라미터 검색 기능 추가 (QueryDSL)
- 프론트(v7 UI)의 예시 데이터를 이 API 응답으로 교체
