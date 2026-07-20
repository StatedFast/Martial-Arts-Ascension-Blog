# Spring Blog 启动脚本 (PowerShell)
# 设置控制台 UTF-8 编码，确保中文日志正常显示

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"

Write-Host "===================================="
Write-Host "  Spring Blog 启动中..."
Write-Host "  控制台编码已设置为 UTF-8"
Write-Host "===================================="

.\mvnw.cmd spring-boot:run
