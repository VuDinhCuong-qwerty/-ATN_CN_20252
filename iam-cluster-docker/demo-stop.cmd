@echo off
echo Dung tat ca services demo...

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
  down

echo Xong!
pause
