FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /build

# Шаг 1: собираем библиотеку и устанавливаем в локальный Maven-репозиторий
COPY parallel-sort-lib/pom.xml parallel-sort-lib/
COPY parallel-sort-lib/src ./parallel-sort-lib/src
RUN cd parallel-sort-lib && mvn clean install -DskipTests -q

# Шаг 2: собираем core-service
WORKDIR /app
COPY core-service/pom.xml .
RUN mvn dependency:go-offline -q
COPY core-service/src ./src
RUN mvn clean package -DskipTests

FROM mirror.gcr.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/computer-store-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]