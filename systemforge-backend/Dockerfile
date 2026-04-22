# ============================================================
# SystemForge Backend — Multi-Stage Docker Build
# ============================================================
# Stage 1: Build with JDK 21 + Maven
# Stage 2: Run with JRE 21 only (~200MB final image)
# ============================================================

# ─── BUILD STAGE ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Cache Maven dependencies (only re-downloads when pom.xml changes)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -B -q

# Copy source and build
COPY src/ src/
RUN ./mvnw package -DskipTests -B -q && \
    mv target/*.jar target/app.jar

# ─── RUNTIME STAGE ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root user
RUN addgroup --system --gid 1001 appgroup && \
    adduser --system --uid 1001 --ingroup appgroup appuser

WORKDIR /app

# Copy only the fat JAR — no source code in the final image
COPY --from=builder /build/target/app.jar ./app.jar

# Create log directory
RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

USER appuser

# Expose Spring Boot port
EXPOSE 8080

# Health check using Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM tuning for containers
# - G1GC: good default for services with varying workloads
# - MaxRAMPercentage: uses 75% of container memory limit
# - ExitOnOutOfMemoryError: fail-fast rather than limping along
ENV JAVA_OPTS="-XX:+UseG1GC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
