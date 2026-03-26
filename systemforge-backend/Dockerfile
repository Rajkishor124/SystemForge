# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy maven executable and configuration
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Give execute permission for maven wrapper
RUN chmod +x ./mvnw

# Resolve dependencies (go offline to cache them in a docker layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: Create a minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root user for security
RUN addgroup -S sfgroup && adduser -S sfuser -G sfgroup
USER sfuser

# Copy the built jar from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
