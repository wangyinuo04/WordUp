# 第一阶段：构建阶段，使用Maven镜像
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
# 复制整个项目
COPY . .
# 进入后端目录并编译打包
WORKDIR /build/app-backend
RUN mvn clean package -DskipTests

# 第二阶段：运行阶段，使用轻量JRE镜像
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# 从构建阶段复制生成的JAR包
COPY --from=builder /build/app-backend/target/app-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]