# ---- Этап 1: Сборка ----
FROM eclipse-temurin:21-jdk AS build

# Устанавливаем Maven (Ubuntu/Debian-подобный пакетный менеджер)
RUN apt-get update && apt-get install -y maven

WORKDIR /app

# Копируем pom.xml, загружаем зависимости в offline-режим
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходники
COPY src ./src

# Собираем jar-файл, пропуская тесты (если нужно)
RUN mvn clean package -DskipTests

# ---- Этап 2: Запуск ----
FROM eclipse-temurin:21-jre

WORKDIR /app

# Копируем сборку из первого этапа
COPY --from=build /app/target/bot-0.0.1-SNAPSHOT.jar app.jar

# Если нужно, открываем порт
EXPOSE 8080

# Параметры для слабого сервера:
# - XX:ActiveProcessorCount=1  => говорит JVM, что у нас 1 ядро
# - Xmx256m                    => максимум 256МБ памяти для хипа
# -Dspring.profiles.active=prod => при наличии production-профиля
ENTRYPOINT ["java", "-XX:ActiveProcessorCount=1", "-Xmx256m", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
