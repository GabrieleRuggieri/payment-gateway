FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common common
COPY authorization-service authorization-service
COPY payment-service/pom.xml payment-service/pom.xml
COPY capture-service/pom.xml capture-service/pom.xml
COPY settlement-service/pom.xml settlement-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml

RUN apk add --no-cache maven && \
    mvn -pl authorization-service -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/authorization-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
