# syntax=docker/dockerfile:1.7
# ===== Stage 1: Build =====
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew

# GitHub Packages 인증 후 의존성 다운로드
ARG GITHUB_USER
ARG GITHUB_TOKEN
ENV GITHUB_USER=$GITHUB_USER
ENV GITHUB_TOKEN=$GITHUB_TOKEN

RUN ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew clean bootJar --no-daemon -x test

RUN java -Djarmode=layertools -jar build/libs/*.jar extract

# ===== Stage 2: Runtime =====
FROM eclipse-temurin:21-jre-alpine

# healthcheck 위해 curl 설치
RUN apk add --no-cache curl

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

COPY --from=builder --chown=appuser:appgroup /app/dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /app/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appgroup /app/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appgroup /app/application/ ./

USER appuser
EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
