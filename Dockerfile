# 多阶段构建：第一阶段 - 构建应用
FROM maven:3.8.6-openjdk-11-slim AS build
WORKDIR /app

# 复制pom.xml并下载依赖（利用Docker缓存）
COPY pom.xml .
RUN mvn dependency:go-offline

# 复制源代码并构建
COPY src ./src
RUN mvn package -DskipTests

# 第二阶段 - 运行环境
FROM  public.ecr.aws/amazoncorretto/amazoncorretto:17-al2-jdk

# 创建非root用户并切换
#RUN addgroup -S appgroup && adduser -S appuser -G appgroup
#USER appuser

# 设置工作目录
WORKDIR /app

# 从构建阶段复制JAR文件
COPY --from=build /app/target/fraud-detection-system-1.0.0.jar app.jar

# 暴露应用端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
