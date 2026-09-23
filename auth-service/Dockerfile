FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml ./
COPY event-contracts/pom.xml event-contracts/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY ingestion-service/pom.xml ingestion-service/pom.xml
COPY rules-engine-service/pom.xml rules-engine-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
RUN mvn -q -N validate
COPY event-contracts/src event-contracts/src
COPY auth-service/src auth-service/src
RUN mvn -q -pl auth-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S streampulse && adduser -S streampulse -G streampulse
WORKDIR /app
COPY --from=build /build/auth-service/target/auth-service.jar app.jar
USER streampulse
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
