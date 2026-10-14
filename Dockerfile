# -------------------------------------------------------------
# Stage 1: Build the Spring Boot application using Maven & Java 17
# -------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# -------------------------------------------------------------
# Stage 2: Minimal, secure runtime image using JRE 17
# -------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as a dedicated non-root user for security best practices
RUN groupadd -r spring && useradd -r -g spring spring

# Create uploads directory and ensure write permissions for non-root user
RUN mkdir -p /app/uploads && chown -R spring:spring /app

# Copy executable jar from builder stage without hardcoding the artifact name
COPY --from=builder /app/target/*.jar app.jar
RUN chown spring:spring app.jar

USER spring:spring

# Expose default port (Render sets $PORT dynamically at runtime)
EXPOSE 8080

# Configure JVM options for container memory limits
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Launch Spring Boot with dynamic PORT assignment from Render
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
