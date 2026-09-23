# Multi-stage Dockerfile for FastFleet Corporate Billing Service
# Stage 1: Build the application artifact
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and POM configuration
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw

# Copy source code and build runnable JAR
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Secure and minimal JRE runtime
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Create unprivileged application user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled executable JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Set user permissions
RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
