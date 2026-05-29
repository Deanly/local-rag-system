FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl services/indexer-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/services/indexer-service/target/indexer-service-1.0.0.jar /app/app.jar
EXPOSE 42142
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
