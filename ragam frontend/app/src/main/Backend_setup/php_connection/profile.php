<?php
/**
 * User Profile API
 * Handles user profile operations
 */

include_once 'config.php';

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, PUT, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

$database = new DatabaseConfig();
$db = $database->getConnection();

$method = $_SERVER['REQUEST_METHOD'];
$action = isset($_GET['action']) ? $_GET['action'] : '';

switch($method) {
    case 'GET':
        if($action == 'profile') {
            getUserProfile($db);
        } elseif($action == 'mentors') {
            getMentors($db);
        } else {
            APIResponse::error("Invalid action");
        }
        break;
    case 'PUT':
        if($action == 'update_profile') {
            updateProfile($db);
        } else {
            APIResponse::error("Invalid action");
        }
        break;
    default:
        APIResponse::error("Method not allowed", 405);
        break;
}

function getUserProfile($db) {
    $user_id = isset($_GET['user_id']) ? $_GET['user_id'] : '';
    
    if(empty($user_id)) {
        APIResponse::error("User ID is required");
        return;
    }
    
    try {
        $query = "SELECT user_id, email, full_name, phone, user_type, profile_image, 
                         bio, experience_years, specialization, is_verified, created_at
                  FROM users 
                  WHERE user_id = :user_id AND is_active = 1";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":user_id", $user_id);
        $stmt->execute();
        
        if($stmt->rowCount() > 0) {
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
            
            // Get additional stats based on user type
            if($user['user_type'] == 'student') {
                // Get enrolled courses count
                $statsQuery = "SELECT COUNT(*) as enrolled_courses FROM enrollments 
                              WHERE student_id = :user_id AND payment_status = 'completed'";
                $statsStmt = $db->prepare($statsQuery);
                $statsStmt->bindParam(":user_id", $user_id);
                $statsStmt->execute();
                $stats = $statsStmt->fetch(PDO::FETCH_ASSOC);
                $user['enrolled_courses'] = $stats['enrolled_courses'];
            } else {
                // Get created courses count
                $statsQuery = "SELECT COUNT(*) as created_courses FROM courses 
                              WHERE teacher_id = :user_id";
                $statsStmt = $db->prepare($statsQuery);
                $statsStmt->bindParam(":user_id", $user_id);
                $statsStmt->execute();
                $stats = $statsStmt->fetch(PDO::FETCH_ASSOC);
                $user['created_courses'] = $stats['created_courses'];
            }
            
            APIResponse::success($user, "Profile retrieved successfully");
        } else {
            APIResponse::error("User not found");
        }
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function getMentors($db) {
    try {
        $query = "SELECT u.user_id, u.full_name, u.profile_image, u.bio, u.experience_years, 
                         u.specialization, u.is_verified,
                         COUNT(c.course_id) as course_count,
                         AVG(c.rating) as avg_rating
                  FROM users u
                  LEFT JOIN courses c ON u.user_id = c.teacher_id AND c.is_published = 1
                  WHERE u.user_type = 'teacher' AND u.is_active = 1
                  GROUP BY u.user_id
                  ORDER BY u.is_verified DESC, course_count DESC";
        
        $stmt = $db->prepare($query);
        $stmt->execute();
        
        $mentors = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        APIResponse::success($mentors, "Mentors retrieved successfully");
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function updateProfile($db) {
    $data = json_decode(file_get_contents("php://input"));
    
    if(empty($data->user_id)) {
        APIResponse::error("User ID is required");
        return;
    }
    
    try {
        $updateFields = [];
        $params = [':user_id' => $data->user_id];
        
        if(isset($data->full_name)) {
            $updateFields[] = "full_name = :full_name";
            $params[':full_name'] = $data->full_name;
        }
        
        if(isset($data->phone)) {
            $updateFields[] = "phone = :phone";
            $params[':phone'] = $data->phone;
        }
        
        if(isset($data->bio)) {
            $updateFields[] = "bio = :bio";
            $params[':bio'] = $data->bio;
        }
        
        if(isset($data->specialization)) {
            $updateFields[] = "specialization = :specialization";
            $params[':specialization'] = $data->specialization;
        }
        
        if(isset($data->experience_years)) {
            $updateFields[] = "experience_years = :experience_years";
            $params[':experience_years'] = $data->experience_years;
        }
        
        if(isset($data->profile_image)) {
            $updateFields[] = "profile_image = :profile_image";
            $params[':profile_image'] = $data->profile_image;
        }
        
        if(empty($updateFields)) {
            APIResponse::error("No fields to update");
            return;
        }
        
        $query = "UPDATE users SET " . implode(', ', $updateFields) . " WHERE user_id = :user_id";
        
        $stmt = $db->prepare($query);
        
        if($stmt->execute($params)) {
            APIResponse::success(null, "Profile updated successfully");
        } else {
            APIResponse::error("Failed to update profile");
        }
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}
?>
