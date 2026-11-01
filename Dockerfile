FROM eclipse-temurin:11-jre
WORKDIR /app
RUN mkdir -p /app/data /app/logs
COPY app.jar app.jar
EXPOSE 8080
ENV SYNC_API_BASE_URL=http://127.0.0.1:18082
ENTRYPOINT ["java", "-jar", "app.jar"]
