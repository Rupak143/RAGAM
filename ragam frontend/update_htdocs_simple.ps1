# Update PHP config.php in XAMPP htdocs with port 3306
# Date: October 16, 2025

Write-Host "Updating PHP Files in XAMPP htdocs" -ForegroundColor Cyan
Write-Host ""

$htdocsPath = "C:\xampp\htdocs\ragamfinal"

# Check if htdocs directory exists
if (-not (Test-Path $htdocsPath)) {
    Write-Host "Error: Directory not found: $htdocsPath" -ForegroundColor Red
    exit 1
}

Write-Host "Found htdocs directory: $htdocsPath" -ForegroundColor Green
Write-Host ""

# Backup current config.php
Write-Host "Backing up current config.php..." -ForegroundColor Yellow
$timestamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$backupFile = "$htdocsPath\config_backup_$timestamp.php"

if (Test-Path "$htdocsPath\config.php") {
    Copy-Item "$htdocsPath\config.php" $backupFile
    Write-Host "Backup created: $backupFile" -ForegroundColor Green
}

Write-Host ""
Write-Host "Updating config.php with port 3306..." -ForegroundColor Yellow

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
Write-Host "config.php updated successfully with port 3306" -ForegroundColor Green
Write-Host ""

# Copy backend PHP files
Write-Host "Copying updated PHP files from project..." -ForegroundColor Yellow
$projectPath = "C:\Users\HP\Documents\New Folder\Ragamfinal\Ragamfinal\backend_web"

$filesToCopy = @("courses.php", "profile.php", "upload_video.php")
$copied = 0

foreach ($file in $filesToCopy) {
    $source = "$projectPath\$file"
    if (Test-Path $source) {
        Copy-Item $source "$htdocsPath\$file" -Force
        Write-Host "  Copied: $file" -ForegroundColor Green
        $copied++
    }
}

Write-Host "Copied $copied file(s)" -ForegroundColor Green
Write-Host ""
Write-Host "UPDATE COMPLETE!" -ForegroundColor Green
Write-Host "MySQL Port: 3306" -ForegroundColor Cyan
Write-Host "Backend IP: 10.17.207.64" -ForegroundColor Cyan
Write-Host ""
