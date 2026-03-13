# Update PHP files in XAMPP htdocs with port 3306 configuration
# Date: October 16, 2025

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "Updating PHP Files in XAMPP htdocs" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""

$htdocsPath = "C:\xampp\htdocs\ragamfinal"
$projectBackendPath = "C:\Users\HP\Documents\New Folder\Ragamfinal\Ragamfinal\backend_web"

# Check if htdocs directory exists
if (-not (Test-Path $htdocsPath)) {
    Write-Host "❌ Error: Directory not found: $htdocsPath" -ForegroundColor Red
    Write-Host "Please make sure XAMPP is installed and the ragamfinal folder exists." -ForegroundColor Yellow
    exit 1
}

Write-Host "✓ Found htdocs directory: $htdocsPath" -ForegroundColor Green
Write-Host ""

# Backup current config.php
Write-Host "[1/4] Backing up current config.php..." -ForegroundColor Yellow
$backupFile = "$htdocsPath\config_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').php"
if (Test-Path "$htdocsPath\config.php") {
    Copy-Item "$htdocsPath\config.php" $backupFile
    Write-Host "✓ Backup created: $backupFile" -ForegroundColor Green
} else {
    Write-Host "⚠ No existing config.php found" -ForegroundColor Yellow
}
Write-Host ""

# Update config.php with port 3306
Write-Host "[2/4] Updating config.php with port 3306..." -ForegroundColor Yellow

$configContent = @'
<?php
/**
 * Database Configuration for Ragam Final Music App
 * Updated: October 16, 2025
 * MySQL Port: 3306
 * IP Address: 10.17.207.64
 */

class DatabaseConfig {
    // Database credentials
    private $host = "localhost";
    private $port = "3306";
    private $username = "root";
    private $password = "";
    private $database = "ragamfinal";
    private $connection;
    
    // Get database connection
    public function getConnection() {
        $this->connection = null;
        
        try {
            $this->connection = new PDO(
                "mysql:host=" . $this->host . ";port=" . $this->port . ";dbname=" . $this->database,
                $this->username,
                $this->password
            );
            $this->connection->exec("set names utf8");
            $this->connection->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
            
            // Test the connection
            $stmt = $this->connection->query("SELECT 1");
            if (!$stmt) {
                error_log("Database connection test failed");
                return null;
            }
            
        } catch(PDOException $exception) {
            // Log error with more details
            error_log("Database connection error: " . $exception->getMessage());
            error_log("Database host: " . $this->host);
            error_log("Database name: " . $this->database);
            return null;
        }
        
        return $this->connection;
    }
}

// Response helper functions
class APIResponse {
    public static function success($data = null, $message = "Success") {
        header('Content-Type: application/json');
        echo json_encode([
            'success' => true,
            'message' => $message,
            'data' => $data
        ]);
    }
    
    public static function error($message = "Error occurred", $code = 400) {
        header('Content-Type: application/json');
        http_response_code($code);
        echo json_encode([
            'success' => false,
            'message' => $message,
            'data' => null
        ]);
    }
}
?>
'@

Set-Content -Path "$htdocsPath\config.php" -Value $configContent -Encoding UTF8
Write-Host "✓ config.php updated successfully" -ForegroundColor Green
Write-Host ""

# Copy other PHP files if they exist in project
Write-Host "[3/4] Copying PHP files from project backend..." -ForegroundColor Yellow

$filesToCopy = @(
    "courses.php",
    "profile.php",
    "upload_video.php"
)

$copiedCount = 0
foreach ($file in $filesToCopy) {
    $sourcePath = "$projectBackendPath\$file"
    if (Test-Path $sourcePath) {
        Copy-Item $sourcePath "$htdocsPath\$file" -Force
        Write-Host "  ✓ Copied: $file" -ForegroundColor Green
        $copiedCount++
    } else {
        Write-Host "  ⚠ Not found: $file" -ForegroundColor Yellow
    }
}

Write-Host "✓ Copied $copiedCount file(s) from project backend" -ForegroundColor Green
Write-Host ""

# Test the database connection
Write-Host "[4/4] Testing database connection..." -ForegroundColor Yellow

$testScript = @'
<?php
require_once 'config.php';

$db = new DatabaseConfig();
$conn = $db->getConnection();

if ($conn) {
    echo "SUCCESS: Connected to MySQL on port 3306\n";
    echo "Database: ragamfinal\n";
    
    // Test query
    $stmt = $conn->query("SELECT DATABASE() as db_name");
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    echo "Current Database: " . $result['db_name'] . "\n";
} else {
    echo "FAILED: Could not connect to database\n";
    exit(1);
}
?>
'@

$testFile = "$htdocsPath\test_connection_port3306.php"
Set-Content -Path $testFile -Value $testScript -Encoding UTF8

# Run the test via PHP CLI if available
try {
    $phpPath = "C:\xampp\php\php.exe"
    if (Test-Path $phpPath) {
        Write-Host "Running connection test..." -ForegroundColor Cyan
        $testResult = & $phpPath $testFile 2>&1
        Write-Host $testResult -ForegroundColor White
    } else {
        Write-Host "⚠ PHP CLI not found. Test manually at: http://localhost/ragamfinal/test_connection_port3306.php" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ Could not run test. Access manually at: http://localhost/ragamfinal/test_connection_port3306.php" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "✅ UPDATE COMPLETE" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Summary:" -ForegroundColor White
Write-Host "  • Updated config.php with port 3306" -ForegroundColor White
Write-Host "  • Backup created: $(Split-Path $backupFile -Leaf)" -ForegroundColor White
Write-Host "  • Copied $copiedCount PHP file(s)" -ForegroundColor White
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Make sure MySQL is running on port 3306" -ForegroundColor White
Write-Host "  2. Test connection: http://localhost/ragamfinal/test_connection_port3306.php" -ForegroundColor White
Write-Host "  3. Test your app with the new IP: 10.17.207.64" -ForegroundColor White
Write-Host ""
