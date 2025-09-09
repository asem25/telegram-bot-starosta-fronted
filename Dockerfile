# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests clean package

# ---- run ----
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
# для healthcheck (curl)
RUN apk add --no-cache curl
COPY --from=build /app/target/bot-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_TOOL_OPTIONS=" \
 -XX:ActiveProcessorCount=1 \
 -XX:+UseSerialGC \
 -XX:MaxRAMPercentage=58 -XX:InitialRAMPercentage=22 \
 -Xss256k \
 -XX:MaxMetaspaceSize=64m \
 -XX:ReservedCodeCacheSize=12m -XX:MaxDirectMemorySize=12m \
 -XX:+ClassUnloading -XX:+ExitOnOutOfMemoryError \
 -Dspring.main.lazy-initialization=true \
 -Dserver.tomcat.max-threads=12 -Dserver.tomcat.accept-count=25 \
 -Dspring.datasource.hikari.maximum-pool-size=2 -Dspring.datasource.hikari.minimum-idle=0 \
"
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
