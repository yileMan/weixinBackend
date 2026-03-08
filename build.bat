@echo off
:: 1. 切换终端为UTF-8编码（解决中文乱码）
chcp 65001 > nul
:: 2. 设置Maven输出编码为UTF-8
set MAVEN_OPTS=-Dfile.encoding=UTF-8
:: 3. 启用延迟扩展（避免变量解析乱码）
setlocal enabledelayedexpansion

:: 强制切换到脚本所在目录（核心！确保路径正确）
cd /d "%~dp0"
echo 📍 脚本所在目录（当前工作目录）：%cd%

echo ======================== 开始执行Maven打包 ========================
:: 执行Maven打包（清理+打包，跳过测试）
call mvn clean package -DskipTests

:: 检查打包是否成功
if %errorlevel% neq 0 (
    echo ❌ Maven打包失败，终止脚本
    pause
    exit /b 1
)
echo ✅ Maven打包成功

:: ======================== 调试关键：查看真实路径和文件 ========================
echo 📂 target目录下的所有文件（Windows兼容语法）：
dir "%cd%\target\"  # 使用绝对路径，避免相对路径报错
echo ============================================================================
pause  # 按任意键继续，先看清target里的JAR文件名

echo ======================== 开始复制JAR文件 ========================
:: 自动匹配target下的唯一可执行JAR（跳过.original的普通JAR，Windows兼容）
set "source_jar="
for /f "delims=" %%i in ('dir "%cd%\target\*.jar" /b ^| findstr /v ".original"') do (
    set "source_jar=%cd%\target\%%i"
)

:: 检查是否找到可执行JAR
if not defined source_jar (
    echo ❌ 未找到可执行JAR文件！target目录下可能只有.original的普通JAR。
    echo 📌 请检查pom.xml是否配置了Spring Boot打包插件（repackage goal）！
    pause
    exit /b 1
)
echo 📌 找到待复制的可执行JAR：%source_jar%

:: 定义目标目录（Windows风格路径，适配你的目录结构）
set "target_dir=%cd%\..\..\cloud\"

:: 创建目标目录（如果不存在）
if not exist "%target_dir%" (
    mkdir "%target_dir%"
    echo 📁 创建目标目录：%target_dir%
)

:: 复制JAR文件到目标目录（/y 覆盖已有文件）
copy /y "%source_jar%" "%target_dir%"
if %errorlevel% equ 0 (
    echo ✅ JAR文件已复制到：%target_dir%
) else (
    echo ❌ JAR文件复制失败
    pause
    exit /b 1
)

echo ======================== 开始Git提交推送 ========================
:: 进入目标目录（确保Git操作路径正确）
cd /d "%target_dir%"

:: 获取JAR文件名（仅文件名，不含路径）
for %%i in ("%source_jar%") do set "jar_name=%%~nxi"

:: Git操作：添加文件 → 提交 → 推送
git add "%jar_name%"
git commit -m "更新"
git push

:: 检查Git操作是否成功
if %errorlevel% equ 0 (
    echo ✅ Git提交推送成功
) else (
    echo ❌ Git提交推送失败（请检查仓库连接/权限）
    pause
    exit /b 1
)

echo ======================== 全部操作完成 ========================
pause