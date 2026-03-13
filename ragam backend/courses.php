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
        } elseif($action == 'get_certificates') {
            getCertificates($db);
        } elseif($action == 'completed_courses') {
            getCompletedCourses($db);
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
        } elseif($action == 'submit_quiz') {
            submitQuiz($db);
        } elseif($action == 'save_certificate') {
            saveCertificate($db);
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
                  LEFT JOIN categories cat ON c.category_id = cat.category_id
                  WHERE c.course_id = :course_id";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":course_id", $course_id);
        $stmt->execute();
        
        if($stmt->rowCount() > 0) {
            $course = $stmt->fetch(PDO::FETCH_ASSOC);
            error_log("getCourseDetails: Successfully retrieved course ID " . $course_id . " - " . $course['course_title']);
            APIResponse::success($course, "Course details retrieved successfully");
        } else {
            error_log("getCourseDetails: Course not found with ID: " . $course_id);
            APIResponse::error("Course not found");
        }
    } catch(PDOException $e) {
        error_log("getCourseDetails error: " . $e->getMessage());
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
        error_log("getEnrolledCourses: Invalid student_id");
        APIResponse::error("Student ID is required");
        return;
    }
    
    try {
        // First, verify student exists
        $checkStmt = $db->prepare("SELECT user_id FROM users WHERE user_id = :student_id");
        $checkStmt->bindParam(":student_id", $student_id, PDO::PARAM_INT);
        $checkStmt->execute();
        
        if($checkStmt->rowCount() == 0) {
            error_log("getEnrolledCourses: Student not found with ID: " . $student_id);
            APIResponse::error("Student not found");
            return;
        }
        
        $query = "SELECT c.course_id, c.course_title, c.course_description, c.course_image, 
                         c.duration_hours, u.full_name as teacher_name,
                         DATE_FORMAT(e.enrollment_date, '%Y-%m-%d %H:%i') as enrollment_date, 
                         e.completion_percentage
                  FROM enrollments e
                  INNER JOIN courses c ON e.course_id = c.course_id
                  LEFT JOIN users u ON c.teacher_id = u.user_id
                  LEFT JOIN certificates cert ON e.student_id = cert.student_id AND e.course_id = cert.course_id
                  WHERE e.student_id = :student_id AND e.completion_percentage < 100 AND cert.certificate_id IS NULL
                  ORDER BY e.enrollment_date DESC";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":student_id", $student_id, PDO::PARAM_INT);
        $stmt->execute();
        
        $courses = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        error_log("getEnrolledCourses: Found " . count($courses) . " courses for student " . $student_id);
        
        // Calculate progress for each course
        foreach($courses as &$course) {
            try {
                // Check if video_progress table exists
                $tableCheck = $db->query("SHOW TABLES LIKE 'video_progress'");
                $videoProgressExists = $tableCheck->rowCount() > 0;
                
                if($videoProgressExists) {
                    $videoQuery = "SELECT COUNT(*) as total_videos,
                                          COALESCE(SUM(CASE WHEN vp.completed = 1 THEN 1 ELSE 0 END), 0) as completed_videos
                                   FROM course_lessons cl
                                   LEFT JOIN video_progress vp ON cl.lesson_id = vp.lesson_id 
                                                                AND vp.student_id = :student_id
                                   WHERE cl.course_id = :course_id";
                } else {
                    // Fallback if video_progress doesn't exist
                    $videoQuery = "SELECT COUNT(*) as total_videos, 0 as completed_videos
                                   FROM course_lessons cl
                                   WHERE cl.course_id = :course_id";
                }
                
                $videoStmt = $db->prepare($videoQuery);
                $videoStmt->bindParam(":student_id", $student_id, PDO::PARAM_INT);
                $videoStmt->bindParam(":course_id", $course['course_id'], PDO::PARAM_INT);
                $videoStmt->execute();
                $videoData = $videoStmt->fetch(PDO::FETCH_ASSOC);
                
                $total_videos = intval($videoData['total_videos']);
                $completed_videos = intval($videoData['completed_videos']);
                
                // Check quiz results if table exists
                $quizTableCheck = $db->query("SHOW TABLES LIKE 'quiz_results'");
                $quizTableExists = $quizTableCheck->rowCount() > 0;
                $quiz_passed = false;
                
                if($quizTableExists) {
                    $quizQuery = "SELECT passed FROM quiz_results 
                                 WHERE student_id = :student_id AND course_id = :course_id 
                                 ORDER BY attempt_date DESC LIMIT 1";
                    
                    $quizStmt = $db->prepare($quizQuery);
                    $quizStmt->bindParam(":student_id", $student_id, PDO::PARAM_INT);
                    $quizStmt->bindParam(":course_id", $course['course_id'], PDO::PARAM_INT);
                    $quizStmt->execute();
                    $quizData = $quizStmt->fetch(PDO::FETCH_ASSOC);
                    
                    $quiz_passed = ($quizData && $quizData['passed'] == 1);
                }
                
                // Calculate progress
                $video_progress = 0;
                if($total_videos > 0) {
                    $video_progress = ($completed_videos / $total_videos) * 70;
                }
                
                $quiz_progress = $quiz_passed ? 30 : 0;
                $total_progress = round($video_progress + $quiz_progress);
                
                $course['progress'] = $total_progress;
                $course['completed_videos'] = $completed_videos;
                $course['total_videos'] = $total_videos;
                
            } catch(Exception $e) {
                error_log("getEnrolledCourses: Error calculating progress for course " . $course['course_id'] . ": " . $e->getMessage());
                $course['progress'] = 0;
                $course['completed_videos'] = 0;
                $course['total_videos'] = 0;
            }
        }
        
        APIResponse::success($courses, "Enrolled courses retrieved successfully");
    } catch(PDOException $e) {
        error_log("getEnrolledCourses: Database error - " . $e->getMessage());
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
        
        // Create test_questions table if not exists
        $createTable = "CREATE TABLE IF NOT EXISTS test_questions (
            question_id INT AUTO_INCREMENT PRIMARY KEY,
            course_id INT NOT NULL,
            question_number INT NOT NULL,
            question_text TEXT NOT NULL,
            option_a VARCHAR(255) NOT NULL,
            option_b VARCHAR(255) NOT NULL,
            option_c VARCHAR(255) NOT NULL,
            option_d VARCHAR(255) NOT NULL,
            correct_answer CHAR(1) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            INDEX idx_course (course_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        try {
            $db->exec($createTable);
        } catch (PDOException $tableError) {
            error_log("getTestPaper: Table creation note: " . $tableError->getMessage());
        }
        
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
            // Return empty array with success so app shows "No quiz available"
            APIResponse::success([], 'No test questions available for this course yet');
            return;
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

function submitQuiz($db) {
    $data = json_decode(file_get_contents("php://input"), true);
    
    if (empty($data['student_id']) || empty($data['course_id'])) {
        APIResponse::error('Student ID and Course ID are required', 400);
        return;
    }
    
    $studentId = intval($data['student_id']);
    $courseId = intval($data['course_id']);
    $score = intval($data['score']);
    $totalQuestions = intval($data['total_questions']);
    $percentage = floatval($data['percentage']);
    $passed = ($percentage >= 50) ? 1 : 0;
    
    try {
        // Create quiz_results table if not exists
        $createTable = "CREATE TABLE IF NOT EXISTS quiz_results (
            result_id INT AUTO_INCREMENT PRIMARY KEY,
            student_id INT NOT NULL,
            course_id INT NOT NULL,
            score INT NOT NULL,
            total_questions INT NOT NULL,
            percentage DECIMAL(5,2) NOT NULL,
            passed TINYINT(1) DEFAULT 0,
            attempts INT DEFAULT 1,
            attempt_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY unique_result (student_id, course_id),
            FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE,
            FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        $db->exec($createTable);
        
        // Insert or update quiz result
        $query = "INSERT INTO quiz_results (student_id, course_id, score, total_questions, percentage, passed, attempts)
                  VALUES (?, ?, ?, ?, ?, ?, 1)
                  ON DUPLICATE KEY UPDATE 
                  score = VALUES(score),
                  total_questions = VALUES(total_questions),
                  percentage = VALUES(percentage),
                  passed = VALUES(passed),
                  attempts = attempts + 1,
                  attempt_date = CURRENT_TIMESTAMP";
        
        $stmt = $db->prepare($query);
        $stmt->execute([$studentId, $courseId, $score, $totalQuestions, $percentage, $passed]);
        
        // Update enrollment completion if passed
        if ($passed) {
            $enrollStmt = $db->prepare("UPDATE enrollments SET completion_percentage = 100 WHERE student_id = ? AND course_id = ?");
            $enrollStmt->execute([$studentId, $courseId]);
            
            // Auto-save certificate when quiz is passed
            try {
                // Get course title
                $courseStmt = $db->prepare("SELECT course_title FROM courses WHERE course_id = ?");
                $courseStmt->execute([$courseId]);
                $courseData = $courseStmt->fetch(PDO::FETCH_ASSOC);
                $courseTitle = $courseData ? $courseData['course_title'] : 'Unknown Course';
                
                // Create certificates table if not exists (without foreign keys)
                $createCertTable = "CREATE TABLE IF NOT EXISTS certificates (
                    certificate_id INT AUTO_INCREMENT PRIMARY KEY,
                    student_id INT NOT NULL,
                    course_id INT NOT NULL,
                    course_title VARCHAR(255) NOT NULL,
                    score INT NOT NULL,
                    total_questions INT NOT NULL,
                    correct_answers INT NOT NULL,
                    issue_date DATE NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY unique_cert (student_id, course_id),
                    INDEX idx_student (student_id),
                    INDEX idx_course (course_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
                
                try {
                    $db->exec($createCertTable);
                } catch (PDOException $certTableError) {
                    // Table might already exist, continue
                    error_log("submitQuiz: Certificate table create warning: " . $certTableError->getMessage());
                }
                
                // Save certificate
                $correctAnswers = intval(($percentage / 100) * $totalQuestions);
                $issueDate = date('Y-m-d');
                
                $certQuery = "INSERT INTO certificates 
                              (student_id, course_id, course_title, score, total_questions, correct_answers, issue_date)
                              VALUES (?, ?, ?, ?, ?, ?, ?)
                              ON DUPLICATE KEY UPDATE 
                              score = VALUES(score), 
                              total_questions = VALUES(total_questions), 
                              correct_answers = VALUES(correct_answers),
                              issue_date = VALUES(issue_date)";
                
                $certStmt = $db->prepare($certQuery);
                $certStmt->execute([$studentId, $courseId, $courseTitle, intval($percentage), $totalQuestions, $correctAnswers, $issueDate]);
                
                error_log("Certificate saved for student $studentId, course $courseId");
            } catch (Exception $certError) {
                error_log("Failed to save certificate: " . $certError->getMessage());
            }
        }
        
        error_log("Quiz submitted for student $studentId, course $courseId - Score: $percentage% (" . ($passed ? 'PASSED' : 'FAILED') . ")");
        APIResponse::success(['passed' => $passed, 'score' => $score, 'percentage' => $percentage], 'Quiz submitted successfully');
        
    } catch (PDOException $e) {
        error_log("submitQuiz error: " . $e->getMessage());
        APIResponse::error('Failed to submit quiz: ' . $e->getMessage(), 500);
    }
}

function saveCertificate($db) {
    $data = json_decode(file_get_contents("php://input"), true);
    
    if (empty($data['student_id']) || empty($data['course_id'])) {
        APIResponse::error('Student ID and Course ID are required', 400);
        return;
    }
    
    $studentId = intval($data['student_id']);
    $courseId = intval($data['course_id']);
    $courseTitle = $data['course_title'] ?? 'Unknown Course';
    $score = intval($data['score']);
    $totalQuestions = intval($data['total_questions']);
    $correctAnswers = intval($data['correct_answers']);
    $issueDate = date('Y-m-d');
    
    try {
        // Create certificates table if not exists (without foreign keys to avoid issues)
        $createTable = "CREATE TABLE IF NOT EXISTS certificates (
            certificate_id INT AUTO_INCREMENT PRIMARY KEY,
            student_id INT NOT NULL,
            course_id INT NOT NULL,
            course_title VARCHAR(255) NOT NULL,
            score INT NOT NULL,
            total_questions INT NOT NULL,
            correct_answers INT NOT NULL,
            issue_date DATE NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY unique_cert (student_id, course_id),
            INDEX idx_student (student_id),
            INDEX idx_course (course_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        try {
            $db->exec($createTable);
        } catch (PDOException $createError) {
            // Table might already exist, continue
            error_log("saveCertificate: Table create warning (might exist): " . $createError->getMessage());
        }
        
        // Insert or update certificate
        $query = "INSERT INTO certificates 
                  (student_id, course_id, course_title, score, total_questions, correct_answers, issue_date)
                  VALUES (?, ?, ?, ?, ?, ?, ?)
                  ON DUPLICATE KEY UPDATE 
                  score = VALUES(score), 
                  total_questions = VALUES(total_questions), 
                  correct_answers = VALUES(correct_answers),
                  issue_date = VALUES(issue_date)";
        
        $stmt = $db->prepare($query);
        $stmt->execute([$studentId, $courseId, $courseTitle, $score, $totalQuestions, $correctAnswers, $issueDate]);
        
        error_log("Certificate manually saved for student $studentId, course $courseId");
        APIResponse::success(['certificate_id' => $db->lastInsertId()], 'Certificate saved successfully');
        
    } catch (PDOException $e) {
        error_log("saveCertificate error: " . $e->getMessage());
        APIResponse::error('Failed to save certificate: ' . $e->getMessage(), 500);
    }
}

function getCertificates($db) {
    $studentId = isset($_GET['student_id']) ? intval($_GET['student_id']) : 0;
    
    if ($studentId <= 0) {
        error_log("getCertificates: Invalid student_id provided");
        APIResponse::error('Valid Student ID is required', 400);
        return;
    }
    
    try {
        // Check if student exists
        $checkStmt = $db->prepare("SELECT user_id FROM users WHERE user_id = ?");
        $checkStmt->execute([$studentId]);
        
        if ($checkStmt->rowCount() == 0) {
            error_log("getCertificates: Student not found with ID: " . $studentId);
            // Return empty array instead of error
            APIResponse::success([], "No certificates found");
            return;
        }
        
        // Create certificates table if not exists (without foreign keys to avoid issues)
        $createTable = "CREATE TABLE IF NOT EXISTS certificates (
            certificate_id INT AUTO_INCREMENT PRIMARY KEY,
            student_id INT NOT NULL,
            course_id INT NOT NULL,
            course_title VARCHAR(255) NOT NULL,
            score INT NOT NULL,
            total_questions INT NOT NULL,
            correct_answers INT NOT NULL,
            issue_date DATE NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE KEY unique_cert (student_id, course_id),
            INDEX idx_student (student_id),
            INDEX idx_course (course_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
        
        try {
            $db->exec($createTable);
        } catch (PDOException $createError) {
            // Table might already exist, continue
            error_log("getCertificates: Table create warning (might exist): " . $createError->getMessage());
        }
        
        // Get all certificates for the student
        $query = "SELECT c.certificate_id, c.student_id, c.course_id, c.course_title, 
                         c.score, c.total_questions, c.correct_answers, c.issue_date,
                         c.created_at, co.course_description, co.course_image
                  FROM certificates c
                  LEFT JOIN courses co ON c.course_id = co.course_id
                  WHERE c.student_id = ?
                  ORDER BY c.issue_date DESC";
        
        $stmt = $db->prepare($query);
        $stmt->execute([$studentId]);
        
        $certificates = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        error_log("getCertificates: Found " . count($certificates) . " certificates for student " . $studentId);
        
        // Return success even if no certificates found
        APIResponse::success($certificates, "Certificates retrieved successfully");
        
    } catch (PDOException $e) {
        error_log("getCertificates error: " . $e->getMessage());
        error_log("getCertificates stack trace: " . $e->getTraceAsString());
        // Return empty array instead of error to prevent app crash
        APIResponse::success([], "Error retrieving certificates");
    }
}

function getCompletedCourses($db) {
    $student_id = isset($_GET['student_id']) ? intval($_GET['student_id']) : 0;
    
    if($student_id == 0) {
        error_log("getCompletedCourses: Invalid student_id");
        APIResponse::error("Student ID is required");
        return;
    }
    
    try {
        // Verify student exists
        $checkStmt = $db->prepare("SELECT user_id FROM users WHERE user_id = :student_id");
        $checkStmt->bindParam(":student_id", $student_id, PDO::PARAM_INT);
        $checkStmt->execute();
        
        if($checkStmt->rowCount() == 0) {
            error_log("getCompletedCourses: Student not found with ID: " . $student_id);
            APIResponse::error("Student not found");
            return;
        }
        
        // Get only completed courses (completion_percentage = 100 OR has certificate)
        $query = "SELECT c.course_id, c.course_title, c.course_description, c.course_image, 
                         c.duration_hours, u.full_name as teacher_name,
                         DATE_FORMAT(e.enrollment_date, '%Y-%m-%d %H:%i') as enrollment_date, 
                         e.completion_percentage
                  FROM enrollments e
                  INNER JOIN courses c ON e.course_id = c.course_id
                  LEFT JOIN users u ON c.teacher_id = u.user_id
                  LEFT JOIN certificates cert ON e.student_id = cert.student_id AND e.course_id = cert.course_id
                  WHERE e.student_id = :student_id AND (e.completion_percentage = 100 OR cert.certificate_id IS NOT NULL)
                  ORDER BY e.enrollment_date DESC";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":student_id", $student_id, PDO::PARAM_INT);
        $stmt->execute();
        
        $courses = $stmt->fetchAll(PDO::FETCH_ASSOC);
        
        error_log("getCompletedCourses: Found " . count($courses) . " completed courses for student " . $student_id);
        
        // Calculate progress for each course (should all be 100%)
        foreach($courses as &$course) {
            try {
                // Check if video_progress table exists
                $tableCheck = $db->query("SHOW TABLES LIKE 'video_progress'");
                $videoProgressExists = $tableCheck->rowCount() > 0;
                
                if($videoProgressExists) {
                    $videoQuery = "SELECT COUNT(*) as total_videos,
                                          COALESCE(SUM(CASE WHEN vp.completed = 1 THEN 1 ELSE 0 END), 0) as completed_videos
                                   FROM course_lessons cl
                                   LEFT JOIN video_progress vp ON cl.lesson_id = vp.lesson_id 
                                                                AND vp.student_id = :student_id
                                   WHERE cl.course_id = :course_id";
                } else {
                    $videoQuery = "SELECT COUNT(*) as total_videos, 0 as completed_videos
                                   FROM course_lessons cl
                                   WHERE cl.course_id = :course_id";
                }
                
                $videoStmt = $db->prepare($videoQuery);
                $videoStmt->bindParam(":student_id", $student_id, PDO::PARAM_INT);
                $videoStmt->bindParam(":course_id", $course['course_id'], PDO::PARAM_INT);
                $videoStmt->execute();
                $videoData = $videoStmt->fetch(PDO::FETCH_ASSOC);
                
                $total_videos = intval($videoData['total_videos']);
                $completed_videos = intval($videoData['completed_videos']);
                
                // Check quiz results
                $quizTableCheck = $db->query("SHOW TABLES LIKE 'quiz_results'");
                $quizTableExists = $quizTableCheck->rowCount() > 0;
                $quiz_passed = false;
                
                if($quizTableExists) {
                    $quizQuery = "SELECT passed FROM quiz_results 
                                 WHERE student_id = :student_id AND course_id = :course_id 
                                 ORDER BY attempt_date DESC LIMIT 1";
                    
                    $quizStmt = $db->prepare($quizQuery);
                    $quizStmt->bindParam(":student_id", $student_id, PDO::PARAM_INT);
                    $quizStmt->bindParam(":course_id", $course['course_id'], PDO::PARAM_INT);
                    $quizStmt->execute();
                    $quizData = $quizStmt->fetch(PDO::FETCH_ASSOC);
                    
                    $quiz_passed = ($quizData && $quizData['passed'] == 1);
                }
                
                $course['progress'] = 100;
                $course['completed_videos'] = $completed_videos;
                $course['total_videos'] = $total_videos;
                $course['quiz_passed'] = $quiz_passed;
                
            } catch(Exception $e) {
                error_log("getCompletedCourses: Error calculating details for course " . $course['course_id'] . ": " . $e->getMessage());
                $course['progress'] = 100;
                $course['completed_videos'] = 0;
                $course['total_videos'] = 0;
                $course['quiz_passed'] = false;
            }
        }
        
        APIResponse::success($courses, "Completed courses retrieved successfully");
    } catch(PDOException $e) {
        error_log("getCompletedCourses: Database error - " . $e->getMessage());
        APIResponse::error("Database error: " . $e->getMessage());
    }
}
?>
