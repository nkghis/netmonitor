# ========== BUILD STAGE ==========
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ========== RUNTIME STAGE ==========
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# ✅ Installer ping
RUN apt-get update && \
    apt-get install -y iputils-ping && \
    rm -rf /var/lib/apt/lists/*

# Security: non-root user
RUN groupadd -r netwatch && useradd -r -g netwatch netwatch

ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom \
               -Dfile.encoding=UTF-8"

COPY --from=build /app/target/netwatch.jar app.jar

# ✅ Donner le droit ICMP au binaire ping pour user non-root
RUN chown netwatch:netwatch app.jar && \
    setcap cap_net_raw+ep /bin/ping

USER netwatch

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
## ========== BUILD STAGE ==========
#FROM maven:3.9.6-eclipse-temurin-17 AS build
#WORKDIR /app
#
## Cache dependencies
#COPY pom.xml .
#RUN mvn dependency:go-offline -B
#
## Build
#COPY src ./src
#RUN mvn clean package -DskipTests -B
#
## ========== RUNTIME STAGE ==========
#FROM eclipse-temurin:17-jre-jammy
#WORKDIR /app
#
## Security: non-root user
#RUN groupadd -r netwatch && useradd -r -g netwatch netwatch
#
## JVM tuning pour conteneur
#ENV JAVA_OPTS="-XX:+UseContainerSupport \
#               -XX:MaxRAMPercentage=75.0 \
#               -XX:+UseG1GC \
#               -XX:+UseStringDeduplication \
#               -Djava.security.egd=file:/dev/./urandom \
#               -Dfile.encoding=UTF-8"
#
#COPY --from=build /app/target/netwatch.jar app.jar
#RUN chown netwatch:netwatch app.jar
#
#USER netwatch
#
#EXPOSE 8080
#
#HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
#    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1
#
#ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
