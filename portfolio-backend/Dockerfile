# Multi-stage Dockerfile for production
# Stage 1: Build the application
FROM gradle:8.11.1-jdk21-alpine AS build

WORKDIR /app

# Copy only necessary files for build
COPY build.gradle settings.gradle ./

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application
RUN gradle clean bootJar -x test --no-daemon

# Stage 2: Runtime image
FROM eclipse-temurin:21.0.5_11-jre-alpine

WORKDIR /app

# Install wget for health checks
RUN apk add --no-cache wget

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/build/libs/*.jar app.jar

# Create uploads directories with proper permissions
RUN mkdir -p uploads/images uploads/cvs uploads/projects uploads/icons && \
    chown -R spring:spring uploads

# Change ownership to non-root user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

EXPOSE 8080

# Health check managed by docker-compose for better flexibility
# CMD: wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
