# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy gradle files
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY src ./src

# Build the application
RUN gradle installDist --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built application from build stage
COPY --from=build /app/build/install/backend ./

# Expose port
EXPOSE 8080

# Set environment variables
ENV PORT=8080
ENV KTOR_ENV=production

# Run the application
CMD ["./bin/backend"]
