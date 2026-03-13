<?php
/**
 * User Authentication API
 * Handles student and teacher login/signup
 */

// Turn off error display and enable error logging
error_reporting(E_ALL);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

// Set content type to JSON first
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

// Include config with error handling
if (!file_exists('config.php')) {
    echo json_encode(['status' => 'error', 'message' => 'Configuration file not found']);
    exit;
}

include_once 'config.php';

$database = new DatabaseConfig();
$db = $database->getConnection();

// Check if database connection failed
if (!$db) {
    APIResponse::error("Database connection failed");
    exit;
}

$method = $_SERVER['REQUEST_METHOD'];
$action = isset($_GET['action']) ? $_GET['action'] : '';

switch($method) {
    case 'POST':
        if($action == 'login') {
            loginUser($db);
        } elseif($action == 'register') {
            registerUser($db);
        } else {
            APIResponse::error("Invalid action");
        }
        break;
    default:
        APIResponse::error("Method not allowed", 405);
        break;
}

function loginUser($db) {
    $rawInput = file_get_contents("php://input");
    
    if (empty($rawInput)) {
        APIResponse::error("No input data received");
        return;
    }
    
    $data = json_decode($rawInput);
    
    if (json_last_error() !== JSON_ERROR_NONE) {
        APIResponse::error("Invalid JSON data: " . json_last_error_msg());
        return;
    }
    
    if(empty($data->email) || empty($data->password) || empty($data->user_type)) {
        APIResponse::error("Email, password and user type are required");
        return;
    }
    
    try {
        $query = "SELECT user_id, email, full_name, phone, user_type, profile_image, bio, 
                         experience_years, specialization, is_verified, is_active, password 
                  FROM users 
                  WHERE email = :email AND user_type = :user_type AND is_active = 1";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":email", $data->email);
        $stmt->bindParam(":user_type", $data->user_type);
        $stmt->execute();
        
        if($stmt->rowCount() > 0) {
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
            
            // Verify password (assuming you'll hash passwords)
            if(password_verify($data->password, $user['password']) || $data->password == $user['password']) {
                // Remove password from response
                unset($user['password']);
                
                APIResponse::success($user, "Login successful");
            } else {
                APIResponse::error("Invalid credentials");
            }
        } else {
            APIResponse::error("User not found or inactive");
        }
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function registerUser($db) {
    $rawInput = file_get_contents("php://input");
    
    if (empty($rawInput)) {
        APIResponse::error("No input data received");
        return;
    }
    
    $data = json_decode($rawInput);
    
    if (json_last_error() !== JSON_ERROR_NONE) {
        APIResponse::error("Invalid JSON data: " . json_last_error_msg());
        return;
    }
    
    if(empty($data->email) || empty($data->password) || empty($data->full_name) || empty($data->user_type)) {
        APIResponse::error("Email, password, full name and user type are required");
        return;
    }
    
    try {
        // Check if user already exists
        $checkQuery = "SELECT user_id FROM users WHERE email = :email";
        $checkStmt = $db->prepare($checkQuery);
        $checkStmt->bindParam(":email", $data->email);
        $checkStmt->execute();
        
        if($checkStmt->rowCount() > 0) {
            APIResponse::error("User already exists with this email");
            return;
        }
        
        // Hash password
        $hashedPassword = password_hash($data->password, PASSWORD_DEFAULT);
        
        // Prepare optional fields
        $phone = isset($data->phone) ? $data->phone : null;
        $bio = isset($data->bio) ? $data->bio : null;
        $experienceYears = isset($data->experience_years) ? $data->experience_years : 0;
        $specialization = isset($data->specialization) ? $data->specialization : null;
        
        $query = "INSERT INTO users (email, password, full_name, phone, user_type, bio, experience_years, specialization) 
                  VALUES (:email, :password, :full_name, :phone, :user_type, :bio, :experience_years, :specialization)";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":email", $data->email);
        $stmt->bindParam(":password", $hashedPassword);
        $stmt->bindParam(":full_name", $data->full_name);
        $stmt->bindParam(":phone", $phone);
        $stmt->bindParam(":user_type", $data->user_type);
        $stmt->bindParam(":bio", $bio);
        $stmt->bindParam(":experience_years", $experienceYears);
        $stmt->bindParam(":specialization", $specialization);
        
        if($stmt->execute()) {
            $userId = $db->lastInsertId();
            
            // Get the created user
            $getUserQuery = "SELECT user_id, email, full_name, phone, user_type, profile_image, 
                                   bio, experience_years, specialization, is_verified, is_active 
                            FROM users WHERE user_id = :user_id";
            $getUserStmt = $db->prepare($getUserQuery);
            $getUserStmt->bindParam(":user_id", $userId);
            $getUserStmt->execute();
            
            $user = $getUserStmt->fetch(PDO::FETCH_ASSOC);
            
            APIResponse::success($user, "Registration successful");
        } else {
            APIResponse::error("Registration failed");
        }
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}
?>
