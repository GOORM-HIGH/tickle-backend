#!/bin/bash

# 채팅/파일/웹소켓 성능 테스트 실행 스크립트

echo "🚀 채팅/파일/웹소켓 성능 테스트 시작"
echo "=================================="

# K6 설치 확인
if ! command -v k6 &> /dev/null; then
    echo "❌ K6가 설치되지 않았습니다. 먼저 K6를 설치해주세요."
    echo "   macOS: brew install k6"
    echo "   또는: https://k6.io/docs/getting-started/installation/"
    exit 1
fi

# Spring Boot 애플리케이션 상태 확인
echo "🔍 Spring Boot 애플리케이션 상태 확인..."
if ! curl -s http://localhost:8081/actuator/health > /dev/null; then
    echo "❌ Spring Boot 애플리케이션이 실행되지 않았습니다."
    echo "   먼저 ./gradlew bootRun으로 애플리케이션을 실행해주세요."
    exit 1
fi
echo "✅ Spring Boot 애플리케이션 실행 중"

# 결과 디렉토리 생성
mkdir -p results

echo ""
echo "📊 테스트 1: WebSocket 채팅 테스트"
echo "--------------------------------"
k6 run --out json=results/chat-websocket-results.json chat-websocket-test.js

echo ""
echo "📊 테스트 2: 파일 업로드 테스트"
echo "----------------------------"
k6 run --out json=results/file-upload-results.json file-upload-test.js

echo ""
echo "📊 테스트 3: 혼합 부하 테스트"
echo "--------------------------"
k6 run --out json=results/mixed-load-results.json mixed-load-test.js

echo ""
echo "📈 테스트 결과 요약"
echo "=================="
echo "결과 파일들이 results/ 디렉토리에 저장되었습니다:"
ls -la results/

echo ""
echo "🎯 Prometheus 메트릭 확인"
echo "========================"
echo "Spring Boot 메트릭: http://localhost:8081/actuator/prometheus"
echo "Prometheus UI: http://localhost:9090"
echo "Grafana UI: http://localhost:3000"

echo ""
echo "✅ 모든 테스트가 완료되었습니다!"
