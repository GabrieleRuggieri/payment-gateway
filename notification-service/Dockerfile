FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common common
COPY notification-service notification-service
COPY payment-service/pom.xml payment-service/pom.xml
COPY authorization-service/pom.xml authorization-service/pom.xml
COPY capture-service/pom.xml capture-service/pom.xml
COPY settlement-service/pom.xml settlement-service/pom.xml
COPY webhook-receiver/pom.xml webhook-receiver/pom.xml

RUN apk add --no-cache maven && \
    mvn -pl notification-service -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/notification-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
