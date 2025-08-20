# syntax=docker/dockerfile:1

# === Build stage ===
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# 依存取得＆ビルド（テスト省略）
RUN mvn -q -B -DskipTests package

# === Run stage ===
FROM eclipse-temurin:17-jre
ENV PORT=8080
WORKDIR /app
# 生成された JAR を1つだけ app.jar としてコピー（finalName 不問）
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
# application.properties に server.port=${PORT:8080} があればこのままでOK
CMD ["java","-jar","/app/app.jar"]
# ※もし server.port の変更をまだ入れていない場合は代わりに↓でもOK
# CMD ["bash","-lc","exec java -jar /app/app.jar --server.port=${PORT}"]
