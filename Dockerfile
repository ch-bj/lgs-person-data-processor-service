FROM openjdk:11-jdk-slim as build
WORKDIR workspace/app

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src

RUN ./mvnw install -DskipTests
RUN mkdir -p target/dependency && (cd target/dependency; jar -xf ../*.jar)

FROM openjdk:11-jre-slim

RUN useradd -m -s /bin/sh spring
USER spring:spring

VOLUME /tmp
ARG DEPENDENCY=/workspace/app/target/dependency
COPY --from=build ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY --from=build ${DEPENDENCY}/META-INF /app/META-INF
COPY --from=build ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","app:app/lib/*","org.datarocks.lwgs.searchindex.client.SearchIndexClientApplication"]
