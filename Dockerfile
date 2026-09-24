# ==============================================================================
# Multi-Stage Dockerfile for ForensicLogEngine (Spring Boot 3.2 + Java 17)
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build stage
# ------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build application jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Runtime stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
LABEL maintainer="Aditya Pandey"
LABEL description="Forensic Log Engine - Scalable Spring Boot Log Ingestion & IOC Detection Engine"
LABEL version="2.0.0"

WORKDIR /app

# Copy built artifact from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose default HTTP port
EXPOSE 8080

# Health check to ensure service readiness
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -q --spider http://localhost:8080/api/stats || exit 1

# Execute Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
