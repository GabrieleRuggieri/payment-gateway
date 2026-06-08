FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml .
COPY common common
COPY settlement-service settlement-service
COPY payment-service/pom.xml payment-service/pom.xml
COPY authorization-service/pom.xml authorization-service/pom.xml
COPY capture-service/pom.xml capture-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml

RUN apk add --no-cache maven && \
    mvn -pl settlement-service -am package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/settlement-service/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
