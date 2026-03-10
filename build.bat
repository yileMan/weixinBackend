@echo off

:: 切换UTF-8编码
chcp 65001 > nul

:: 设置Maven编码
set MAVEN_OPTS=-Dfile.encoding=UTF-8

setlocal enabledelayedexpansion

:: 切换到脚本所在目录
cd /d "%~dp0"

echo 📍 当前目录: %cd%
echo ======================== 开始Maven打包 ========================

call mvn clean package -DskipTests

if %errorlevel% neq 0 (
    echo ❌ Maven打包失败
    pause
    exit /b 1
)

echo ✅ Maven打包成功

echo ======================== 查找JAR文件 ========================

set "source_jar="

for /f "delims=" %%i in ('dir "%cd%\target\*.jar" /b ^| findstr /v ".original"') do (
    set "source_jar=%cd%\target\%%i"
)

if not defined source_jar (
    echo ❌ 未找到可执行JAR
    pause
    exit /b 1
)

echo 📦 JAR文件: !source_jar!

:: ======================== History备份 ========================

set "history_dir=%cd%\History"

if not exist "!history_dir!" (
    mkdir "!history_dir!"
)

copy /y "!source_jar!" "!history_dir!" >nul

if %errorlevel% neq 0 (
    echo ❌ 备份失败
    pause
    exit /b 1
)

echo ✅ 已备份到 History

:: ======================== cloud目录处理 ========================

set "target_dir=%cd%\..\..\cloud"

if not exist "!target_dir!" (
    mkdir "!target_dir!"
)

echo 🗑 清理旧JAR
del /q /f "!target_dir!\*.jar" >nul 2>&1

echo 📤 复制新JAR
copy /y "!source_jar!" "!target_dir!" >nul

if %errorlevel% neq 0 (
    echo ❌ 复制JAR失败
    pause
    exit /b 1
)

echo ✅ 新JAR复制完成

:: ======================== Git提交 ========================

cd /d "!target_dir!"

echo ======================== Git提交推送 ========================

git add -A
git commit -m "update jar"
git push

if %errorlevel% neq 0 (
    echo ❌ Git推送失败
    pause
    exit /b 1
)

echo ✅ Git推送成功

echo ======================== 完成 ========================

pause