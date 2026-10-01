FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -ntp clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN groupadd --gid 1000 render-secrets \
    && useradd --uid 10001 --gid 1000 --no-create-home --shell /usr/sbin/nologin orman \
    && mkdir -p /app/storage \
    && chown -R 10001:1000 /app/storage

COPY --from=build --chown=10001:1000 /app/target/backend-0.0.1-SNAPSHOT.jar /app/app.jar

USER 10001:1000

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
