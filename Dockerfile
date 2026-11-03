# syntax=docker/dockerfile:1
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B -DskipTests -Dspotless.check.skip=true package \
    && java -Djarmode=tools -jar target/server-*.jar extract --layers --launcher --destination /workspace/layers

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/layers/dependencies/ ./
COPY --from=build /workspace/layers/spring-boot-loader/ ./
COPY --from=build /workspace/layers/snapshot-dependencies/ ./
COPY --from=build /workspace/layers/application/ ./
USER app
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
