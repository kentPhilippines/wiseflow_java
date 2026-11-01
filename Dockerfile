FROM maven:3.9.9-eclipse-temurin-11 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:11-jre
WORKDIR /app
RUN mkdir -p /app/data /app/logs
COPY --from=build /build/target/wiseflow_java-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV SYNC_API_BASE_URL=http://127.0.0.1:18082
ENTRYPOINT ["java", "-jar", "app.jar"]
