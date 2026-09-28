FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

COPY contracts contracts
COPY analytics-service analytics-service

RUN chmod +x gradlew
RUN ./gradlew :analytics-service:bootJar -x test

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=builder /app/analytics-service/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]