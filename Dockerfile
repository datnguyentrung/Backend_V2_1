# Stage 1: Build với Maven và Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
# Build bỏ qua tests để tiết kiệm thời gian và tài nguyên trên Render
RUN mvn clean package -DskipTests

# Stage 2: Chạy ứng dụng với JRE 21 nhẹ
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Tối ưu RAM cho môi trường Render (giới hạn 512MB)
# -Xmx300m: Giới hạn Heap Memory ở mức 300MB để dành phần còn lại cho hệ thống và Metaspace
ENV JAVA_OPTS="-Xmx300m -Xms256m -XX:+UseSerialGC -Xss512k"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]