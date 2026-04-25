# syntax=docker/dockerfile:1.7

# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Maven wrapper を使わずに maven:3.9 を利用
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    apt-get update && apt-get install -y --no-install-recommends maven && rm -rf /var/lib/apt/lists/* \
 && mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -DskipTests package \
 && cp target/family-schedule-*.jar app.jar

# --- Runtime stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
