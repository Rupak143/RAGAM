<?php
/**
 * Courses API
 * Handles course-related operations
 */

// Enable error reporting for debugging
error_reporting(E_ALL);
ini_set('display_errors', 1);
ini_set('log_errors', 1);
ini_set('error_log', 'php_errors.log');

include_once 'config.php';

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

$database = new DatabaseConfig();
$db = $database->getConnection();

// Check if database connection was successful
if ($db === null) {
    error_log("Database connection failed in courses.php");
    APIResponse::error("Database connection failed", 500);
    exit;
}

// Test database connection
try {
    $testStmt = $db->query("SELECT COUNT(*) as count FROM courses");
    $testResult = $testStmt->fetch(PDO::FETCH_ASSOC);
    error_log("Database test successful - found " . $testResult['count'] . " courses in database");
} catch (Exception $e) {
    error_log("Database test failed: " . $e->getMessage());
}

$method = $_SERVER['REQUEST_METHOD'];
$action = isset($_GET['action']) ? trim($_GET['action']) : '';

error_log("Courses.php - Method: " . $method . ", Action: '" . $action . "'");

switch($method) {
    case 'GET':
        if($action == 'all_courses') {
            getAllCourses($db);
        } elseif($action == 'course_details') {
            getCourseDetails($db);
        } elseif($action == 'categories') {
            getCategories($db);
        } elseif($action == 'course_lessons') {
            getCourseLessons($db);
        } elseif($action == 'enrolled_courses') {
            getEnrolledCourses($db);
        } elseif($action == 'teacher_courses') {
            getTeacherCourses($db);
        } elseif($action == 'course_students') {
            getCourseStudents($db);
        } elseif($action == 'teacher_students') {
            getTeacherStudents($db);
        } elseif($action == 'debug_create') {
            debugCreateCourse($db);
        } elseif($action == 'get_mentors') {
            getMentors($db);
        } elseif($action == 'get_course_videos') {
            getCourseVideos($db);
        } elseif($action == 'get_test_paper') {
            getTestPaper($db);
        } elseif($action == 'completed_courses') {
            getCompletedCourses($db);
        } elseif($action == 'get_video_progress') {
            getVideoProgress($db);
        } else {
            error_log("Invalid action received: '" . $action . "'");
            APIResponse::error("Invalid action - " . $action);
        }
        break;
    case 'POST':
        if($action == 'enroll') {
            enrollCourse($db);
        } elseif($action == 'create_course') {
            createCourse($db);
        } elseif($action == 'delete_course') {
            deleteCourse($db);
        } elseif($action == 'add_lesson') {
            addCourseLesson($db);
        } elseif($action == 'save_test_paper') {
            saveTestPaper($db);
        } elseif($action == 'mark_video_completed') {
            markVideoCompleted($db);
        } else {
            APIResponse::error("Invalid action");
        }
        break;
    default:
        APIResponse::error("Method not allowed", 405);
        break;
}

