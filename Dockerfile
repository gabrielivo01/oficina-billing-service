FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/oficina-billing-service-0.0.1-SNAPSHOT.jar app.jar
USER 10001
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
