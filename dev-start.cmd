@echo off
REM ============================================================
REM  dev-start.cmd -- Khoi dong tat ca services cho moi truong dev
REM  Yeu cau: Oracle DB, Redis, Kafka da chay truoc khi chay file nay
REM ============================================================

set BASE=%~dp0

echo === IAM Dev Environment ===
echo.

echo [1/8] iam-auth-service     (port 8888)
start "iam-auth-service" /d "%BASE%iam-auth-service" cmd /k "mvn spring-boot:run"

echo [2/8] iam-identity-service (port 8081)
start "iam-identity-service" /d "%BASE%iam-identity-service" cmd /k "mvn spring-boot:run"

echo [3/8] iam-app-service      (port 8082)
start "iam-app-service" /d "%BASE%iam-app-service" cmd /k "mvn spring-boot:run"

REM [--] iam-notify-service (port 8083) -- tam disable
REM start "iam-notify-service" /d "%BASE%iam-notify-service" cmd /k "mvn spring-boot:run"

echo [4/8] iam-gateway          (port 8080)
start "iam-gateway" /d "%BASE%iam-gateway" cmd /k "mvn spring-boot:run"

echo [5/8] ldap-server          (port 10389)
start "ldap-server" /d "%BASE%ldap-server" cmd /k "mvn spring-boot:run"

echo [6/8] iam-web-service      (Angular, port 4200)
start "iam-web-service" /d "%BASE%iam-web-service" cmd /k "ng serve"

echo [7/8] change-app-service   (port 8085)
start "change-app-service" /d "%BASE%demo-change-app\change-app-service" cmd /k "mvn spring-boot:run"

echo [8/8] change-web-service   (Angular, port 4201)
start "change-web-service" /d "%BASE%demo-change-app\change-web-service" cmd /k "ng serve"

echo.
echo Tat ca 8 terminal da duoc mo.
echo Notify service bi comment -- bo REM de bat lai.
echo.
echo *** Nhan phim bat ky de mo hop xac nhan tat he thong ***
pause >nul

echo.
set /p CONFIRM=Ban co muon tat toan bo he thong khong? (y/n):
if /i not "%CONFIRM%"=="y" (
    echo Huy bo. He thong van dang chay.
    pause >nul
    exit
)

echo.
echo === Dang tat tat ca services... ===
echo.

echo [1/8] iam-auth-service     (port 8888)...
call :killport 8888
taskkill /fi "WINDOWTITLE eq iam-auth-service"     /f /t >nul 2>&1

echo [2/8] iam-identity-service (port 8081)...
call :killport 8081
taskkill /fi "WINDOWTITLE eq iam-identity-service" /f /t >nul 2>&1

echo [3/8] iam-app-service      (port 8082)...
call :killport 8082
taskkill /fi "WINDOWTITLE eq iam-app-service"      /f /t >nul 2>&1

echo [4/8] iam-gateway          (port 8080)...
call :killport 8080
taskkill /fi "WINDOWTITLE eq iam-gateway"          /f /t >nul 2>&1

echo [5/8] ldap-server          (port 10389)...
call :killport 10389
taskkill /fi "WINDOWTITLE eq ldap-server"          /f /t >nul 2>&1

echo [6/8] iam-web-service      (port 4200)...
call :killport 4200
taskkill /fi "WINDOWTITLE eq iam-web-service"      /f /t >nul 2>&1

echo [7/8] change-app-service   (port 8085)...
call :killport 8085
taskkill /fi "WINDOWTITLE eq change-app-service"   /f /t >nul 2>&1

echo [8/8] change-web-service   (port 4201)...
call :killport 4201
taskkill /fi "WINDOWTITLE eq change-web-service"   /f /t >nul 2>&1

echo.
echo Tat ca services da dung. Dong cua so sau 3 giay...
timeout /t 3 /nobreak >nul
exit

REM ── Kill process dang lang nghe tren port chi dinh ──────────────────────────
:killport
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%~1 " ^| findstr "LISTENING" 2^>nul') do (
    taskkill /f /t /pid %%p >nul 2>&1
)
exit /b
