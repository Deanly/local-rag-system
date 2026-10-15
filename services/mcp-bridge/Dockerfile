FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl services/mcp-bridge -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/services/mcp-bridge/target/mcp-bridge-1.2.1.jar /app/app.jar
EXPOSE 42144
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
