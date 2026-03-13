<?php
/**
 * Courses API - MySQL Version
 * Handles all course-related operations using MySQL database
 */
require_once 'config_mysql.php';
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}
$action = isset($_GET['action']) ? $_GET['action'] : '';
// ============ GET ALL COURSES ============
if ($action == 'all_courses') {
    try {
        $pdo = getDBConnection();
        // Check if category_id filter is provided
        $category_id = isset($_GET['category_id']) ? intval($_GET['category_id']) : 0;
        $sql = "
            SELECT
                c.course_id,
                c.course_title,
                c.course_description,
                c.course_price,
                c.difficulty_level,
                c.is_free,
                c.enrollment_count,
                c.rating,
                c.created_at,
                u.full_name AS teacher_name,
                u.user_id AS instructor_id,
                u.specialization,
                cat.category_id,
                cat.category_name,
                COUNT(DISTINCT cl.lesson_id) AS video_count
            FROM courses c
            LEFT JOIN users u ON c.teacher_id = u.user_id
            LEFT JOIN categories cat ON c.category_id = cat.category_id
            LEFT JOIN course_lessons cl ON c.course_id = cl.course_id
            WHERE c.is_published = 1
        ";
        // Add category filter if provided
        if ($category_id > 0) {
            $sql .= " AND c.category_id = " . $category_id;
        }
        $sql .= " GROUP BY c.course_id ORDER BY c.created_at DESC";
        $stmt = $pdo->query($sql);
        $courses = $stmt->fetchAll();
        // Format data - keep consistent field names
        foreach ($courses as &$course) {
            // Keep course_title field for Android compatibility
            $course['price_formatted'] = $course['is_free'] ? 'Free' : 'â‚¹' . number_format($course['course_price'], 2);
            $course['rating_formatted'] = number_format($course['rating'], 1) . 'â˜…';
        }
        sendSuccess($courses, 'Courses retrieved successfully');
    } catch (PDOException $e) {
        error_log("Get courses error: " . $e->getMessage());
        sendError('Failed to retrieve courses: ' . $e->getMessage(), 500);
    }
}
// ============ CREATE COURSE ============
else if ($action == 'create_course') {
    $rawInput = file_get_contents("php://input");
    $data = json_decode($rawInput, true);
    $teacher_id = isset($data['instructor_id']) ? intval($data['instructor_id']) : 0;
    $title = isset($data['course_title']) ? trim($data['course_title']) : '';
    $description = isset($data['course_description']) ? trim($data['course_description']) : '';
    $category_name = isset($data['category']) ? trim($data['category']) : '';
    $category_id = isset($data['category_id']) ? intval($data['category_id']) : 0;
    if (empty($title) || $teacher_id <= 0) {
        sendError('Course title and instructor ID are required', 400);
    }
    try {
        $pdo = getDBConnection();
        $pdo->beginTransaction();
        // If category_id provided, use it; otherwise lookup/create by name
        if ($category_id > 0) {
            $stmt = $pdo->prepare("SELECT category_id, category_name FROM categories WHERE category_id = ?");
            $stmt->execute([$category_id]);
            $category = $stmt->fetch();
            if (!$category) {
                sendError('Invalid category ID', 400);
            }
            $category_name = $category['category_name'];
        } else if (!empty($category_name)) {
            // Get or create category by name
            $stmt = $pdo->prepare("SELECT category_id FROM categories WHERE category_name = ?");
            $stmt->execute([$category_name]);
            $category = $stmt->fetch();
            if (!$category) {
                $stmt = $pdo->prepare("INSERT INTO categories (category_name, is_active) VALUES (?, 1)");
                $stmt->execute([$category_name]);
                $category_id = $pdo->lastInsertId();
            } else {
                $category_id = $category['category_id'];
            }
        } else {
            sendError('Category is required', 400);
        }
        // Insert course
        $stmt = $pdo->prepare("
            INSERT INTO courses (teacher_id, category_id, course_title, course_description,
                                is_free, is_published, difficulty_level) 
            VALUES (?, ?, ?, ?, 1, 1, 'beginner')
        ");
        $stmt->execute([$teacher_id, $category_id, $title, $description]);
        $course_id = $pdo->lastInsertId();
        $pdo->commit();
        sendSuccess([
            'course_id' => $course_id,
            'course_title' => $title,
            'course_description' => $description,
            'category' => $category_name,
            'category_id' => $category_id,
            'instructor_id' => $teacher_id
        ], 'Course created successfully');
    } catch (PDOException $e) {
        if ($pdo->inTransaction()) {
            $pdo->rollBack();
        }
        error_log("Create course error: " . $e->getMessage());
        sendError('Failed to create course: ' . $e->getMessage(), 500);
    }
}
// ============ UPLOAD VIDEO (Metadata) ============
else if ($action == 'upload_video') {
    $rawInput = file_get_contents("php://input");
    $data = json_decode($rawInput, true);
    $course_id = isset($data['course_id']) ? intval($data['course_id']) : 0;
    $title = isset($data['video_title']) ? trim($data['video_title']) : '';
    $description = isset($data['video_description']) ? trim($data['video_description']) : '';
    $video_url = isset($data['video_url']) ? trim($data['video_url']) : '';
    $video_path = isset($data['video_path']) ? trim($data['video_path']) : '';
    if ($course_id <= 0 || empty($title)) {
        sendError('Course ID and video title are required', 400);
    }
    try {
        $pdo = getDBConnection();
        // Get next lesson order
        $stmt = $pdo->prepare("SELECT MAX(lesson_order) as max_order FROM course_lessons WHERE course_id = ?");
        $stmt->execute([$course_id]);
        $result = $stmt->fetch();
        $lesson_order = ($result['max_order'] ?? 0) + 1;
        // Determine final video URL
        $final_video_url = !empty($video_url) ? $video_url :
                          (!empty($video_path) ? $video_path : '');
        // Insert lesson
        $stmt = $pdo->prepare("
            INSERT INTO course_lessons (course_id, lesson_title, lesson_description,
                                       video_url, lesson_order, is_free)
            VALUES (?, ?, ?, ?, ?, 1)
        ");
        $stmt->execute([$course_id, $title, $description, $final_video_url, $lesson_order]);
        $lesson_id = $pdo->lastInsertId();
        // Update course video count
        $pdo->exec("UPDATE courses SET duration_hours = (
            SELECT COUNT(*) FROM course_lessons WHERE course_id = $course_id
        ) WHERE course_id = $course_id");
        sendSuccess([
            'lesson_id' => $lesson_id,
            'video_id' => $lesson_id,
            'course_id' => $course_id,
            'title' => $title,
            'description' => $description,
            'video_url' => $final_video_url,
            'lesson_order' => $lesson_order
        ], 'Video saved successfully');
    } catch (PDOException $e) {
        error_log("Upload video error: " . $e->getMessage());
        sendError('Failed to save video: ' . $e->getMessage(), 500);
    }
}
// ============ GET COURSE VIDEOS ============
else if ($action == 'get_course_videos') {
    $course_id = isset($_GET['course_id']) ? intval($_GET['course_id']) : 0;
    if ($course_id <= 0) {
        sendError('Course ID is required', 400);
    }
    try {
        $pdo = getDBConnection();
        $stmt = $pdo->prepare("
            SELECT
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
            WHERE course_id = ?
            ORDER BY lesson_order ASC
        ");
        $stmt->execute([$course_id]);
        $videos = $stmt->fetchAll();
        sendSuccess($videos, 'Videos retrieved successfully');
    } catch (PDOException $e) {
        error_log("Get videos error: " . $e->getMessage());
        sendError('Failed to retrieve videos: ' . $e->getMessage(), 500);
    }
}
// ============ ENROLL IN COURSE ============
else if ($action == 'enroll') {
    $rawInput = file_get_contents("php://input");
    $data = json_decode($rawInput, true);
    $student_id = isset($data['student_id']) ? intval($data['student_id']) : 0;
    $course_id = isset($data['course_id']) ? intval($data['course_id']) : 0;
    if ($student_id <= 0 || $course_id <= 0) {
        sendError('Student ID and Course ID are required', 400);
    }
    try {
        $pdo = getDBConnection();
        // Check if already enrolled
        $stmt = $pdo->prepare("
            SELECT enrollment_id FROM enrollments
            WHERE student_id = ? AND course_id = ?
        ");
        $stmt->execute([$student_id, $course_id]);
        if ($existing = $stmt->fetch()) {
            sendSuccess($existing, 'Already enrolled');
            return;
        }
        // Insert enrollment
        $stmt = $pdo->prepare("
            INSERT INTO enrollments (student_id, course_id, payment_status, completion_percentage)
            VALUES (?, ?, 'completed', 0.00)
        ");
        $stmt->execute([$student_id, $course_id]);
        $enrollment_id = $pdo->lastInsertId();
        // Update enrollment count
        $pdo->exec("UPDATE courses SET enrollment_count = enrollment_count + 1 WHERE course_id = $course_id");
        sendSuccess([
            'enrollment_id' => $enrollment_id,
            'student_id' => $student_id,
            'course_id' => $course_id,
            'enrolled_at' => date('Y-m-d H:i:s')
        ], 'Enrolled successfully');
    } catch (PDOException $e) {
        error_log("Enrollment error: " . $e->getMessage());
        sendError('Failed to enroll: ' . $e->getMessage(), 500);
    }
}
// ============ GET ENROLLED COURSES ============
else if ($action == 'get_enrolled_courses') {
    $student_id = isset($_GET['student_id']) ? intval($_GET['student_id']) : 0;
    if ($student_id <= 0) {
        sendError('Student ID is required', 400);
    }
    try {
        $pdo = getDBConnection();
        $stmt = $pdo->prepare("
            SELECT
                c.course_id,
                c.course_title,
                c.course_description,
                c.course_price,
                c.rating,
                u.full_name AS teacher_name,
                u.user_id AS instructor_id,
                cat.category_id,
                cat.category_name,
                e.enrollment_id,
                e.enrollment_date AS enrolled_at,
                e.completion_percentage AS progress,
                COUNT(DISTINCT cl.lesson_id) AS video_count,
                COUNT(DISTINCT sp.progress_id) AS completed_videos
            FROM enrollments e
            JOIN courses c ON e.course_id = c.course_id
            LEFT JOIN users u ON c.teacher_id = u.user_id
            LEFT JOIN categories cat ON c.category_id = cat.category_id
            LEFT JOIN course_lessons cl ON c.course_id = cl.course_id
            LEFT JOIN student_progress sp ON cl.lesson_id = sp.lesson_id AND sp.student_id = e.student_id AND sp.is_completed = 1
            WHERE e.student_id = ?
            GROUP BY c.course_id, e.enrollment_id
            ORDER BY e.enrollment_date DESC
        ");
        $stmt->execute([$student_id]);
        $courses = $stmt->fetchAll();
        sendSuccess($courses, 'Enrolled courses retrieved successfully');
    } catch (PDOException $e) {
        error_log("Get enrolled courses error: " . $e->getMessage());
        sendError('Failed to retrieve enrolled courses: ' . $e->getMessage(), 500);
    }
}
// ============ GET COURSES BY TEACHER ============
else if ($action == 'get_teacher_courses') {
    $teacher_id = isset($_GET['teacher_id']) ? intval($_GET['teacher_id']) : 0;
    if ($teacher_id <= 0) {
        sendError('Teacher ID is required', 400);
    }
    try {
        $pdo = getDBConnection();
        $stmt = $pdo->prepare("
            SELECT
                c.course_id,
                c.course_title,
                c.course_description,
                c.course_price,
                c.difficulty_level,
                c.is_free,
                c.is_published,
                c.enrollment_count,
                c.rating,
                c.created_at,
                cat.category_id,
                cat.category_name,
                COUNT(DISTINCT cl.lesson_id) AS video_count
            FROM courses c
            LEFT JOIN categories cat ON c.category_id = cat.category_id
            LEFT JOIN course_lessons cl ON c.course_id = cl.course_id
            WHERE c.teacher_id = ?
            GROUP BY c.course_id
            ORDER BY c.created_at DESC
        ");
        $stmt->execute([$teacher_id]);
        $courses = $stmt->fetchAll();
        sendSuccess($courses, 'Teacher courses retrieved successfully');
    } catch (PDOException $e) {
        error_log("Get teacher courses error: " . $e->getMessage());
        sendError('Failed to retrieve teacher courses: ' . $e->getMessage(), 500);
    }
}
// ============ GET MENTORS BY CATEGORY ============
else if ($action == 'get_mentors') {
    try {
        $pdo = getDBConnection();
        // Check if category_id filter is provided
        $category_id = isset($_GET['category_id']) ? intval($_GET['category_id']) : 0;
        $sql = "
            SELECT DISTINCT
                u.user_id,
                u.full_name,
                u.email,
                u.specialization,
                u.experience_years,
                u.profile_image,
                u.bio,
                COUNT(DISTINCT c.course_id) AS course_count,
                GROUP_CONCAT(DISTINCT cat.category_name) AS categories
            FROM users u
            INNER JOIN courses c ON u.user_id = c.teacher_id
            LEFT JOIN categories cat ON c.category_id = cat.category_id
            WHERE u.user_type = 'teacher' AND c.is_published = 1
        ";
        // Add category filter if provided
        if ($category_id > 0) {
            $sql .= " AND c.category_id = " . $category_id;
        }
        $sql .= " GROUP BY u.user_id ORDER BY course_count DESC, u.full_name ASC";
        $stmt = $pdo->query($sql);
        $mentors = $stmt->fetchAll();
        sendSuccess($mentors, 'Mentors retrieved successfully');
    } catch (PDOException $e) {
        error_log("Get mentors error: " . $e->getMessage());
        sendError('Failed to retrieve mentors: ' . $e->getMessage(), 500);
    }
}
// ============ SAVE TEST PAPER ============
else if ($action == 'save_test_paper') {
    $rawInput = file_get_contents("php://input");
    $data = json_decode($rawInput, true);
    // For now, just return success (test papers need separate table implementation)
    sendSuccess([
        'test_paper_id' => rand(1000, 9999),
        'course_id' => $data['course_id'] ?? 0,
        'message' => 'Test paper saved (feature in development)'
    ], 'Test paper saved successfully');
}
// ============ GET TEST PAPER ============
else if ($action == 'get_test_paper') {
    $course_id = isset($_GET['course_id']) ? intval($_GET['course_id']) : 0;
    // For now, return empty (test papers need separate table implementation)
    sendSuccess([
        'test_paper_id' => 0,
        'course_id' => $course_id,
        'questions' => [],
        'message' => 'Test paper feature in development'
    ], 'No test paper available');
}
else {
    sendError('Invalid action: ' . $action, 400);
}
?>
