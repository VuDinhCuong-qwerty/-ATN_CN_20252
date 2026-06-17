# Stage 1: Build minimal custom JRE with jlink
FROM eclipse-temurin:21-jdk-alpine AS jre-builder
RUN jlink --module-path /opt/java/openjdk/jmods \
    --add-modules java.base,java.compiler,java.desktop,java.instrument,java.management,java.management.rmi,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,jdk.charsets,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.jfr,jdk.localedata,jdk.management,jdk.management.jfr,jdk.naming.dns,jdk.naming.rmi,jdk.net,jdk.sctp,jdk.security.auth,jdk.security.jgss,jdk.unsupported,jdk.xml.dom,jdk.zipfs \
    --strip-debug --no-man-pages --no-header-files --compress zip-9 \
    --output /custom-jre

# Stage 2: Minimal Alpine + custom JRE
FROM alpine:3.21
ENV JAVA_HOME=/opt/jre
ENV PATH=$JAVA_HOME/bin:$PATH
COPY --from=jre-builder /custom-jre $JAVA_HOME
WORKDIR /app
COPY target/*.jar app.jar
COPY template/ template/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
