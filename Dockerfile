# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests clean package

# ---- run ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# для healthcheck (curl)
RUN apk add --no-cache curl
COPY --from=build /app/target/bot-0.0.1-SNAPSHOT.jar app.jar

ENV JAVA_TOOL_OPTIONS="\
 -XX:ActiveProcessorCount=1 \
 -XX:+UseSerialGC -Xms64m -Xmx128m \
 -XX:MaxMetaspaceSize=96m -XX:ReservedCodeCacheSize=32m \
 -Dspring.main.lazy-initialization=true \
 -Dserver.tomcat.max-threads=30 \
 -Dspring.datasource.hikari.maximum-pool-size=3 \
 -Dspring.datasource.hikari.minimum-idle=0 \
"
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
