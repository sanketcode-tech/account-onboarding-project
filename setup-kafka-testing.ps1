# Quick Start Script for Testing Kafka Producer/Consumer
# Run this from PowerShell in the onboarding directory

Write-Host "================================" -ForegroundColor Green
Write-Host "Application Service Kafka Setup" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green
Write-Host ""

# Check prerequisites
Write-Host "[1] Checking Prerequisites..." -ForegroundColor Cyan

# Check Java
try {
    $javaVersion = java -version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Java is installed" -ForegroundColor Green
    } else {
        Write-Host "✗ Java is NOT installed. Please install Java 21+" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "✗ Java is NOT installed" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[2] Build Application Service..." -ForegroundColor Cyan
cd application-service
.\mvnw -DskipTests clean package
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Build successful" -ForegroundColor Green
} else {
    Write-Host "✗ Build failed" -ForegroundColor Red
    exit 1
}
cd ..

Write-Host ""
Write-Host "[3] Configuration" -ForegroundColor Cyan
Write-Host "Set environment variables (optional):" -ForegroundColor Yellow
Write-Host ""
Write-Host "  # Kafka bootstrap servers (default: localhost:9092)" -ForegroundColor Gray
Write-Host "`$env:SPRING_KAFKA_BOOTSTRAP_SERVERS = 'localhost:9092'" -ForegroundColor White
Write-Host ""
Write-Host "  # Application port (default: 8082)" -ForegroundColor Gray
Write-Host "`$env:SERVER_PORT_APPLICATION = '8082'" -ForegroundColor White
Write-Host ""

Write-Host "[4] Start the Application Service" -ForegroundColor Cyan
Write-Host ""
Write-Host "Run in a new PowerShell terminal:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  cd $pwd\application-service" -ForegroundColor White
Write-Host "  java -Dspring.kafka.bootstrap-servers=localhost:9092 -jar target\application-service-0.0.1-SNAPSHOT.jar" -ForegroundColor White
Write-Host ""

Write-Host "[5] Start Kafka Consumer" -ForegroundColor Cyan
Write-Host ""
Write-Host "Run in a new PowerShell terminal:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  wsl -d UbuntuUser bash -c `"cd /home/dev/kafka && bin/kafka-console-consumer.sh --topic application.submitted --bootstrap-server localhost:9092 --from-beginning`"" -ForegroundColor White
Write-Host ""

Write-Host "[6] Test the API" -ForegroundColor Cyan
Write-Host ""
Write-Host "From another PowerShell terminal, run these curl commands:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  # Check health" -ForegroundColor Gray
Write-Host "  curl http://localhost:8082/api/v1/applications/health" -ForegroundColor White
Write-Host ""
Write-Host "  # Send simple test message" -ForegroundColor Gray
Write-Host "  curl -X POST http://localhost:8082/api/v1/applications/test-send" -ForegroundColor White
Write-Host ""
Write-Host "  # Send with custom values" -ForegroundColor Gray
Write-Host "  curl -X POST `"http://localhost:8082/api/v1/applications/test-send?applicationId=APP-001&applicantName=John%20Doe`"" -ForegroundColor White
Write-Host ""
Write-Host "  # Submit application (JSON)" -ForegroundColor Gray
Write-Host "  curl -X POST http://localhost:8082/api/v1/applications/submit `"" -ForegroundColor White
Write-Host "       -H `"Content-Type: application/json`" `"" -ForegroundColor White
Write-Host "       -d '{`"applicationId`":`"APP-2026-001`",`"applicantName`":`"John Doe`",`"email`":`"john.doe@example.com`",`"status`":`"SUBMITTED`",`"eventType`":`"APPLICATION_SUBMITTED`"}'" -ForegroundColor White
Write-Host ""

Write-Host "================================" -ForegroundColor Green
Write-Host "Setup Complete!" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green
Write-Host ""
Write-Host "For detailed instructions, see: APPLICATION_KAFKA_TESTING_GUIDE.md" -ForegroundColor Cyan
