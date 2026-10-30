FROM ubuntu:latest
LABEL authors="Ganesh"

ENTRYPOINT ["top", "-b"]

# Stage 1: Build the application using Gradle
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy the gradle wrapper and config files first
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Grant execute permission to the gradlew script
RUN chmod +x gradlew

# Download dependencies (cached layer)
RUN ./gradlew dependencies --no-daemon

# Copy the source code and build the jar
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Render will automatically assign the PORT environment variable
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]