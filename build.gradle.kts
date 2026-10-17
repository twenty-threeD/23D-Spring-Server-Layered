plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot") version "3.5.13"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.google.cloud.tools.jib") version "3.5.4"
}

group = "spring"
version = "0.0.1-SNAPSHOT"
description = "springServer"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

dependencies {

    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-messaging")

    // Phone
    implementation("com.github.L98293:phone:2.1.1")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // PostgreSQL
    runtimeOnly("org.postgresql:postgresql")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Cache (Caffeine 로컬 캐시)
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // unit
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // JJWT
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    implementation("io.jsonwebtoken:jjwt-impl:0.13.0")
    implementation("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // Dotenv
    developmentOnly("me.paulschwarz:springboot3-dotenv:5.1.0")

    // JPA
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Mail
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // Thymeleaf
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")

    // Actuator + Prometheus
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // spring boot test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

    // WebSocket
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // OAuth
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // tika
    implementation("org.apache.tika:tika-core:3.2.1")
    implementation("org.apache.tika:tika-parsers-standard-package:3.2.1")

    // secp256k1
    implementation("org.bouncycastle:bcprov-jdk18on:1.80")

    // Solapi
    implementation("com.solapi:sdk:1.0.3")

    // Agora (RTC 토큰 발급)
    implementation("io.agora:authentication:2.1.3")

    // Anthropic (Claude API, 리뷰 AI 분석)
    // anthropic-java-core 가 끌어오는 swagger-annotations 2.2.31 이 springdoc 의 jakarta 변종(2.2.47)
    // 과 같은 Schema 클래스를 담아 로드 순서에 따라 Schema.$dynamicRef() 가 사라져 /v3/api-docs 가 500 난다.
    implementation("com.anthropic:anthropic-java:2.34.0") {
        exclude(group = "io.swagger.core.v3", module = "swagger-annotations")
    }

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

// 의존성/리소스/클래스를 별도 레이어로 나눠, 코드만 바뀌면 작은 레이어만 push·pull 되게 한다.
// 이미지 이름·태그는 CI에서 -Djib.to.image, -Djib.to.tags로 넘긴다.
jib {
    from {
        image = "eclipse-temurin:21-jre-alpine"
    }
    container {
        // upload.file-dir(./files)이 /app/files를 가리키도록 작업 디렉터리를 맞춘다
        workingDirectory = "/app"
        ports = listOf("8080")
    }
}