<?php
// Remove BOM if present
ob_start();

header('Content-Type: application/json; charset=UTF-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, PUT, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

$action = isset($_GET['action']) ? $_GET['action'] : '';

// GET MENTORS/TEACHERS
if ($action == 'mentors') {
    ob_clean();
    echo json_encode(array(
        'success' => true,
        'status' => 'success',
        'message' => 'Mentors retrieved successfully',
        'data' => array(
            array(
                'user_id' => 12,
                'full_name' => 'Ram',
                'email' => 'ram@gmail.com',
                'specialization' => 'Flute',
                'experience_years' => 10,
                'bio' => 'Expert Flute instructor with 10 years of experience in teaching classical and contemporary flute techniques',
                'is_verified' => 1,
                'course_count' => 1,
                'avg_rating' => '5.0',
                'profile_image' => null
            ),
            array(
                'user_id' => 11,
                'full_name' => 'Mohan Teacher',
                'email' => 'mohan@gmail.com',
                'specialization' => 'Carnatic Vocal',
                'experience_years' => 15,
                'bio' => 'Senior Carnatic music instructor specializing in traditional vocal training and ragas',
                'is_verified' => 1,
                'course_count' => 2,
                'avg_rating' => '4.5',
                'profile_image' => null
            ),
            array(
                'user_id' => 4,
                'full_name' => 'Ram',
                'email' => 'ram4@gmail.com',
                'specialization' => 'PhD',
                'experience_years' => 20,
                'bio' => 'PhD in Music Education',
                'is_verified' => 1,
                'course_count' => 1,
                'avg_rating' => '5.0',
                'profile_image' => null
            ),
            array(
                'user_id' => 6,
                'full_name' => 'Ravina',
                'email' => 'ravina@gmail.com',
                'specialization' => 'Vocal',
                'experience_years' => 12,
                'bio' => 'Vocal music specialist',
                'is_verified' => 1,
                'course_count' => 3,
                'avg_rating' => '4.8',
                'profile_image' => null
            )
        )
    ));
    exit;
}

// GET USER PROFILE
if ($action == 'profile') {
    $userId = isset($_GET['user_id']) ? $_GET['user_id'] : '';
    
    if (empty($userId)) {
        ob_clean();
        echo json_encode(array(
            'success' => false,
            'status' => 'error',
            'message' => 'User ID is required',
            'data' => null
        ));
        exit;
    }
    
    // Return profile based on user ID
    if ($userId == 9 || $userId == '9') {
        $user = array(
            'user_id' => 9,
            'email' => 'rupak@gmail.com',
            'full_name' => 'Rupak Student',
            'phone' => '9191700000',
            'user_type' => 'student',
            'bio' => 'Music enthusiast learning classical music',
            'enrolled_courses' => 2,
            'is_verified' => 1,
            'is_active' => 1,
            'created_at' => '2024-01-01 10:00:00'
        );
    } else if ($userId == 1 || $userId == '1') {
        $user = array(
            'user_id' => 1,
            'email' => 'rupak@gmail.com',
            'full_name' => 'Rupak Student',
            'phone' => '9191700000',
            'user_type' => 'student',
            'bio' => 'Music enthusiast learning classical music',
            'enrolled_courses' => 2,
            'is_verified' => 1,
            'is_active' => 1,
            'created_at' => '2024-01-01 10:00:00'
        );
    } else if ($userId == 4 || $userId == '4') {
        $user = array(
            'user_id' => 4,
            'email' => 'ram4@gmail.com',
            'full_name' => 'Ram',
            'phone' => '9191700004',
            'user_type' => 'teacher',
            'specialization' => 'PhD',
            'experience_years' => 20,
            'bio' => 'PhD in Music Education',
            'created_courses' => 1,
            'is_verified' => 1,
            'is_active' => 1,
            'created_at' => '2023-01-01 10:00:00'
        );
    } else if ($userId == 6 || $userId == '6') {
        $user = array(
            'user_id' => 6,
            'email' => 'ravina@gmail.com',
            'full_name' => 'Ravina',
            'phone' => '9191700006',
            'user_type' => 'teacher',
            'specialization' => 'Vocal',
            'experience_years' => 12,
            'bio' => 'Vocal music specialist',
            'created_courses' => 3,
            'is_verified' => 1,
            'is_active' => 1,
            'created_at' => '2023-01-01 10:00:00'
        );
    } else if ($userId == 12 || $userId == '12') {
        $user = array(
            'user_id' => 12,
            'email' => 'ram@gmail.com',
            'full_name' => 'Ram',
            'phone' => '9191700012',
            'user_type' => 'teacher',
            'specialization' => 'Flute',
            'experience_years' => 10,
            'bio' => 'Expert Flute instructor',
            'created_courses' => 1,
            'is_verified' => 1,
            'is_active' => 1,
            'created_at' => '2023-01-01 10:00:00'
        );
    } else {
        $user = array(
            'user_id' => $userId,
            'email' => 'user@example.com',
            'full_name' => 'Demo User',
            'user_type' => 'student',
            'enrolled_courses' => 0,
            'is_verified' => 1,
            'is_active' => 1
        );
    }
    
    ob_clean();
    echo json_encode(array(
        'success' => true,
        'status' => 'success',
        'message' => 'Profile retrieved successfully',
        'data' => $user
    ));
    exit;
}

// UPDATE PROFILE
if ($action == 'update_profile') {
    $rawInput = file_get_contents("php://input");
    $data = json_decode($rawInput, true);
    
    if (!$data || empty($data['user_id'])) {
        ob_clean();
        echo json_encode(array(
            'success' => false,
            'status' => 'error',
            'message' => 'User ID is required',
            'data' => null
        ));
        exit;
    }
    
    ob_clean();
    echo json_encode(array(
        'success' => true,
        'status' => 'success',
        'message' => 'Profile updated successfully',
        'data' => $data
    ));
    exit;
}

// Default response
ob_clean();
echo json_encode(array(
    'success' => false,
    'status' => 'error',
    'message' => 'Invalid action: ' . $action,
    'data' => null
));
?>
