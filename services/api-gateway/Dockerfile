FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl services/api-gateway -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/services/api-gateway/target/api-gateway-1.2.1.jar /app/app.jar
EXPOSE 42120
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
