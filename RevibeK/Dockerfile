# --- build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 의존성만 먼저 받아서 캐시 레이어 분리 (소스 변경 시 매번 재다운로드 방지)
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- run stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
