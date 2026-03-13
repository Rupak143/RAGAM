<?php
/**
 * Profile API - MySQL Version
 * Handles user profile operations
 */
require_once 'config_mysql.php';
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}
$action = isset($_GET['action']) ? $_GET['action'] : '';
// ============ GET USER PROFILE ============
if ($action == 'profile') {
    $user_id = isset($_GET['user_id']) ? intval($_GET['user_id']) : 0;
    if ($user_id <= 0) {
        sendError('User ID is required', 400);
    }
    try {
        $pdo = getDBConnection();
        $stmt = $pdo->prepare("
            SELECT 
                user_id,
                email,
                full_name,
                phone,
                user_type,
                profile_image,
                bio,
                experience_years,
                specialization,
                is_verified,
                is_active,
                created_at
            FROM users
            WHERE user_id = ?
        ");
        $stmt->execute([$user_id]);
        $user = $stmt->fetch();
        if ($user) {
            // Add computed fields for compatibility
            $user['id'] = $user['user_id'];
            $user['name'] = $user['full_name'];
            sendSuccess($user, 'Profile retrieved successfully');
        } else {
            sendError('User not found', 404);
        }
    } catch (PDOException $e) {
        error_log("Get profile error: " . $e->getMessage());
        sendError('Failed to retrieve profile: ' . $e->getMessage(), 500);
    }
}
// ============ UPDATE USER PROFILE ============
else if ($action == 'update_profile') {
    $rawInput = file_get_contents("php://input");
    $data = json_decode($rawInput, true);
    $user_id = isset($data['user_id']) ? intval($data['user_id']) : 0;
    if ($user_id <= 0) {
        sendError('User ID is required', 400);
    }
    try {
        $pdo = getDBConnection();
        // Build dynamic update query based on provided fields
        $updates = [];
        $params = [];
        if (isset($data['full_name']) && !empty($data['full_name'])) {
            $updates[] = "full_name = ?";
            $params[] = trim($data['full_name']);
        }
        if (isset($data['phone'])) {
            $updates[] = "phone = ?";
            $params[] = trim($data['phone']);
        }
        if (isset($data['bio'])) {
            $updates[] = "bio = ?";
            $params[] = trim($data['bio']);
        }
        if (isset($data['specialization'])) {
            $updates[] = "specialization = ?";
            $params[] = trim($data['specialization']);
        }
        if (isset($data['experience_years'])) {
            $updates[] = "experience_years = ?";
            $params[] = intval($data['experience_years']);
        }
        if (empty($updates)) {
            sendError('No fields to update', 400);
        }
        $params[] = $user_id;
        $sql = "UPDATE users SET " . implode(", ", $updates) . " WHERE user_id = ?";
        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        // Get updated profile
        $stmt = $pdo->prepare("
            SELECT 
                user_id,
                email,
                full_name,
                phone,
                user_type,
                profile_image,
                bio,
                experience_years,
                specialization,
                is_verified,
                is_active
            FROM users
            WHERE user_id = ?
        ");
        $stmt->execute([$user_id]);
        $user = $stmt->fetch();
        sendSuccess($user, 'Profile updated successfully');
    } catch (PDOException $e) {
        error_log("Update profile error: " . $e->getMessage());
        sendError('Failed to update profile: ' . $e->getMessage(), 500);
    }
}
// ============ GET MENTORS (for compatibility) ============
else if ($action == 'mentors') {
    // Redirect to courses.php get_mentors action
    header("Location: courses.php?action=get_mentors");
    exit();
}
else {
    sendError('Invalid action: ' . $action, 400);
}
?>
