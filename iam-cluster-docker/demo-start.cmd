@echo off
echo ============================================
echo   Go-Live Management Portal - Demo Start
echo ============================================
echo.
echo Khoi dong: Redis + Kafka + IAM Services + Change App
echo.

docker compose ^
  -f docker-compose.yml ^
  -f docker-compose.redis.yml ^
  -f docker-compose.kafka.yml ^
  -f docker-compose.kafka-ui.yml ^
  -f docker-compose.iam-auth-service.yml ^
  -f docker-compose.iam-identity-service.yml ^
  -f docker-compose.iam-app-service.yml ^
  -f docker-compose.iam-gateway.yml ^
  -f docker-compose.iam-notify-service.yml ^
  -f docker-compose.demo-change-app.yml ^
  -f docker-compose.ldap-server.yml ^
  -f docker-compose.kibana.yml ^
  -f docker-compose.fluent-bit.yml ^
  up -d

echo.
echo ============================================
echo   Services dang chay:
echo   - IAM Auth:     http://localhost:8888
echo   - IAM Gateway:  http://localhost:8080
echo   - Change App:   http://localhost:8085
echo   - Kafka UI:     http://localhost:8090
echo   - Kibana:       http://localhost:5601
echo   - LDAP:         localhost:10389
echo ============================================
pause
