<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, PUT');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'config.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    try {
        $database = new DatabaseConfig();
        $pdo = $database->getConnection();
        
        if (!$pdo) {
            throw new Exception('Database connection failed');
        }
        
        $action = $_POST['action'] ?? '';
        
        switch ($action) {
            case 'update_profile':
                updateProfile($pdo);
                break;
            case 'upload_profile_photo':
                uploadProfilePhoto($pdo);
                break;
            default:
                throw new Exception('Invalid action');
        }
        
    } catch (Exception $e) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => $e->getMessage()
        ]);
    }
} else if ($_SERVER['REQUEST_METHOD'] === 'GET') {
    try {
        $database = new DatabaseConfig();
        $pdo = $database->getConnection();
        
        if (!$pdo) {
            throw new Exception('Database connection failed');
        }
        
        getProfile($pdo);
        
    } catch (Exception $e) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => $e->getMessage()
        ]);
    }
} else {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
}

function updateProfile($pdo) {
    $userId = $_POST['user_id'] ?? '';
    $fullName = $_POST['full_name'] ?? '';
    $email = $_POST['email'] ?? '';
    $phone = $_POST['phone'] ?? '';
    $password = $_POST['password'] ?? '';
    $bio = $_POST['bio'] ?? '';
    $specialization = $_POST['specialization'] ?? '';
    $experienceYears = $_POST['experience_years'] ?? 0;
    
    if (empty($userId) || empty($fullName) || empty($email)) {
        throw new Exception('User ID, full name, and email are required');
    }
    
    // Check if email already exists for other users
    $stmt = $pdo->prepare("SELECT user_id FROM users WHERE email = ? AND user_id != ?");
    $stmt->execute([$email, $userId]);
    if ($stmt->fetch()) {
        throw new Exception('Email already exists');
    }
    
    // Prepare update query
    $updateFields = [
        'full_name = ?',
        'email = ?',
        'phone = ?',
        'bio = ?',
        'specialization = ?',
        'experience_years = ?',
        'updated_at = NOW()'
    ];
    $params = [$fullName, $email, $phone, $bio, $specialization, $experienceYears];
    
    // Add password to update if provided
    if (!empty($password)) {
        $updateFields[] = 'password = ?';
        $params[] = password_hash($password, PASSWORD_DEFAULT);
    }
    
    $params[] = $userId; // Add user_id for WHERE clause
    
    $sql = "UPDATE users SET " . implode(', ', $updateFields) . " WHERE user_id = ?";
    $stmt = $pdo->prepare($sql);
    
    if ($stmt->execute($params)) {
        // Get updated user data
        $stmt = $pdo->prepare("SELECT user_id, email, full_name, phone, user_type, profile_image, bio, experience_years, specialization, is_verified, created_at FROM users WHERE user_id = ?");
        $stmt->execute([$userId]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        
        echo json_encode([
            'success' => true,
            'message' => 'Profile updated successfully',
            'data' => [
                'user' => $user
            ]
        ]);
    } else {
        throw new Exception('Failed to update profile');
    }
}

function uploadProfilePhoto($pdo) {
    if (!isset($_FILES['profile_photo']) || $_FILES['profile_photo']['error'] !== UPLOAD_ERR_OK) {
        throw new Exception('No profile photo uploaded or upload error occurred');
    }
    
    $userId = $_POST['user_id'] ?? '';
    if (empty($userId)) {
        throw new Exception('User ID is required');
    }
    
    $uploadedFile = $_FILES['profile_photo'];
    
    // Validate file type - check both MIME type and extension
    $allowedMimeTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif'];
    $allowedExtensions = ['jpg', 'jpeg', 'png', 'gif'];
    
    $fileType = $uploadedFile['type'];
    $fileExtension = strtolower(pathinfo($uploadedFile['name'], PATHINFO_EXTENSION));
    
    $isValidMimeType = in_array($fileType, $allowedMimeTypes);
    $isValidExtension = in_array($fileExtension, $allowedExtensions);
    
    if (!$isValidMimeType && !$isValidExtension) {
        throw new Exception('Invalid file type. Only JPEG, PNG, and GIF images are allowed. Detected: ' . $fileType . ', Extension: ' . $fileExtension);
    }
    
    // Validate file size (max 10MB)
    $maxSize = 10 * 1024 * 1024; // 10MB in bytes
    if ($uploadedFile['size'] > $maxSize) {
        throw new Exception('File size too large. Maximum size is 10MB');
    }
    
    // Create uploads directory if it doesn't exist
    $uploadDir = 'uploads/profile_photos/';
    if (!is_dir($uploadDir)) {
        mkdir($uploadDir, 0755, true);
    }
    
    // Generate unique filename
    $fileExtension = pathinfo($uploadedFile['name'], PATHINFO_EXTENSION);
    $filename = 'profile_' . $userId . '_' . time() . '.' . $fileExtension;
    $uploadPath = $uploadDir . $filename;
    
    // Move uploaded file
    if (!move_uploaded_file($uploadedFile['tmp_name'], $uploadPath)) {
        throw new Exception('Failed to move uploaded file');
    }
    
    // Update user's profile_image in database
    $relativePath = 'profile_photos/' . $filename;
    $stmt = $pdo->prepare("UPDATE users SET profile_image = ?, updated_at = NOW() WHERE user_id = ?");
    
    if ($stmt->execute([$relativePath, $userId])) {
        echo json_encode([
            'success' => true,
            'message' => 'Profile photo uploaded successfully',
            'data' => [
                'profile_image' => $relativePath,
                'filename' => $filename
            ]
        ]);
    } else {
        // Delete uploaded file if database update fails
        unlink($uploadPath);
        throw new Exception('Failed to update profile photo in database');
    }
}

function getProfile($pdo) {
    $userId = $_GET['user_id'] ?? '';
    if (empty($userId)) {
        throw new Exception('User ID is required');
    }
    
    $stmt = $pdo->prepare("SELECT user_id, email, full_name, phone, user_type, profile_image, bio, experience_years, specialization, is_verified, created_at FROM users WHERE user_id = ?");
    $stmt->execute([$userId]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$user) {
        throw new Exception('User not found');
    }
    
    echo json_encode([
        'success' => true,
        'message' => 'Profile retrieved successfully',
        'data' => [
            'user' => $user
        ]
    ]);
}
?>
