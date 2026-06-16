FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl services/source-registry-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/services/source-registry-service/target/source-registry-service-1.2.0.jar /app/app.jar
EXPOSE 42141
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
