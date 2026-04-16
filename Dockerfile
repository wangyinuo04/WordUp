# 第一阶段：构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
# 复制整个后端项目内容
COPY . .
# 在当前目录执行 Maven 打包（因为 pom.xml 就在这里）
RUN mvn clean package -DskipTests

# 第二阶段：运行阶段
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# 从构建阶段复制生成的 JAR 包
COPY --from=builder /build/target/app-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]