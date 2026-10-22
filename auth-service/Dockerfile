FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

COPY auth-service auth-service

RUN chmod +x gradlew
RUN ./gradlew :auth-service:bootJar -x test

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=builder /app/auth-service/build/libs/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
