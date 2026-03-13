<?php
/**
 * Authentication API - MySQL Version
 * Handles login and registration using MySQL database
 */

require_once 'config_mysql.php';

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

$action = isset($_GET['action']) ? $_GET['action'] : '';

// ============ LOGIN ============
if ($action == 'login') {
    $input = json_decode(file_get_contents('php://input'), true);
    $email = isset($input['email']) ? trim($input['email']) : '';
    $password = isset($input['password']) ? $input['password'] : '';

    if (empty($email) || empty($password)) {
        sendError('Email and password are required', 400);
    }

    try {
        $pdo = getDBConnection();
        
        // Fetch user from database
        $stmt = $pdo->prepare("
            SELECT user_id, email, password, full_name, phone, user_type, 
                   profile_image, bio, experience_years, specialization, 
                   is_verified, is_active 
            FROM users 
            WHERE email = ? AND is_active = 1
        ");
        $stmt->execute([$email]);
        $user = $stmt->fetch();

        if (!$user) {
            sendError('Invalid email or password', 401);
        }

        // Verify password
        if (!password_verify($password, $user['password'])) {
            sendError('Invalid email or password', 401);
        }

        // Remove password from response
        unset($user['password']);
        
        // Add legacy fields for compatibility
        $user['id'] = $user['user_id'];
        $user['name'] = $user['full_name'];

        sendSuccess($user, 'Login successful');

    } catch (PDOException $e) {
        error_log("Login error: " . $e->getMessage());
        sendError('Login failed. Please try again.', 500);
    }
}

// ============ REGISTER ============
else if ($action == 'register') {
    $input = json_decode(file_get_contents('php://input'), true);
    
    $email = isset($input['email']) ? trim($input['email']) : '';
    $password = isset($input['password']) ? $input['password'] : '';
    $full_name = isset($input['full_name']) ? trim($input['full_name']) : '';
    $phone = isset($input['phone']) ? trim($input['phone']) : '';
    $user_type = isset($input['user_type']) ? $input['user_type'] : 'student';
    $specialization = isset($input['specialization']) ? $input['specialization'] : null;
    $experience_years = isset($input['experience_years']) ? intval($input['experience_years']) : 0;

    // Validation
    if (empty($email) || empty($password) || empty($full_name)) {
        sendError('Email, password, and full name are required', 400);
    }

    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        sendError('Invalid email format', 400);
    }

    if (strlen($password) < 6) {
        sendError('Password must be at least 6 characters', 400);
    }

    try {
        $pdo = getDBConnection();
        
        // Check if email already exists
        $stmt = $pdo->prepare("SELECT user_id FROM users WHERE email = ?");
        $stmt->execute([$email]);
        if ($stmt->fetch()) {
            sendError('Email already registered', 409);
        }

        // Hash password
        $hashed_password = password_hash($password, PASSWORD_BCRYPT);

        // Insert new user
        $stmt = $pdo->prepare("
            INSERT INTO users (email, password, full_name, phone, user_type, 
                              specialization, experience_years, is_active, is_verified) 
            VALUES (?, ?, ?, ?, ?, ?, ?, 1, 0)
        ");
        
        $stmt->execute([
            $email, 
            $hashed_password, 
            $full_name, 
            $phone, 
            $user_type, 
            $specialization, 
            $experience_years
        ]);

        $user_id = $pdo->lastInsertId();

        // Fetch the created user
        $stmt = $pdo->prepare("
            SELECT user_id, email, full_name, phone, user_type, 
                   profile_image, bio, experience_years, specialization 
            FROM users 
            WHERE user_id = ?
        ");
        $stmt->execute([$user_id]);
        $user = $stmt->fetch();
        
        // Add legacy fields for compatibility
        $user['id'] = $user['user_id'];
        $user['name'] = $user['full_name'];

        sendSuccess($user, 'Registration successful');

    } catch (PDOException $e) {
        error_log("Registration error: " . $e->getMessage());
        sendError('Registration failed. Please try again.', 500);
    }
}

// ============ VERIFY USER ============
else if ($action == 'verify') {
    $input = json_decode(file_get_contents('php://input'), true);
    $user_id = isset($input['user_id']) ? intval($input['user_id']) : 0;

    if ($user_id <= 0) {
        sendError('Invalid user ID', 400);
    }

    try {
        $pdo = getDBConnection();
        
        $stmt = $pdo->prepare("UPDATE users SET is_verified = 1 WHERE user_id = ?");
        $stmt->execute([$user_id]);

        sendSuccess(null, 'User verified successfully');

    } catch (PDOException $e) {
        error_log("Verification error: " . $e->getMessage());
        sendError('Verification failed', 500);
    }
}

else {
    sendError('Invalid action', 400);
}
?>