function getAllCourses($db) {
    try {
        $query = "SELECT c.*, u.full_name as teacher_name, u.profile_image as teacher_image,
                         cat.category_name
                  FROM courses c
                  INNER JOIN users u ON c.teacher_id = u.user_id
                  INNER JOIN categories cat ON c.category_id = cat.category_id
                  WHERE c.is_published = 1
                  ORDER BY c.created_at DESC";
        
        $stmt = $db->prepare($query);
        $stmt->execute();
        
        $courses = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        APIResponse::success($courses, "Courses retrieved successfully");
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function getCourseDetails($db) {
    $course_id = isset($_GET['course_id']) ? $_GET['course_id'] : '';
    
    if(empty($course_id)) {
        APIResponse::error("Course ID is required");
        return;
    }
    
    try {
        $query = "SELECT c.*, u.full_name as teacher_name, u.profile_image as teacher_image,
                         u.bio as teacher_bio, u.experience_years, u.specialization,
                         cat.category_name
                  FROM courses c
                  JOIN users u ON c.teacher_id = u.user_id
                  JOIN categories cat ON c.category_id = cat.category_id
                  WHERE c.course_id = :course_id AND c.is_published = 1";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":course_id", $course_id);
        $stmt->execute();
        
        if($stmt->rowCount() > 0) {
            $course = $stmt->fetch(PDO::FETCH_ASSOC);
            APIResponse::success($course, "Course details retrieved successfully");
        } else {
            APIResponse::error("Course not found");
        }
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function getCategories($db) {
    try {
        $query = "SELECT * FROM categories WHERE is_active = 1 ORDER BY category_name";
        $stmt = $db->prepare($query);
        $stmt->execute();
        
        $categories = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        APIResponse::success($categories, "Categories retrieved successfully");
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function getCourseLessons($db) {
    $course_id = isset($_GET['course_id']) ? $_GET['course_id'] : '';
    $student_id = isset($_GET['student_id']) ? $_GET['student_id'] : '';
    
    if(empty($course_id)) {
        APIResponse::error("Course ID is required");
        return;
    }
    
    try {
        // Check if student is enrolled
        $enrolled = false;
        if(!empty($student_id)) {
            $enrollQuery = "SELECT enrollment_id FROM enrollments 
                           WHERE student_id = :student_id AND course_id = :course_id";
            $enrollStmt = $db->prepare($enrollQuery);
            $enrollStmt->bindParam(":student_id", $student_id);
            $enrollStmt->bindParam(":course_id", $course_id);
            $enrollStmt->execute();
            $enrolled = $enrollStmt->rowCount() > 0;
        }
        
        $query = "SELECT cl.*, 
                         CASE WHEN sp.is_completed IS NULL THEN 0 ELSE sp.is_completed END as is_completed
                  FROM course_lessons cl
                  LEFT JOIN student_progress sp ON cl.lesson_id = sp.lesson_id AND sp.student_id = :student_id
                  WHERE cl.course_id = :course_id
                  ORDER BY cl.lesson_order";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":course_id", $course_id);
        $stmt->bindParam(":student_id", $student_id);
        $stmt->execute();
        
        $lessons = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        // Hide video URLs for non-enrolled students and non-free lessons
        if(!$enrolled) {
            foreach($lessons as &$lesson) {
                if(!$lesson['is_free']) {
                    $lesson['video_url'] = null;
                }
            }
        }
        
        APIResponse::success([
            'lessons' => $lessons,
            'is_enrolled' => $enrolled
        ], "Course lessons retrieved successfully");
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function enrollCourse($db) {
    $data = json_decode(file_get_contents("php://input"));
    
    if(empty($data->student_id) || empty($data->course_id)) {
        APIResponse::error("Student ID and Course ID are required");
        return;
    }
    
    try {
        // Check if already enrolled
        $checkQuery = "SELECT enrollment_id FROM enrollments 
                      WHERE student_id = :student_id AND course_id = :course_id";
        $checkStmt = $db->prepare($checkQuery);
        $checkStmt->bindParam(":student_id", $data->student_id);
        $checkStmt->bindParam(":course_id", $data->course_id);
        $checkStmt->execute();
        
        if($checkStmt->rowCount() > 0) {
            APIResponse::error("Already enrolled in this course");
            return;
        }
        
        // Get course details
        $courseQuery = "SELECT course_price, is_free FROM courses WHERE course_id = :course_id";
        $courseStmt = $db->prepare($courseQuery);
        $courseStmt->bindParam(":course_id", $data->course_id);
        $courseStmt->execute();
        $course = $courseStmt->fetch(PDO::FETCH_ASSOC);
        
        // Set all enrollments as completed (no payment process)
        $payment_status = 'completed';
        
        // Insert enrollment
        $query = "INSERT INTO enrollments (student_id, course_id, payment_status, payment_amount) 
                  VALUES (:student_id, :course_id, :payment_status, :payment_amount)";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":student_id", $data->student_id);
        $stmt->bindParam(":course_id", $data->course_id);
        $stmt->bindParam(":payment_status", $payment_status);
        $stmt->bindParam(":payment_amount", $course['course_price']);
        
        if($stmt->execute()) {
            // Update enrollment count
            $updateQuery = "UPDATE courses SET enrollment_count = enrollment_count + 1 
                           WHERE course_id = :course_id";
            $updateStmt = $db->prepare($updateQuery);
            $updateStmt->bindParam(":course_id", $data->course_id);
            $updateStmt->execute();
            
            APIResponse::success([
                'enrollment_id' => $db->lastInsertId(),
                'payment_status' => $payment_status
            ], "Enrollment successful");
        } else {
            APIResponse::error("Enrollment failed");
        }
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function getEnrolledCourses($db) {
    $student_id = isset($_GET['student_id']) ? intval($_GET['student_id']) : 0;
    
    if($student_id == 0) {
        APIResponse::error("Student ID is required");
        return;
    }
    
    try {
        $query = "SELECT c.course_id, c.course_title, c.course_description, c.course_image, 
                         c.duration_hours, u.full_name as teacher_name,
                         DATE_FORMAT(e.enrollment_date, '%Y-%m-%d %H:%i') as enrollment_date, 
                         e.completion_percentage
                  FROM enrollments e
                  INNER JOIN courses c ON e.course_id = c.course_id
                  LEFT JOIN users u ON c.teacher_id = u.user_id
                  WHERE e.student_id = :student_id
                  ORDER BY e.enrollment_date DESC";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":student_id", $student_id, PDO::PARAM_INT);
        $stmt->execute();
        
        $courses = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        APIResponse::success($courses, "Enrolled courses retrieved successfully");
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function createCourse($db) {
    $data = json_decode(file_get_contents("php://input"));
    
    // Log the received data for debugging
    error_log("Create course request data: " . json_encode($data));
    
    if(empty($data->teacher_id) || empty($data->course_title) || empty($data->course_description)) {
        APIResponse::error("Teacher ID, course title, and description are required");
        return;
    }
    
    try {
        // Set default values if not provided
        $category_id = isset($data->category_id) ? $data->category_id : 1;
        $course_price = isset($data->course_price) ? $data->course_price : 0;
        $difficulty_level = isset($data->difficulty_level) ? $data->difficulty_level : 'beginner';
        
        // Insert course
        $query = "INSERT INTO courses (teacher_id, category_id, course_title, course_description, 
                                     course_price, difficulty_level, is_published, is_free) 
                  VALUES (:teacher_id, :category_id, :course_title, :course_description, 
                         :course_price, :difficulty_level, 1, :is_free)";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":teacher_id", $data->teacher_id);
        $stmt->bindParam(":category_id", $category_id);
        $stmt->bindParam(":course_title", $data->course_title);
        $stmt->bindParam(":course_description", $data->course_description);
        $stmt->bindParam(":course_price", $course_price);
        $stmt->bindParam(":difficulty_level", $difficulty_level);
        $is_free = ($course_price == 0) ? 1 : 0;
        $stmt->bindParam(":is_free", $is_free);
        
        if($stmt->execute()) {
            $courseId = $db->lastInsertId();
            
            // Create first lesson with video
            if(!empty($data->video_path)) {
                $lessonQuery = "INSERT INTO course_lessons (course_id, lesson_title, lesson_description, 
                                                          video_url, lesson_order, is_free) 
                               VALUES (:course_id, :lesson_title, :lesson_description, 
                                      :video_url, 1, 1)";
                
                $lessonStmt = $db->prepare($lessonQuery);
                $lessonStmt->bindParam(":course_id", $courseId);
                $lessonTitle = $data->course_title . " - Introduction";
                $lessonStmt->bindParam(":lesson_title", $lessonTitle);
                $lessonStmt->bindParam(":lesson_description", $data->course_description);
                $lessonStmt->bindParam(":video_url", $data->video_path);
                
                if(!$lessonStmt->execute()) {
                    error_log("Failed to create lesson for course: " . $courseId);
                }
            }
            
            APIResponse::success([
                'course_id' => $courseId,
                'success' => true  // Add this for Android app compatibility
            ], "Course created successfully");
        } else {
            APIResponse::error("Course creation failed - Database insert failed");
        }
    } catch(PDOException $e) {
        error_log("Database error in createCourse: " . $e->getMessage());
        APIResponse::error("Database error: " . $e->getMessage());
    } catch(Exception $e) {
        error_log("General error in createCourse: " . $e->getMessage());
        APIResponse::error("Error: " . $e->getMessage());
    }
}

function addCourseLesson($db) {
    $data = json_decode(file_get_contents("php://input"));
    
    if(empty($data->course_id) || empty($data->video_url)) {
        APIResponse::error("Course ID and video URL are required");
        return;
    }
    
    try {
        // Get max lesson order
        $orderQuery = "SELECT COALESCE(MAX(lesson_order), 0) as max_order FROM course_lessons WHERE course_id = :course_id";
        $orderStmt = $db->prepare($orderQuery);
        $orderStmt->bindParam(":course_id", $data->course_id);
        $orderStmt->execute();
        $result = $orderStmt->fetch(PDO::FETCH_ASSOC);
        $nextOrder = $result['max_order'] + 1;
        
        $lessonQuery = "INSERT INTO course_lessons (course_id, lesson_title, lesson_description, 
                                                  video_url, lesson_order, is_free) 
                       VALUES (:course_id, :lesson_title, :lesson_description, 
                              :video_url, :lesson_order, 1)";
        
        $lessonStmt = $db->prepare($lessonQuery);
        $lessonStmt->bindParam(":course_id", $data->course_id);
        $lessonTitle = isset($data->video_title) ? $data->video_title : "Lesson " . $nextOrder;
        $lessonStmt->bindParam(":lesson_title", $lessonTitle);
        $lessonDescription = isset($data->video_description) ? $data->video_description : "";
        $lessonStmt->bindParam(":lesson_description", $lessonDescription);
        $lessonStmt->bindParam(":video_url", $data->video_url);
        $lessonStmt->bindParam(":lesson_order", $nextOrder);
        
        if($lessonStmt->execute()) {
            APIResponse::success([
                'lesson_id' => $db->lastInsertId(),
                'lesson_order' => $nextOrder
            ], "Lesson added successfully");
        } else {
            APIResponse::error("Failed to add lesson");
        }
    } catch(PDOException $e) {
        error_log("addCourseLesson error: " . $e->getMessage());
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function getTeacherCourses($db) {
    $teacher_id = isset($_GET['teacher_id']) ? $_GET['teacher_id'] : '';
    
    if(empty($teacher_id)) {
        APIResponse::error("Teacher ID is required");
        return;
    }
    
    try {
        $query = "SELECT c.*, COUNT(e.enrollment_id) as enrollment_count
                  FROM courses c
                  LEFT JOIN enrollments e ON c.course_id = e.course_id AND e.payment_status = 'completed'
                  WHERE c.teacher_id = :teacher_id
                  GROUP BY c.course_id
                  ORDER BY c.created_at DESC";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":teacher_id", $teacher_id);
        $stmt->execute();
        
        $courses = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        APIResponse::success($courses, "Teacher courses retrieved successfully");
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function getCourseStudents($db) {
    $course_id = isset($_GET['course_id']) ? intval($_GET['course_id']) : 0;
    
    if($course_id == 0) {
        APIResponse::error("Course ID is required");
        return;
    }
    
    try {
        $query = "SELECT u.user_id, u.full_name, u.email, u.phone,
                         DATE_FORMAT(e.enrollment_date, '%Y-%m-%d %H:%i') as enrollment_date,
                         e.completion_percentage
                  FROM enrollments e
                  INNER JOIN users u ON e.student_id = u.user_id
                  WHERE e.course_id = :course_id
                  ORDER BY e.enrollment_date DESC";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":course_id", $course_id, PDO::PARAM_INT);
        $stmt->execute();
        
        $students = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        APIResponse::success($students, "Course students retrieved successfully");
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function getTeacherStudents($db) {
    $teacher_id = isset($_GET['teacher_id']) ? $_GET['teacher_id'] : '';
    
    if(empty($teacher_id)) {
        APIResponse::error("Teacher ID is required");
        return;
    }
    
    try {
        $query = "SELECT u.user_id, u.full_name, u.email, u.phone, 
                         c.course_title, e.enrollment_date, e.completion_percentage
                  FROM enrollments e
                  JOIN users u ON e.student_id = u.user_id
                  JOIN courses c ON e.course_id = c.course_id
                  WHERE c.teacher_id = :teacher_id AND e.payment_status = 'completed'
                  ORDER BY e.enrollment_date DESC";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":teacher_id", $teacher_id);
        $stmt->execute();
        
        $students = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        APIResponse::success($students, "Teacher students retrieved successfully");
    } catch(PDOException $e) {
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function debugCreateCourse($db) {
    $debugInfo = [];
    
    // Check database connection
    if ($db === null) {
        $debugInfo['database'] = 'Connection failed';
    } else {
        $debugInfo['database'] = 'Connected successfully';
    }
    
    // Check for POST data
    $rawData = file_get_contents("php://input");
    $debugInfo['received_data'] = $rawData;
    $debugInfo['request_method'] = $_SERVER['REQUEST_METHOD'];
    $debugInfo['content_type'] = $_SERVER['CONTENT_TYPE'] ?? 'not set';
    
    // Try to parse JSON
    if (!empty($rawData)) {
        $data = json_decode($rawData);
        if ($data === null) {
            $debugInfo['json_parse'] = 'Failed - ' . json_last_error_msg();
        } else {
            $debugInfo['json_parse'] = 'Success';
            $debugInfo['parsed_data'] = $data;
        }
    } else {
        $debugInfo['json_parse'] = 'No data received';
    }
    
    // Check required tables
    try {
        $tables = ['users', 'courses', 'categories', 'course_lessons'];
        foreach ($tables as $table) {
            $stmt = $db->query("SHOW TABLES LIKE '$table'");
            $debugInfo['tables'][$table] = $stmt->rowCount() > 0 ? 'exists' : 'missing';
        }
        
        // Check for teachers
        $stmt = $db->query("SELECT COUNT(*) as count FROM users WHERE user_type = 'teacher'");
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        $debugInfo['teachers_count'] = $result['count'];
        
        // Check for categories
        $stmt = $db->query("SELECT COUNT(*) as count FROM categories");
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        $debugInfo['categories_count'] = $result['count'];
        
    } catch (Exception $e) {
        $debugInfo['table_check_error'] = $e->getMessage();
    }
    
    APIResponse::success($debugInfo, "Debug information");
}

function deleteCourse($db) {
    try {
        // Get JSON input
        $input = json_decode(file_get_contents('php://input'), true);
        
        if (!isset($input['course_id'])) {
            APIResponse::error("Course ID is required");
            return;
        }
        
        $courseId = $input['course_id'];
        
        // Start transaction
        $db->beginTransaction();
        
        // First, delete all enrollments for this course
        $stmt = $db->prepare("DELETE FROM enrollments WHERE course_id = ?");
        $stmt->execute([$courseId]);
        
        // Delete course lessons if table exists
        try {
            $stmt = $db->prepare("DELETE FROM course_lessons WHERE course_id = ?");
            $stmt->execute([$courseId]);
        } catch (Exception $e) {
            // Table might not exist, continue
        }
        
        // Delete course reviews if table exists
        try {
            $stmt = $db->prepare("DELETE FROM course_reviews WHERE course_id = ?");
            $stmt->execute([$courseId]);
        } catch (Exception $e) {
            // Table might not exist, continue
        }
        
        // Finally, delete the course itself
        $stmt = $db->prepare("DELETE FROM courses WHERE course_id = ?");
        $stmt->execute([$courseId]);
        
        if ($stmt->rowCount() === 0) {
            $db->rollBack();
            APIResponse::error("Course not found or could not be deleted");
            return;
        }
        
        // Commit transaction
        $db->commit();
        
        APIResponse::success(null, "Course deleted successfully");
        
    } catch (Exception $e) {
        // Rollback transaction on error
        if ($db->inTransaction()) {
            $db->rollBack();
        }
        
        error_log("Delete course error: " . $e->getMessage());
        APIResponse::error("Failed to delete course: " . $e->getMessage());
    }
}

function getTestPaper($db) {
    $course_id = isset($_GET['course_id']) ? intval($_GET['course_id']) : 0;
    
    if ($course_id <= 0) {
        error_log("getTestPaper: Invalid course_id provided");
        APIResponse::error('Course ID is required', 400);
        return;
    }
    
    try {
        // First verify the course exists
        $courseCheck = "SELECT course_id, course_title FROM courses WHERE course_id = :course_id";
        $checkStmt = $db->prepare($courseCheck);
        $checkStmt->bindParam(':course_id', $course_id, PDO::PARAM_INT);
        $checkStmt->execute();
        $courseInfo = $checkStmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$courseInfo) {
            error_log("getTestPaper: Course not found with ID: " . $course_id);
            APIResponse::error('Course not found', 404);
            return;
        }
        
        error_log("getTestPaper: Getting test questions for course '" . $courseInfo['course_title'] . "' (ID: " . $course_id . ")");
        
        // Get test questions for this course
        $query = "SELECT 
                    question_id,
                    course_id,
                    question_number,
                    question_text,
                    option_a,
                    option_b,
                    option_c,
                    option_d,
                    correct_answer,
                    created_at
                  FROM test_questions
                  WHERE course_id = :course_id
                  ORDER BY question_number ASC";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(':course_id', $course_id, PDO::PARAM_INT);
        $stmt->execute();
        
        $questions = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        error_log("getTestPaper: Retrieved " . count($questions) . " questions for course ID: " . $course_id);
        
        if (count($questions) == 0) {
            error_log("getTestPaper: No questions found in test_questions table for course_id: " . $course_id);
            // Return success with empty array so app doesn't error
        }
        
        APIResponse::success($questions, 'Test paper retrieved successfully');
        
    } catch (PDOException $e) {
        error_log("getTestPaper error: " . $e->getMessage());
        APIResponse::error('Failed to retrieve test paper: ' . $e->getMessage(), 500);
    }
}

function getCourseVideos($db) {
    $course_id = isset($_GET['course_id']) ? intval($_GET['course_id']) : 0;
    
    if ($course_id <= 0) {
        error_log("getCourseVideos: Invalid course_id provided");
        APIResponse::error('Course ID is required', 400);
        return;
    }
    
    try {
        // First, check if the course exists
        $courseCheck = "SELECT course_id, course_title, teacher_id FROM courses WHERE course_id = :course_id";
        $checkStmt = $db->prepare($courseCheck);
        $checkStmt->bindParam(':course_id', $course_id, PDO::PARAM_INT);
        $checkStmt->execute();
        $courseInfo = $checkStmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$courseInfo) {
            error_log("getCourseVideos: Course not found with ID: " . $course_id);
            APIResponse::error('Course not found', 404);
            return;
        }
        
        error_log("getCourseVideos: Found course '" . $courseInfo['course_title'] . "' (ID: " . $course_id . ", Teacher: " . $courseInfo['teacher_id'] . ")");
        
        // Get all videos for this course
        $query = "SELECT 
                    lesson_id AS video_id,
                    course_id,
                    lesson_title AS title,
                    lesson_description AS description,
                    video_url,
                    lesson_order AS video_number,
                    duration_minutes,
                    is_free,
                    created_at AS uploaded_at
                  FROM course_lessons
                  WHERE course_id = :course_id
                  ORDER BY lesson_order ASC";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(':course_id', $course_id, PDO::PARAM_INT);
        $stmt->execute();
        
        $videos = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        error_log("getCourseVideos: Retrieved " . count($videos) . " videos for course ID: " . $course_id);
        
        if (count($videos) == 0) {
            error_log("getCourseVideos: No videos found in course_lessons table for course_id: " . $course_id);
            // Still return success with empty array so the app doesn't error out
        }
        
        // Log the first video details if available
        if (count($videos) > 0) {
            error_log("getCourseVideos: First video - Title: '" . $videos[0]['title'] . "', URL: '" . $videos[0]['video_url'] . "'");
        }
        
        APIResponse::success($videos, 'Videos retrieved successfully');
        
    } catch (PDOException $e) {
        error_log("getCourseVideos error: " . $e->getMessage());
        APIResponse::error('Failed to retrieve videos: ' . $e->getMessage(), 500);
    }
}

function saveTestPaper($db) {
    $data = json_decode(file_get_contents("php://input"), true);
    
    if (empty($data['course_id'])) {
        error_log("saveTestPaper: Missing course_id");
        APIResponse::error('Course ID is required', 400);
        return;
    }
    
    if (empty($data['questions']) || !is_array($data['questions'])) {
        error_log("saveTestPaper: Missing or invalid questions");
        APIResponse::error('Questions array is required', 400);
        return;
    }
    
    $courseId = intval($data['course_id']);
    $questions = $data['questions'];
    
    try {
        $db->beginTransaction();
        
        // First verify the course exists
        $checkStmt = $db->prepare("SELECT course_id, course_title FROM courses WHERE course_id = ?");
        $checkStmt->execute([$courseId]);
        $course = $checkStmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$course) {
            $db->rollBack();
            error_log("saveTestPaper: Course not found with ID: " . $courseId);
            APIResponse::error('Course not found', 404);
            return;
        }
        
        error_log("saveTestPaper: Saving test paper for course '" . $course['course_title'] . "' (ID: " . $courseId . ") with " . count($questions) . " questions");
        
        // Delete existing questions for this course (if updating)
        $deleteStmt = $db->prepare("DELETE FROM test_questions WHERE course_id = ?");
        $deleteStmt->execute([$courseId]);
        
        // Insert new questions
        $insertStmt = $db->prepare("
            INSERT INTO test_questions 
            (course_id, question_number, question_text, option_a, option_b, option_c, option_d, correct_answer) 
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ");
        
        $savedCount = 0;
        foreach ($questions as $question) {
            $questionNumber = intval($question['question_number']);
            $questionText = trim($question['question_text']);
            $optionA = trim($question['option_a']);
            $optionB = trim($question['option_b']);
            $optionC = trim($question['option_c']);
            $optionD = trim($question['option_d']);
            $correctAnswerRaw = trim($question['correct_answer']);
            
            // Convert 1,2,3,4 to A,B,C,D
            $answerMap = ['1' => 'A', '2' => 'B', '3' => 'C', '4' => 'D'];
            if (isset($answerMap[$correctAnswerRaw])) {
                $correctAnswer = $answerMap[$correctAnswerRaw];
            } else {
                $correctAnswer = strtoupper($correctAnswerRaw);
            }
            
            // Log the received correct answer for debugging
            error_log("saveTestPaper Q" . $questionNumber . ": correct_answer received = '" . $correctAnswerRaw . "', converted to = '" . $correctAnswer . "'");
            
            // Validate question data
            if (empty($questionText) || empty($optionA) || empty($optionB) || empty($optionC) || empty($optionD) || empty($correctAnswer)) {
                $db->rollBack();
                error_log("saveTestPaper: Invalid question data at question " . $questionNumber);
                APIResponse::error('Invalid question data for question ' . $questionNumber, 400);
                return;
            }
            
            // Validate correct answer is A, B, C, or D
            if (!in_array($correctAnswer, ['A', 'B', 'C', 'D'])) {
                $db->rollBack();
                error_log("saveTestPaper: Invalid correct_answer value: '" . $correctAnswer . "' (length: " . strlen($correctAnswer) . ")");
                APIResponse::error('Correct answer must be 1, 2, 3, or 4 (or A, B, C, D) for question ' . $questionNumber . '. You entered: "' . $correctAnswerRaw . '"', 400);
                return;
            }
            
            $insertStmt->execute([
                $courseId,
                $questionNumber,
                $questionText,
                $optionA,
                $optionB,
                $optionC,
                $optionD,
                $correctAnswer
            ]);
            
            $savedCount++;
        }
        
        // Mark the course as published since test paper is saved
        $updateStmt = $db->prepare("UPDATE courses SET is_published = 1 WHERE course_id = ?");
        $updateStmt->execute([$courseId]);
        
        $db->commit();
        
        error_log("saveTestPaper: Successfully saved " . $savedCount . " questions for course " . $courseId);
        
        APIResponse::success([
            'course_id' => $courseId,
            'questions_saved' => $savedCount,
            'is_published' => true
        ], 'Test paper saved successfully');
        
    } catch (PDOException $e) {
        if ($db->inTransaction()) {
            $db->rollBack();
        }
        error_log("saveTestPaper error: " . $e->getMessage());
        APIResponse::error('Failed to save test paper: ' . $e->getMessage(), 500);
    }
}

function getMentors($db) {
    try {
        // Check if category_id filter is provided
        $categoryId = isset($_GET['category_id']) ? intval($_GET['category_id']) : 0;

        $query = "
            SELECT DISTINCT
                u.user_id,
                u.full_name,
                u.email,
                u.specialization,
                u.experience_years,
                u.profile_image,
                u.bio,
                COUNT(DISTINCT c.course_id) AS course_count,
                COALESCE(AVG(cr.rating), 0) AS avg_rating,
                1 AS is_verified
            FROM users u
            INNER JOIN courses c ON u.user_id = c.teacher_id
            LEFT JOIN categories cat ON c.category_id = cat.category_id
            LEFT JOIN course_reviews cr ON c.course_id = cr.course_id
            WHERE u.user_type = 'teacher' AND c.is_published = 1
        ";
        
        // Add category filter if provided
        if ($categoryId > 0) {
            $query .= " AND c.category_id = :category_id";
        }
        
        $query .= " GROUP BY u.user_id ORDER BY course_count DESC, u.full_name ASC";
        
        $stmt = $db->prepare($query);
        
        if ($categoryId > 0) {
            $stmt->bindParam(':category_id', $categoryId, PDO::PARAM_INT);
        }
        
        $stmt->execute();
        $mentors = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        APIResponse::success($mentors, "Mentors retrieved successfully");
        
    } catch (PDOException $e) {
        error_log("Get mentors error: " . $e->getMessage());
        APIResponse::error("Failed to retrieve mentors: " . $e->getMessage());
    }
}

function getCompletedCourses($db) {
    $student_id = isset($_GET['student_id']) ? intval($_GET['student_id']) : 0;

    if ($student_id == 0) {
        APIResponse::error("Student ID is required");
        return;
    }

    try {
        $query = "SELECT c.course_id, c.course_title, c.course_description, c.course_image,
                         c.duration_hours, u.full_name as teacher_name,
                         COUNT(cl.lesson_id) as total_videos,
                         COUNT(CASE WHEN sp.is_completed = 1 THEN 1 END) as completed_videos
                  FROM enrollments e
                  INNER JOIN courses c ON e.course_id = c.course_id
                  LEFT JOIN users u ON c.teacher_id = u.user_id
                  LEFT JOIN course_lessons cl ON cl.course_id = c.course_id
                  LEFT JOIN student_progress sp ON sp.lesson_id = cl.lesson_id AND sp.student_id = :student_id
                  WHERE e.student_id = :student_id2
                  GROUP BY c.course_id, c.course_title, c.course_description, c.course_image, c.duration_hours, u.full_name
                  HAVING COUNT(cl.lesson_id) > 0 AND COUNT(CASE WHEN sp.is_completed = 1 THEN 1 END) = COUNT(cl.lesson_id)
                  ORDER BY e.enrollment_date DESC";

        $stmt = $db->prepare($query);
        $stmt->bindParam(":student_id", $student_id, PDO::PARAM_INT);
        $stmt->bindParam(":student_id2", $student_id, PDO::PARAM_INT);
        $stmt->execute();

        $courses = $stmt->fetchAll(PDO::FETCH_ASSOC);
        APIResponse::success($courses, "Completed courses retrieved successfully");
    } catch (PDOException $e) {
        error_log("getCompletedCourses error: " . $e->getMessage());
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function markVideoCompleted($db) {
    $data = json_decode(file_get_contents("php://input"), true);

    if (empty($data['student_id']) || empty($data['video_id']) || empty($data['course_id'])) {
        APIResponse::error("student_id, video_id and course_id are required");
        return;
    }

    $student_id = intval($data['student_id']);
    $lesson_id  = intval($data['video_id']);
    $course_id  = intval($data['course_id']);

    try {
        // Insert or update student_progress
        $checkStmt = $db->prepare("SELECT progress_id FROM student_progress WHERE student_id = ? AND lesson_id = ?");
        $checkStmt->execute([$student_id, $lesson_id]);

        if ($checkStmt->rowCount() > 0) {
            $upd = $db->prepare("UPDATE student_progress SET is_completed = 1 WHERE student_id = ? AND lesson_id = ?");
            $upd->execute([$student_id, $lesson_id]);
        } else {
            $ins = $db->prepare("INSERT INTO student_progress (student_id, lesson_id, is_completed) VALUES (?, ?, 1)");
            $ins->execute([$student_id, $lesson_id]);
        }

        // Calculate new completion percentage
        $totalStmt = $db->prepare("SELECT COUNT(*) as total FROM course_lessons WHERE course_id = ?");
        $totalStmt->execute([$course_id]);
        $total = $totalStmt->fetchColumn();

        $doneStmt = $db->prepare("SELECT COUNT(*) as done FROM student_progress sp
                                   INNER JOIN course_lessons cl ON sp.lesson_id = cl.lesson_id
                                   WHERE sp.student_id = ? AND cl.course_id = ? AND sp.is_completed = 1");
        $doneStmt->execute([$student_id, $course_id]);
        $done = $doneStmt->fetchColumn();

        $percentage = ($total > 0) ? round(($done / $total) * 100) : 0;

        $updEnroll = $db->prepare("UPDATE enrollments SET completion_percentage = ? WHERE student_id = ? AND course_id = ?");
        $updEnroll->execute([$percentage, $student_id, $course_id]);

        APIResponse::success([
            'completed_lessons' => $done,
            'total_lessons'     => $total,
            'completion_percentage' => $percentage
        ], "Progress saved");
    } catch (PDOException $e) {
        error_log("markVideoCompleted error: " . $e->getMessage());
        APIResponse::error("Database error: " . $e->getMessage());
    }
}

function getVideoProgress($db) {
    $student_id = isset($_GET['student_id']) ? intval($_GET['student_id']) : 0;
    $course_id  = isset($_GET['course_id'])  ? intval($_GET['course_id'])  : 0;

    if ($student_id == 0 || $course_id == 0) {
        APIResponse::error("student_id and course_id are required");
        return;
    }

    try {
        $stmt = $db->prepare("SELECT sp.lesson_id FROM student_progress sp
                               INNER JOIN course_lessons cl ON sp.lesson_id = cl.lesson_id
                               WHERE sp.student_id = ? AND cl.course_id = ? AND sp.is_completed = 1");
        $stmt->execute([$student_id, $course_id]);
        $rows = $stmt->fetchAll(PDO::FETCH_COLUMN);

        $totalStmt = $db->prepare("SELECT COUNT(*) FROM course_lessons WHERE course_id = ?");
        $totalStmt->execute([$course_id]);
        $total = $totalStmt->fetchColumn();

        $percentage = ($total > 0) ? round((count($rows) / $total) * 100) : 0;

        APIResponse::success([
            'completed_lessons'     => $rows,
            'total_lessons'         => $total,
            'completion_percentage' => $percentage
        ], "Progress retrieved");
    } catch (PDOException $e) {
        error_log("getVideoProgress error: " . $e->getMessage());
        APIResponse::error("Database error: " . $e->getMessage());
    }
}
?>
