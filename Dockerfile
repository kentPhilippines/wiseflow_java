FROM eclipse-temurin:11-jre
WORKDIR /app
RUN mkdir -p /app/data /app/logs
COPY app.jar app.jar
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
EXPOSE 8080
ENV SPRING_DATASOURCE_URL="jdbc:h2:file:/app/data/newsdb;DB_CLOSE_ON_EXIT=FALSE"
ENTRYPOINT ["/docker-entrypoint.sh"]
