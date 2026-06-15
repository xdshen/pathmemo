@echo off
chcp 65001 >nul

where python >nul 2>nul
if errorlevel 1 (
    echo [错误] 未找到 python，请确保 Python 已安装并加入系统 PATH。
    pause
    exit /b 1
)

cd /d "%~dp0\tools\web_viewer"
echo 正在启动 PathMemo Web Viewer...
python web_viewer.py
pause
