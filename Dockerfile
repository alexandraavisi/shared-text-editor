FROM eclipse-temurin:23-jre

WORKDIR /app

COPY out/production/Proiect/ .

COPY server-files/ server-files/

EXPOSE 5000

CMD ["java", "server.ServerMain"]