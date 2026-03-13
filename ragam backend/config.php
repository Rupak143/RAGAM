<?php
/**
 * Database Configuration for Ragam Final Music App
 * Place this file in your htdocs/ragam_api/ folder
 */

class DatabaseConfig {
    // Database credentials
    private $host = "localhost";
    private $port = "3305";
    private $username = "root";
    private $password = "";
    private $database = "ragam";
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