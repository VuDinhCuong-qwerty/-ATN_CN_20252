# ── Stage 1: Build Angular ──────────────────────────────────────────────────
FROM node:20-alpine AS angular-build
WORKDIR /build/web
COPY change-web-service/package*.json ./
RUN npm ci --ignore-scripts
COPY change-web-service/ ./
RUN npx ng build --configuration=production --base-href=/change-app-service/

# ── Stage 2: Build Spring Boot (embed Angular dist vào static/) ─────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS maven-build
WORKDIR /build/app
COPY change-app-service/pom.xml ./
RUN mvn dependency:go-offline -B -q
COPY change-app-service/src ./src
COPY --from=angular-build /build/web/dist/change-web-service/browser ./src/main/resources/static
RUN mvn clean package -DskipTests -B -q

# ── Stage 3: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=maven-build /build/app/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
