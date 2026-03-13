# Quick Rebuild and Install Script
# Default IP: 192.168.1.10 (WiFi Network)
# Run this after fixing issues to rebuild and install the app

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   RAGAM FINAL - Rebuild and Install" -ForegroundColor Yellow
Write-Host "   Default IP: 192.168.1.10 (WiFi Network)" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

$projectPath = "C:\Users\91917\StudioProjects\Ragamfinal"

Write-Host "[Step 1/3] Cleaning build..." -ForegroundColor Green
Set-Location $projectPath
.\gradlew clean

Write-Host ""
Write-Host "[Step 2/3] Building APK..." -ForegroundColor Green
.\gradlew assembleDebug

Write-Host ""
Write-Host "[Step 3/3] Installing on device..." -ForegroundColor Green
.\gradlew installDebug

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   ✅ Build and Install Complete!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Default IP: 192.168.1.10 (WiFi Network)" -ForegroundColor Green
Write-Host "  Base URL: http://192.168.1.10/ragamfinal/" -ForegroundColor Cyan
Write-Host ""
Write-Host "Test the app with:" -ForegroundColor Yellow
Write-Host "  Student: rupak@gmail.com / 12345678" -ForegroundColor Cyan
Write-Host "  Teacher: ram@gmail.com / 12345678" -ForegroundColor Cyan
Write-Host ""
Write-Host "Switch IPs without rebuilding:" -ForegroundColor Yellow
Write-Host "  Open app → Network Settings → Select IP → Save" -ForegroundColor White
Write-Host ""
