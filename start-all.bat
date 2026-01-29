@echo off
chcp 65001
echo ===========================================
echo 正在启动 Secure Pastebin 服务 (本地/局域网模式)...
echo ===========================================

REM 1. 启动后端 (Spring Boot)
echo [1/2] 正在启动后端服务 (8080端口)...
start "Pastebin Backend Domain" cmd /c "mvn spring-boot:run"

REM 2. 启动前端 (Vue)
echo [2/2] 正在启动前端服务 (3000端口)...
cd frontend
start "Pastebin Frontend" cmd /c "npm run dev"

cd ..
echo.
echo 服务已启动。
echo 本机访问: http://localhost:3000
echo 局域网访问: http://10.10.9.201:3000
echo.
pause