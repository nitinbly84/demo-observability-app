# Global ARG - The single source of truth
ARG BASE_IMAGE=eclipse-temurin:21-jdk-alpine
# Global ARG for the port
ARG APP_PORT=8080

# --- STAGE 1: Maven Build ---
FROM ${BASE_IMAGE} AS maven-builder
WORKDIR /build
# 1. Copy the Maven Wrapper and pom.xml first
# You need the .mvn folder and the mvnw script
COPY .mvn/ .mvn/
COPY mvnw pom.xml layers.xml ./
# 2. Fix line endings and permissions (Crucial for Windows users)
RUN tr -d '\r' < mvnw > mvnw_unix && \
    mv mvnw_unix mvnw && \
    chmod +x mvnw
# 3. Download dependencies
RUN ./mvnw dependency:go-offline -B || true
# 4. Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests&& ls -l target && ls -l target/classes/user.properties || echo "FILE NOT FOUND IN TARGET"

# --- STAGE 2: JLink Runtime ---
FROM ${BASE_IMAGE} AS jvm-builder
RUN apk add --no-cache binutils
RUN jlink \
    --add-modules java.base,java.logging,java.naming,java.desktop,java.management,jdk.management.agent,java.security.jgss,java.instrument,java.sql,jdk.unsupported,jdk.net,java.scripting,jdk.crypto.ec,java.compiler,java.xml,jdk.zipfs \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /custom-runtime

# --- STAGE 3: Application Extractor ---
FROM ${BASE_IMAGE} AS app-extractor
WORKDIR /extractor
COPY --from=maven-builder /build/target/demo-observability-app-1.0.0.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# --- STAGE 4: Final Hardened Production Image ---
FROM alpine:latest
# Re-declare the ARG in this stage to make it available
ARG APP_PORT 
ENV PORT=${APP_PORT}
# 1. Install Essential Libraries
RUN apk update && apk upgrade && \
    apk add --no-cache gcompat libstdc++ && \
    rm -rf /var/cache/apk/*
# 2. Set Environment & Workspace
ENV JAVA_HOME=/opt/jdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
WORKDIR /opt/app
# 3. Security: Non-root User
RUN addgroup -S spring && adduser -S spring -G spring
# 4. Copy Configurations (Vault)
# We copy the folder from the host. Because of .dockerignore, 'data' is skipped.
COPY --chown=spring:spring vault/ ./vault/
# 5. Copy Components from Previous Stages
COPY --from=jvm-builder /custom-runtime /opt/jdk
COPY --from=app-extractor /extractor/dependencies/ ./
COPY --from=app-extractor /extractor/spring-boot-loader/ ./
COPY --from=app-extractor /extractor/snapshot-dependencies/ ./
COPY --from=app-extractor /extractor/application/ ./
# NEW: Copy the resources layer which now contains your .properties files
COPY --from=app-extractor /extractor/resources/ ./

USER spring:spring

# 6. Ports & Healthcheck
EXPOSE ${PORT}
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD wget -q --spider http://localhost:${PORT}/actuator/health || exit 1

# 7. Entrypoint
ENTRYPOINT ["java", \
            "-XX:InitialRAMPercentage=50.0", \
            "-XX:MaxRAMPercentage=80.0", \
            "-XX:+UseParallelGC", \
			"-Dloader.path=BOOT-INF/classes,BOOT-INF/lib", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "org.springframework.boot.loader.launch.JarLauncher"]