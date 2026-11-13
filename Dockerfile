# syntax=docker/dockerfile:1
# One build recipe, parameterised by MODULE so each service shares the same base layers.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
ARG MODULE
RUN mvn -q -B -DskipTests -pl "${MODULE}" -am package

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
RUN useradd --system --uid 10001 payforge
COPY --from=build "/build/${MODULE}/target/${MODULE}-1.0.0.jar" app.jar
USER payforge
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
