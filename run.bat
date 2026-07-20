@echo off
chcp 65001 >nul
echo ====================================
echo   Spring Blog 启动中...
echo   控制台编码已设置为 UTF-8
echo ====================================
call .\mvnw.cmd spring-boot:run
