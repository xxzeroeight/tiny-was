# tiny-was
외부 라이브러리 없이 순수 Java만으로 HTTP 요청을 받고 처리하는 경량 WAS  
Spring Boot, Tomcat, Netty 없이 TCP 소켓부터 HTTP 파싱, 라우팅, 스레드 풀, Keep-Alive까지 구현

## 기술 스택
- Java 17
- Gradle
- JUnit 5

## 아키텍처
<img width="1561" height="789" alt="Image" src="https://github.com/user-attachments/assets/ff4e13b8-8b2d-415f-be19-548657c449de" />

## 구현 내용
### v0.1.0 - HTTP 기본
- TCP 소켓 기반 HTTP/1.1 요청 파싱, 응답
- request line, header, body 파싱
- 4xx/5xx 에러 응답

### v0.2.0 - 라우터 + 정적 파일
- URL 기반 핸들러 라우팅, 정적 파일 서빙 (핸들러가 없으면 StaticFileHandler로 fallback)
- path traversal 방어

### v0.3.0 - 스레드 풀
- `new Thread().start()` &rarr; `ExecutorService` 기반으로 전환
- `stop()` 호출 시 진행 중인 요청을 완료한 뒤 Graceful Shutdown 구현

### v0.4.0 - Keep-Alive
- 연결을 재사용하는 구조로 변경
- `maxRequests` 초과 또는 Timeout 시 연결을 종료

## 벤치마크
**환경:** M4 Pro / `ab -n 30000 -c 200 http://localhost:8080/hello`

**스레드 풀 사이즈별 (v0.3.0)**

| 풀 사이즈 | RPS   | 평균 응답시간 |
|-------|-------|---------|
| 10    | 6,707 | 29.8ms  |
| 50    | 6,680 | 29.9ms  |
| 100   | 6,643 | 30.1ms  |

풀 사이즈와 무관하게 수렴: 병목은 스레드 수가 아닌 TCP 연결 오버헤드(~15ms)

**Keep-Alive (v0.4.0)**

|            | ~v0.3.0 | v0.4.0                               |
|------------|---------|--------------------------------------|
| RPS        | 6,568   | 2,853                                |
| Connect 시간 | ~15ms   | <span style="color:red;">~0ms</span> |

Connect 시간: 15ms → 0ms, TCP 연결 재사용 확인

## 설계 판단
<!-- 
**BufferedInputStream 루프 외부 생성**
Keep-Alive 루프에서 매번 새로 감싸면 내부 버퍼에 미리 읽어둔 다음 요청 데이터가 유실됨

**Connection 헤더 토큰 리스트 파싱**
RFC 7230 기준 `Connection` 헤더는 콤마로 구분된 토큰 목록 → `close` 포함 여부로 판단

**MIME 타입 수동 매핑**
`Files.probeContentType()`은 OS마다 결과가 달라 확장자 기반 수동 매핑으로 대체

**Path Traversal 방어**
`resolve().normalize().startsWith(staticRoot)` 로 경로 조작 감지 → 방어 메커니즘 노출 없이 404 반환
-->

## 한계 (~v0.4.0)
<!--
- `newFixedThreadPool` 내부의 무제한 `LinkedBlockingQueue` → 과부하 시 OOM 위험
- Blocking I/O 기반 → 연결당 스레드 점유, 대규모 동시 연결 시 NIO 전환 필요
-->

## 실행
```bash
./gradlew build
./gradlew run
```