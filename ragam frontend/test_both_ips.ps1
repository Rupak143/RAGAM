# Test Both IP Addresses
# This script tests connectivity to both configured IP addresses

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   Testing Both IP Addresses" -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

$ip1 = "172.20.10.3"
$ip2 = "192.168.1.10"
$testEndpoint = "/ragamfinal/auth.php?action=login"
$testBody = '{"email":"rupak@gmail.com","password":"12345678"}'

$results = @()

# Test IP 1
Write-Host "[Testing IP 1: $ip1]" -ForegroundColor Green
Write-Host "URL: http://$ip1$testEndpoint"
try {
    $response1 = Invoke-WebRequest -Uri "http://$ip1$testEndpoint" `
        -Method POST `
        -ContentType "application/json" `
        -Body $testBody `
        -TimeoutSec 10 `
        -UseBasicParsing -ErrorAction Stop
    
    $statusCode = $response1.StatusCode
    $content = $response1.Content
    
    Write-Host "  ✓ Status Code: $statusCode" -ForegroundColor Green
    Write-Host "  ✓ Response Length: $($content.Length) bytes" -ForegroundColor Green
    
    # Check for BOM
    if ($content -match "^[ï»¿]") {
        Write-Host "  ⚠ WARNING: BOM detected in response!" -ForegroundColor Yellow
        $results += "IP1: PASS (but has BOM)"
    } else {
        Write-Host "  ✓ No BOM detected" -ForegroundColor Green
        $results += "IP1: PASS"
    }
    
    # Try to parse JSON
    try {
        $json = $content | ConvertFrom-Json
        if ($json.success) {
            Write-Host "  ✓ Login endpoint working" -ForegroundColor Green
        } else {
            Write-Host "  ⚠ Login failed: $($json.message)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  ✗ JSON parsing failed" -ForegroundColor Red
    }
    
} catch {
    Write-Host "  ✗ Connection FAILED" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
    $results += "IP1: FAIL"
}

Write-Host ""

# Test IP 2
Write-Host "[Testing IP 2: $ip2]" -ForegroundColor Green
Write-Host "URL: http://$ip2$testEndpoint"
try {
    $response2 = Invoke-WebRequest -Uri "http://$ip2$testEndpoint" `
        -Method POST `
        -ContentType "application/json" `
        -Body $testBody `
        -TimeoutSec 10 `
        -UseBasicParsing -ErrorAction Stop
    
    $statusCode = $response2.StatusCode
    $content = $response2.Content
    
    Write-Host "  ✓ Status Code: $statusCode" -ForegroundColor Green
    Write-Host "  ✓ Response Length: $($content.Length) bytes" -ForegroundColor Green
    
    # Check for BOM
    if ($content -match "^[ï»¿]") {
        Write-Host "  ⚠ WARNING: BOM detected in response!" -ForegroundColor Yellow
        $results += "IP2: PASS (but has BOM)"
    } else {
        Write-Host "  ✓ No BOM detected" -ForegroundColor Green
        $results += "IP2: PASS"
    }
    
    # Try to parse JSON
    try {
        $json = $content | ConvertFrom-Json
        if ($json.success) {
            Write-Host "  ✓ Login endpoint working" -ForegroundColor Green
        } else {
            Write-Host "  ⚠ Login failed: $($json.message)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  ✗ JSON parsing failed" -ForegroundColor Red
    }
    
} catch {
    Write-Host "  ✗ Connection FAILED" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
    $results += "IP2: FAIL"
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   Test Summary" -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Cyan

foreach ($result in $results) {
    if ($result -like "*PASS*") {
        Write-Host "  $result" -ForegroundColor Green
    } else {
        Write-Host "  $result" -ForegroundColor Red
    }
}

Write-Host ""

# Recommendation
$ip1Pass = $results[0] -like "*PASS*"
$ip2Pass = $results[1] -like "*PASS*"

if ($ip1Pass -and $ip2Pass) {
    Write-Host "✅ Both IPs are working! You can use either one." -ForegroundColor Green
    Write-Host "   Recommended: Keep IP1 ($ip1) as default" -ForegroundColor Cyan
} elseif ($ip1Pass) {
    Write-Host "✅ IP1 ($ip1) is working" -ForegroundColor Green
    Write-Host "⚠️  IP2 ($ip2) is not accessible" -ForegroundColor Yellow
    Write-Host "   Use IP1 for now" -ForegroundColor Cyan
} elseif ($ip2Pass) {
    Write-Host "⚠️  IP1 ($ip1) is not accessible" -ForegroundColor Yellow
    Write-Host "✅ IP2 ($ip2) is working" -ForegroundColor Green
    Write-Host "   Switch to IP2 in AppConfig.java" -ForegroundColor Cyan
} else {
    Write-Host "❌ Both IPs failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Troubleshooting steps:" -ForegroundColor Yellow
    Write-Host "  1. Check if XAMPP Apache is running" -ForegroundColor White
    Write-Host "  2. Verify your laptop's IP address:" -ForegroundColor White
    Write-Host "     Run: ipconfig" -ForegroundColor Cyan
    Write-Host "  3. Ensure mobile and laptop are on same network" -ForegroundColor White
    Write-Host "  4. Check firewall settings" -ForegroundColor White
    Write-Host "  5. Test in browser: http://YOUR_IP/ragamfinal/test.php" -ForegroundColor White
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
