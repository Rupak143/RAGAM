# Quick Test - 192.168.1.10 Backend
# Tests the new default IP address

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   Testing Backend: 192.168.1.10" -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

$ip = "192.168.1.10"
$baseUrl = "http://$ip/ragamfinal"

# Test 1: Auth endpoint
Write-Host "[Test 1] Authentication Endpoint" -ForegroundColor Green
Write-Host "URL: $baseUrl/auth.php" -ForegroundColor Cyan

try {
    $body = '{"email":"rupak@gmail.com","password":"12345678"}'
    $response = Invoke-WebRequest -Uri "$baseUrl/auth.php?action=login" `
        -Method POST -ContentType "application/json" -Body $body `
        -TimeoutSec 10 -UseBasicParsing
    
    Write-Host "  ✓ Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "  ✓ Response: $($response.Content.Length) bytes" -ForegroundColor Green
    
    $json = $response.Content | ConvertFrom-Json
    if ($json.success) {
        Write-Host "  ✓ Login: SUCCESS" -ForegroundColor Green
        Write-Host "  ✓ User ID: $($json.user_id)" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Login failed: $($json.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "  ✗ FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 2: Courses endpoint
Write-Host "[Test 2] Courses Endpoint" -ForegroundColor Green
Write-Host "URL: $baseUrl/courses.php" -ForegroundColor Cyan

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/courses.php?action=get_all_courses" `
        -Method GET -TimeoutSec 10 -UseBasicParsing
    
    Write-Host "  ✓ Status: $($response.StatusCode)" -ForegroundColor Green
    
    $json = $response.Content | ConvertFrom-Json
    if ($json.success) {
        $count = $json.courses.Count
        Write-Host "  ✓ Courses loaded: $count courses" -ForegroundColor Green
    }
}
catch {
    Write-Host "  ✗ FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# Test 3: Profile endpoint
Write-Host "[Test 3] Profile Endpoint" -ForegroundColor Green
Write-Host "URL: $baseUrl/profile.php" -ForegroundColor Cyan

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/profile.php?action=get_profile&user_id=9" `
        -Method GET -TimeoutSec 10 -UseBasicParsing
    
    Write-Host "  ✓ Status: $($response.StatusCode)" -ForegroundColor Green
    
    $json = $response.Content | ConvertFrom-Json
    if ($json.success) {
        Write-Host "  ✓ Profile loaded: $($json.profile.name)" -ForegroundColor Green
    }
}
catch {
    Write-Host "  ✗ FAILED: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   Summary" -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  IP Address: $ip" -ForegroundColor Green
Write-Host "  Base URL: $baseUrl/" -ForegroundColor Cyan
Write-Host ""
Write-Host "If all tests passed, you're ready to rebuild the app:" -ForegroundColor White
Write-Host "  .\rebuild_and_install.ps1" -ForegroundColor Green
Write-Host ""
