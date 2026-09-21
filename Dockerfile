FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY . .

RUN mkdir -p out && javac -encoding UTF-8 -d out WebMain.java model/*.java factory/*.java singleton/*.java chain/*.java observer/*.java proxy/*.java

CMD ["java", "-cp", "out", "WebMain"]