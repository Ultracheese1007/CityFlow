# 基础镜像：官方 Java 运行时环境，推荐使用 LTS 版本 21
FROM eclipse-temurin:21-jre

# 镜像内创建一个目录用于存放 jar 包
WORKDIR /app

# 拷贝 jar 包（通配符更灵活）
COPY target/*.jar app.jar

# 暴露应用端口（可选，用于文档提示）
EXPOSE 18080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]


