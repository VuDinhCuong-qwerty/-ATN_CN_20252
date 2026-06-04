@echo off
REM ============================================================
REM  dev-stop.cmd -- Tat tung ung dung roi dong tat ca terminal
REM  Dung /t de kill ca tien trinh con (java, node, mvn...)
REM ============================================================

echo === IAM Dev -- Dang tat tat ca services... ===
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
echo Tat ca 8 services da duoc tat.
echo Doi 3 giay roi tu dong dong cua so nay...
timeout /t 3 /nobreak >nul
exit

REM ── Kill process dang lang nghe tren port chi dinh ──────────────────────────
:killport
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%~1 " ^| findstr "LISTENING" 2^>nul') do (
    taskkill /f /t /pid %%p >nul 2>&1
)
exit /b
