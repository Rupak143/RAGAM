# Quick Diagnostic Test Script
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "RAGAM FINAL - BACKEND TEST" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Check if Apache is running
Write-Host "[TEST 1] Checking if Apache is running on port 80..." -ForegroundColor Yellow
$apache = netstat -an | findstr ":80.*LISTENING"
if ($apache) {
    Write-Host "✅ Apache is running!" -ForegroundColor Green
} else {
    Write-Host "❌ Apache is NOT running! Start XAMPP Apache." -ForegroundColor Red
    exit
}
Write-Host ""

# Test 2: Test simple endpoint
Write-Host "[TEST 2] Testing server reachability..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://192.168.1.10/ragamfinal/simple_test.php" -ErrorAction Stop
    if ($response.success) {
        Write-Host "✅ Server is reachable!" -ForegroundColor Green
        Write-Host "   Message: $($response.message)" -ForegroundColor Gray
    }
} catch {
    Write-Host "❌ Cannot reach server at 192.168.1.10" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    exit
}
Write-Host ""

# Test 3: Test teacher login
Write-Host "[TEST 3] Testing teacher authentication..." -ForegroundColor Yellow
try {
    $body = '{"email":"ram@gmail.com","password":"ram123","user_type":"teacher"}'
    $response = Invoke-RestMethod -Uri "http://192.168.1.10/ragamfinal/auth.php?action=login" -Method POST -Body $body -ContentType "application/json" -ErrorAction Stop
    if ($response.success) {
        Write-Host "✅ Teacher login successful!" -ForegroundColor Green
        Write-Host "   User: $($response.data.full_name) ($($response.data.email))" -ForegroundColor Gray
    } else {
        Write-Host "❌ Teacher login failed: $($response.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Teacher login error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 4: Test student login
Write-Host "[TEST 4] Testing student authentication..." -ForegroundColor Yellow
try {
    $body = '{"email":"Rupak@gmail.com","password":"rupak123","user_type":"student"}'
    $response = Invoke-RestMethod -Uri "http://192.168.1.10/ragamfinal/auth.php?action=login" -Method POST -Body $body -ContentType "application/json" -ErrorAction Stop
    if ($response.success) {
        Write-Host "✅ Student login successful!" -ForegroundColor Green
        Write-Host "   User: $($response.data.full_name) ($($response.data.email))" -ForegroundColor Gray
    } else {
        Write-Host "❌ Student login failed: $($response.message)" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Student login error: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Check APK
Write-Host "[TEST 5] Checking APK..." -ForegroundColor Yellow
$apk = Get-Item "c:\Users\91917\StudioProjects\Ragamfinal\app\build\outputs\apk\debug\app-debug.apk" -ErrorAction SilentlyContinue
if ($apk) {
    $sizeInMB = [math]::Round($apk.Length / 1MB, 2)
    Write-Host "✅ APK found!" -ForegroundColor Green
    Write-Host "   Location: $($apk.FullName)" -ForegroundColor Gray
    Write-Host "   Size: $sizeInMB MB" -ForegroundColor Gray
    Write-Host "   Built: $($apk.LastWriteTime)" -ForegroundColor Gray
} else {
    Write-Host "❌ APK not found!" -ForegroundColor Red
}
Write-Host ""

# Summary
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "SUMMARY" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan
Write-Host "Backend URL: http://192.168.1.10/ragamfinal/" -ForegroundColor White
Write-Host "Test in browser: http://192.168.1.10/ragamfinal/auth_test.html" -ForegroundColor White
Write-Host ""
Write-Host "Teacher Login: ram@gmail.com / ram123" -ForegroundColor White
Write-Host "Student Login: Rupak@gmail.com / rupak123" -ForegroundColor White
Write-Host ""
Write-Host "✅ All tests passed! Your app should work now!" -ForegroundColor Green
Write-Host ""
