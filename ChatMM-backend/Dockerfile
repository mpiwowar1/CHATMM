# Multi-stage build dla Spring Boot

FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Kopiowanie pom.xml
COPY pom.xml .

# Pobieranie zależności
RUN mvn dependency:go-offline -B

# Kopiowanie kodu
COPY src ./src

# Budowanie
RUN mvn clean package -DskipTests

# Runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
