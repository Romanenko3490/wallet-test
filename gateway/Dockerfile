FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8080"]