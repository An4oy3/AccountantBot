# Stage 1: Build the JAR file
FROM gradle:8.8-jdk17 AS builder
WORKDIR /app
COPY . .
COPY gradlew gradlew
COPY gradle gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew clean bootJar -x test --no-daemon

# Stage 2: Create the final image
FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
CMD ["java", "-jar", "app.jar"]

# Expose port
EXPOSE 8080