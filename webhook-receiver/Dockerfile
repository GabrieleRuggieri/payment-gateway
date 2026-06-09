FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY webhook-receiver webhook-receiver
COPY common/pom.xml common/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY authorization-service/pom.xml authorization-service/pom.xml
COPY capture-service/pom.xml capture-service/pom.xml
COPY settlement-service/pom.xml settlement-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml

RUN apk add --no-cache maven && \
    mvn -pl webhook-receiver -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/webhook-receiver/target/*.jar app.jar

EXPOSE 8099
ENTRYPOINT ["java", "-jar", "app.jar"]
