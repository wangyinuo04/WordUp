# 第一阶段：构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
# 复制整个仓库（因为 Dockerfile 在根目录，上下文是根目录）
COPY . .
# 进入后端目录并打包
WORKDIR /build/app-backend
RUN mvn clean package -DskipTests

# 第二阶段：运行阶段
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# 从构建阶段复制打包好的 JAR 文件
COPY --from=builder /build/app-backend/target/app-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]