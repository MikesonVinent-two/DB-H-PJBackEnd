@echo off
setlocal enabledelayedexpansion

REM 数据库迁移执行脚本
REM 作者：AI助手
REM 创建日期：2025-06-02

REM 设置数据库连接参数
set DB_HOST=localhost
set DB_PORT=3306
set DB_NAME=your_database_name
set DB_USER=your_username
set DB_PASS=your_password

REM 颜色定义
set RED=[91m
set GREEN=[92m
set YELLOW=[93m
set NC=[0m

REM 解析命令行参数
:parse_args
if "%~1"=="" goto :check_params
if /i "%~1"=="-h" (
    set DB_HOST=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="--host" (
    set DB_HOST=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="-P" (
    set DB_PORT=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="--port" (
    set DB_PORT=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="-d" (
    set DB_NAME=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="--database" (
    set DB_NAME=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="-u" (
    set DB_USER=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="--user" (
    set DB_USER=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="-p" (
    set DB_PASS=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="--password" (
    set DB_PASS=%~2
    shift
    shift
    goto :parse_args
)
if /i "%~1"=="--help" (
    call :show_help
    exit /b 0
)
echo %RED%错误: 未知选项 %~1%NC%
call :show_help
exit /b 1

:check_params
REM 检查必填参数
if "%DB_NAME%"=="your_database_name" (
    echo %RED%错误: 请指定数据库名称 (-d 或 --database)%NC%
    call :show_help
    exit /b 1
)

if "%DB_USER%"=="your_username" (
    echo %RED%错误: 请指定数据库用户名 (-u 或 --user)%NC%
    call :show_help
    exit /b 1
)

if "%DB_PASS%"=="your_password" (
    echo %RED%错误: 请指定数据库密码 (-p 或 --password)%NC%
    call :show_help
    exit /b 1
)

REM 迁移脚本目录
set MIGRATIONS_DIR=.\migrations

REM 检查迁移脚本目录是否存在
if not exist "%MIGRATIONS_DIR%" (
    echo %RED%错误: 迁移脚本目录不存在: %MIGRATIONS_DIR%%NC%
    exit /b 1
)

REM 获取所有迁移脚本
set "MIGRATION_FILES="
for /f "tokens=*" %%a in ('dir /b /o:n "%MIGRATIONS_DIR%\V*__*.sql" 2^>nul') do (
    set "MIGRATION_FILES=!MIGRATION_FILES! "%%a""
)

if "%MIGRATION_FILES%"=="" (
    echo %YELLOW%警告: 没有找到迁移脚本%NC%
    exit /b 0
)

REM 执行迁移脚本
echo %GREEN%开始执行数据库迁移...%NC%
echo 数据库: %DB_NAME%
echo 主机: %DB_HOST%:%DB_PORT%
echo 用户: %DB_USER%
echo.

for %%f in (%MIGRATION_FILES%) do (
    echo %YELLOW%执行迁移脚本: %%f%NC%
    
    REM 执行SQL脚本
    mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% %DB_NAME% < "%MIGRATIONS_DIR%\%%f"
    
    if !errorlevel! equ 0 (
        echo %GREEN%✓ 迁移成功%NC%
    ) else (
        echo %RED%✗ 迁移失败%NC%
        exit /b 1
    )
    
    echo.
)

echo %GREEN%所有迁移脚本执行完成!%NC%
exit /b 0

:show_help
echo 数据库迁移执行脚本
echo 用法: %~nx0 [options]
echo.
echo 选项:
echo   -h, --host      数据库主机 (默认: localhost)
echo   -P, --port      数据库端口 (默认: 3306)
echo   -d, --database  数据库名称 (必填)
echo   -u, --user      数据库用户名 (必填)
echo   -p, --password  数据库密码 (必填)
echo   --help          显示此帮助信息
echo.
exit /b 