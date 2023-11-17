FROM gradle:8.4-jdk17-alpine AS build

WORKDIR /app
ADD . .
RUN gradle shadowJar

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY --from=build /app/app/build/libs/REBot-all.jar .
CMD ["java", "-jar", "REBot-all.jar"]
