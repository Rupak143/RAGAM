<?php
/**
 * Simple Mock Authentication API for Testing
 * This works WITHOUT database - perfect for testing sign-in pages
 */

// Set headers
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

$method = $_SERVER['REQUEST_METHOD'];
$action = isset($_GET['action']) ? $_GET['action'] : '';

// Handle only POST requests
if ($method !== 'POST') {
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed',
        'data' => null
    ]);
    exit;
}

// Handle login action
if ($action == 'login') {
    $rawInput = file_get_contents("php://input");
    
    if (empty($rawInput)) {
        echo json_encode([
            'success' => false,
            'message' => 'No input data received',
            'data' => null
        ]);
        exit;
    }
    
    $data = json_decode($rawInput);
    
    if (json_last_error() !== JSON_ERROR_NONE) {
        echo json_encode([
            'success' => false,
            'message' => 'Invalid JSON data',
            'data' => null
        ]);
        exit;
    }
    
    // Check required fields
    if (empty($data->email) || empty($data->password) || empty($data->user_type)) {
        echo json_encode([
            'success' => false,
            'message' => 'Email, password and user type are required',
            'data' => null
        ]);
        exit;
    }
    
    // Mock user database - add any test users here
    $mockUsers = [
        [
            'email' => 'student@test.com',
            'password' => 'password',
            'user_type' => 'student',
            'user_id' => '1',
            'full_name' => 'Test Student',
            'phone' => '1234567890'
        ],
        [
            'email' => 'teacher@test.com',
            'password' => 'password',
            'user_type' => 'teacher',
            'user_id' => '2',
            'full_name' => 'Test Teacher',
            'phone' => '0987654321'
        ],
        [
            'email' => 'student@gmail.com',
            'password' => '123456',
            'user_type' => 'student',
            'user_id' => '3',
            'full_name' => 'Demo Student',
            'phone' => '5555555555'
        ],
        [
            'email' => 'teacher@gmail.com',
            'password' => '123456',
            'user_type' => 'teacher',
            'user_id' => '4',
            'full_name' => 'Demo Teacher',
            'phone' => '6666666666'
        ]
    ];
    
    // Check credentials
    $foundUser = null;
    foreach ($mockUsers as $user) {
        if ($user['email'] === $data->email && 
            $user['password'] === $data->password && 
            $user['user_type'] === $data->user_type) {
            $foundUser = $user;
            break;
        }
    }
    
    if ($foundUser) {
        // Remove password from response
        unset($foundUser['password']);
        
        // Add fields Android app expects
        $foundUser['id'] = $foundUser['user_id'];
        $foundUser['name'] = $foundUser['full_name'];
        $foundUser['profile_image'] = null;
        $foundUser['bio'] = null;
        $foundUser['experience_years'] = 0;
        $foundUser['specialization'] = null;
        $foundUser['is_verified'] = true;
        $foundUser['is_active'] = true;
        
        echo json_encode([
            'success' => true,
            'message' => 'Login successful',
            'data' => $foundUser
        ]);
    } else {
        echo json_encode([
            'success' => false,
            'message' => 'Invalid credentials',
            'data' => null
        ]);
    }
    
} elseif ($action == 'register') {
    // Simple registration - just echo back success
    $rawInput = file_get_contents("php://input");
    $data = json_decode($rawInput);
    
    if (empty($data->email) || empty($data->password) || empty($data->full_name) || empty($data->user_type)) {
        echo json_encode([
            'success' => false,
            'message' => 'All fields are required',
            'data' => null
        ]);
        exit;
    }
    
    // Mock successful registration
    $newUser = [
        'id' => rand(100, 999),
        'user_id' => rand(100, 999),
        'email' => $data->email,
        'name' => $data->full_name,
        'full_name' => $data->full_name,
        'user_type' => $data->user_type,
        'phone' => isset($data->phone) ? $data->phone : null,
        'profile_image' => null,
        'bio' => null,
        'experience_years' => 0,
        'specialization' => null,
        'is_verified' => true,
        'is_active' => true
    ];
    
    echo json_encode([
        'success' => true,
        'message' => 'Registration successful',
        'data' => $newUser
    ]);
    
} else {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid action',
        'data' => null
    ]);
}
?>