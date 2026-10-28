# Multi-stage build for IPL D11 Stars Backend
# Stage 1: Build Java backend
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy root pom and backend pom for dependency caching
COPY pom.xml ./
COPY backend/pom.xml ./backend/

# Download dependencies in batch mode
WORKDIR /app/backend
RUN mvn dependency:go-offline -B || true

# Copy backend source
WORKDIR /app
COPY backend ./backend

# Run tests and package backend jar
WORKDIR /app/backend
RUN mvn clean package -B

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/backend/target/backend-*.jar app.jar

# Expose backend port
EXPOSE 8080

# Healthcheck hitting the health endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=15s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Dserver.port=8080", \
    "-jar", \
    "app.jar"]
