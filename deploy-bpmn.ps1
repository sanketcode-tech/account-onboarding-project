# BPMN Deployment Script for Camunda 8 SaaS
# This script validates and deploys the BPMN file to Camunda 8 SaaS

param(
    [string]$BpmnFile = "onboarding-service/src/main/resources/bpmn/current-account-onboarding.bpmn",
    [string]$EnvFile = ".env",
    [switch]$SkipValidation = $false
)

# Load environment variables from .env file
function Load-EnvFile {
    param([string]$FilePath)

    if (-not (Test-Path $FilePath)) {
        Write-Host "❌ .env file not found: $FilePath" -ForegroundColor Red
        exit 1
    }

    Get-Content $FilePath | ForEach-Object {
        $line = $_.Trim()

        # Skip comments and empty lines
        if ($line.StartsWith("#") -or [string]::IsNullOrWhiteSpace($line)) {
            return
        }

        # Parse variable assignment
        if ($line -match "^([^=]+)=(.*)$") {
            $varName = $matches[1].Trim()
            $varValue = $matches[2].Trim()
            # Remove surrounding quotes if present
            $varValue = $varValue -replace '^"(.*)"$', '$1' -replace "^'(.*)'`$", '$1'

            Set-Item -Path "env:$varName" -Value $varValue
            Write-Host "  ✓ Loaded: $varName" -ForegroundColor Green
        }
    }
}

function Main {
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "BPMN Deployment Script - Camunda 8 SaaS" -ForegroundColor Cyan
    Write-Host "========================================`n" -ForegroundColor Cyan

    # Step 1: Load environment variables
    Write-Host "[Step 1] Loading environment variables from $EnvFile" -ForegroundColor Yellow
    Load-EnvFile $EnvFile

    # Step 2: Verify BPMN file exists
    Write-Host "`n[Step 2] Verifying BPMN file exists" -ForegroundColor Yellow
    $BpmnFullPath = (Get-Item $BpmnFile -ErrorAction SilentlyContinue).FullName

    if (-not (Test-Path $BpmnFile)) {
        Write-Host "❌ BPMN file not found: $BpmnFile" -ForegroundColor Red
        exit 1
    }

    $BpmnFileSize = (Get-Item $BpmnFile).Length
    Write-Host "  ✓ BPMN file found: $BpmnFullPath" -ForegroundColor Green
    Write-Host "  ✓ File size: $BpmnFileSize bytes" -ForegroundColor Green

    # Step 3: Display deployment configuration
    Write-Host "`n[Step 3] Deployment Configuration" -ForegroundColor Yellow
    Write-Host "  Cluster ID: $env:CAMUNDA_CLUSTER_ID" -ForegroundColor Cyan
    Write-Host "  Region: $env:CAMUNDA_REGION" -ForegroundColor Cyan
    Write-Host "  Client ID: $($env:CAMUNDA_CLIENT_ID.Substring(0, [Math]::Min(10, $env:CAMUNDA_CLIENT_ID.Length)))***" -ForegroundColor Cyan
    Write-Host "  Deployer Enabled: $env:PROCESS_DEPLOYER_ENABLED" -ForegroundColor Cyan

    # Step 4: Build the project
    Write-Host "`n[Step 4] Building the project" -ForegroundColor Yellow
    Write-Host "Running: mvn -q clean package -DskipTests -pl onboarding-service" -ForegroundColor Gray
    mvn -q clean package -DskipTests -pl onboarding-service

    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Build failed" -ForegroundColor Red
        exit 1
    }
    Write-Host "  ✓ Build successful" -ForegroundColor Green

    # Step 5: Validate BPMN (if not skipped)
    if (-not $SkipValidation) {
        Write-Host "`n[Step 5] Validating BPMN structure" -ForegroundColor Yellow
        Write-Host "Checking XML syntax and BPMN elements..." -ForegroundColor Gray

        try {
            $xml = New-Object System.Xml.XmlDocument
            $xml.Load($BpmnFullPath)
            Write-Host "  ✓ XML structure valid" -ForegroundColor Green

            # Check for required BPMN elements
            $xmlNs = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
            $xmlNs.AddNamespace("bpmn", "http://www.omg.org/spec/BPMN/20100524/MODEL")

            $processes = $xml.SelectNodes("//bpmn:process", $xmlNs)
            $startEvents = $xml.SelectNodes("//bpmn:startEvent", $xmlNs)
            $endEvents = $xml.SelectNodes("//bpmn:endEvent", $xmlNs)
            $tasks = $xml.SelectNodes("//bpmn:serviceTask | //bpmn:userTask", $xmlNs)

            Write-Host "  ✓ BPMN Elements Found:" -ForegroundColor Green
            Write-Host "    - Processes: $($processes.Count)" -ForegroundColor Green
            Write-Host "    - Start Events: $($startEvents.Count)" -ForegroundColor Green
            Write-Host "    - End Events: $($endEvents.Count)" -ForegroundColor Green
            Write-Host "    - Tasks (Service/User): $($tasks.Count)" -ForegroundColor Green

            if ($processes.Count -eq 0) {
                Write-Host "❌ No BPMN processes found in file" -ForegroundColor Red
                exit 1
            }

            if ($startEvents.Count -eq 0 -or $endEvents.Count -eq 0) {
                Write-Host "⚠ Warning: Process missing start or end events" -ForegroundColor Yellow
            }

        } catch {
            Write-Host "❌ BPMN validation failed: $_" -ForegroundColor Red
            exit 1
        }
    }

    # Step 6: Run deployment
    Write-Host "`n[Step 6] Deploying BPMN to Camunda 8 SaaS" -ForegroundColor Yellow

    $jarPath = "onboarding-service/target/onboarding-service-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path $jarPath)) {
        Write-Host "❌ JAR file not found: $jarPath" -ForegroundColor Red
        exit 1
    }

    Write-Host "Launching deployment tool..." -ForegroundColor Gray
    java -cp "$jarPath;onboarding-service/target/lib/*" `
        com.northbridge.onboarding.util.BPMNDeploymentTool `
        --file $BpmnFullPath `
        --cluster-id $env:CAMUNDA_CLUSTER_ID `
        --region $env:CAMUNDA_REGION `
        --client-id $env:CAMUNDA_CLIENT_ID `
        --client-secret $env:CAMUNDA_CLIENT_SECRET

    if ($LASTEXITCODE -ne 0) {
        Write-Host "`n❌ Deployment failed" -ForegroundColor Red
        exit 1
    }

    Write-Host "`n✓ Deployment completed successfully!" -ForegroundColor Green
    Write-Host "========================================`n" -ForegroundColor Cyan
}

# Run main function
Main

