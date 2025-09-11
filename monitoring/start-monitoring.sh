#!/bin/bash

# 채팅/파일/웹소켓 모니터링 환경 시작 스크립트

echo "🚀 채팅/파일/웹소켓 모니터링 환경 시작"
echo "====================================="

# Docker 설치 확인
if ! command -v docker &> /dev/null; then
    echo "❌ Docker가 설치되지 않았습니다. 먼저 Docker를 설치해주세요."
    exit 1
fi

# Docker Compose 설치 확인
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose가 설치되지 않았습니다. 먼저 Docker Compose를 설치해주세요."
    exit 1
fi

# 기존 컨테이너 정리
echo "🧹 기존 모니터링 컨테이너 정리..."
docker-compose down

# 모니터링 환경 시작
echo "📊 Prometheus & Grafana 시작..."
docker-compose up -d

# 컨테이너 상태 확인
echo "🔍 컨테이너 상태 확인..."
sleep 5
docker-compose ps

echo ""
echo "✅ 모니터링 환경이 시작되었습니다!"
echo ""
echo "📈 접속 정보:"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin123)"
echo "  - Spring Boot 메트릭: http://localhost:8081/actuator/prometheus"
echo ""
echo "🎯 주요 메트릭:"
echo "  - WebSocket 연결 수: websocket_active_sessions"
echo "  - 채팅 메시지 수: chat_messages_created_total"
echo "  - 파일 업로드 수: file_uploads_total"
echo "  - JWT 검증 시간: jwt_validation_time"
echo "  - S3 API 호출 수: s3_api_calls_total"
