FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app
COPY aa1-web/ ./aa1-web/

WORKDIR /app/aa1-web
RUN find lib -name "*Javadoc*.jar" -delete \
    && mkdir -p server-out \
    && javac -cp "lib/ojdbc11.jar" -d server-out backend/*.java

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app/aa1-web
COPY --from=build /app/aa1-web ./

ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
EXPOSE 10000

CMD ["java", "-cp", "server-out:lib/*", "GestionDBServer"]
