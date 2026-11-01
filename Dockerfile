FROM eclipse-temurin:11-jre
WORKDIR /app
RUN mkdir -p /app/data /app/logs
COPY app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
