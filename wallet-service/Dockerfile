FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY wallet-service/target/*.jar app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=9090"]