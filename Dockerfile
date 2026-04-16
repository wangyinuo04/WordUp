# 第一阶段：构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
# 复制整个仓库内容
COPY . .
# 直接指定 pom.xml 的路径来执行 Maven 打包
RUN mvn clean package -DskipTests -f /build/app-backend/pom.xml

# 第二阶段：运行阶段
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# 从构建阶段复制生成的 JAR 包
COPY --from=builder /build/app-backend/target/app-backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]