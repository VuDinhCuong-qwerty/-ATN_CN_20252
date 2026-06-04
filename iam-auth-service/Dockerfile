FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
COPY template/ template/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
# docker build -t <image-name>:<tag> .