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
# -Xmx256m: Dành thêm RAM cho Metaspace, native memory và network buffers trên Render 512MB.
ENV JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
