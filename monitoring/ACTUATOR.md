# Spring Boot Actuator 연동

## 현재 상태

`~/app.jar` 을 열어 확인한 결과, Actuator 는 **설치되어 있지 않다.**

```
BOOT-INF/lib/micrometer-observation-1.15.10.jar   ← spring-boot-starter-web 전이 의존성
BOOT-INF/lib/micrometer-commons-1.15.10.jar       ← 같음
spring-boot-starter-actuator                       ← 없음
micrometer-registry-prometheus                     ← 없음
```

Micrometer 1.15.10 은 **Spring Boot 3.5.x** 계열이다. 아래 설정은 그 기준이다.

## 1. 의존성 추가

Gradle:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-actuator")
runtimeOnly("io.micrometer:micrometer-registry-prometheus")
```

Maven:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
  <scope>runtime</scope>
</dependency>
```

## 2. application.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        # health 와 prometheus 만. env / heapdump / threaddump 는
        # 환경변수와 자격증명을 그대로 흘리므로 절대 넣지 않는다.
        include: health,prometheus
  endpoint:
    health:
      show-details: never
  # Spring Boot 3.x 에서 이 프로퍼티 경로가 바뀌었다.
  # (구) management.metrics.export.prometheus.enabled
  prometheus:
    metrics:
      export:
        enabled: true
  metrics:
    tags:
      application: itda-backend
```

## 3. 카디널리티 — 여기서 Prometheus 가 죽는다

Spring Boot 의 `http_server_requests` 는 `uri` 를 라벨로 쓴다. path variable 이
템플릿으로 정규화되지 않으면 요청마다 새 시계열이 생겨 메모리가 폭발한다.

```
나쁨:  uri="/users/1"  uri="/users/2"  uri="/users/3"  ...  → 무한 증식
좋음:  uri="/users/{id}"                                    → 시계열 1개
```

`@PathVariable` 을 쓰는 정상적인 `@GetMapping("/users/{id}")` 라면 자동으로
템플릿화된다. 문제가 되는 건 직접 문자열로 URL 을 만들거나 필터에서 태그를
붙이는 경우다. 배포 후 반드시 확인할 것:

```bash
curl -s localhost:8080/actuator/prometheus | grep http_server_requests | wc -l
```

수백 줄이 넘으면 카디널리티 문제를 의심한다. 상한을 걸어두려면:

```yaml
management:
  metrics:
    web:
      server:
        max-uri-tags: 100
```

## 4. Spring Security — 중요한 함정

현재 이 앱은 인증 없는 모든 요청에 401 을 반환한다(`/zzz-nonexistent` 도 401).
그대로 두면 Prometheus 스크레이프도 401 로 실패한다.

**함정:** "localhost 만 허용" 으로 풀면 안 된다. Prometheus 는 컨테이너에서
`host.docker.internal` 을 거쳐 오므로, 앱이 보는 `remoteAddr` 은 루프백이 아니라
**Docker 브리지 게이트웨이 IP(172.x.x.x)** 다.

### 방법 A — Docker 브리지 대역 허용 (간단)

```java
@Bean
@Order(1)
SecurityFilterChain actuatorChain(HttpSecurity http) throws Exception {
    IpAddressMatcher docker = new IpAddressMatcher("172.16.0.0/12");
    IpAddressMatcher loopback = new IpAddressMatcher("127.0.0.1/8");

    http.securityMatcher("/actuator/**")
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/actuator/prometheus").access((authn, ctx) -> {
                var req = ctx.getRequest();
                return new AuthorizationDecision(
                    docker.matches(req) || loopback.matches(req));
            })
            .anyRequest().denyAll())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    return http.build();
}
```

### 방법 B — 스크레이프 전용 계정 (엄격)

basic auth 계정을 하나 만들고 `prometheus.yml` 의 `basic_auth` 주석을 해제한다.
IP 대역에 의존하지 않아 더 안전하다.

## 5. nginx 차단 — 반드시 같이 할 것

방법 A 든 B 든, `/actuator` 가 외부에서 닿으면 안 된다. nginx 서버 블록에 추가:

```nginx
location /actuator {
    deny all;
    return 404;
}
```

적용 후 밖에서 확인:

```bash
curl -s -o /dev/null -w "%{http_code}\n" https://<도메인>/actuator/health   # 404 여야 정상
```

## 6. 검증

재배포 후 서버에서:

```bash
curl -s localhost:8080/actuator/prometheus | head -20
```

메트릭이 나오면 Prometheus 의 Targets 화면(`localhost:9090/targets`)에서
`spring-boot` job 이 UP 으로 바뀐다.

## 7. 이걸로 무엇을 보게 되나

- `jvm_memory_used_bytes` — 힙/논힙 사용량. **`-Xmx` 미설정 문제를 눈으로 확인 가능**
- `jvm_gc_pause_seconds` — GC 정지 시간
- `http_server_requests_seconds` — 엔드포인트별 처리량·지연·에러율
- `hikaricp_connections_active` — DB 커넥션 풀 (PostgreSQL 병목 진단)
- `process_cpu_usage`, `system_cpu_usage`
