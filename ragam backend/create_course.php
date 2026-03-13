<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    exit(0);
}

// Database connection
$host = '127.0.0.1';
$dbname = 'ragam';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;port=3305;dbname=$dbname;charset=utf8mb4", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch(PDOException $e) {
    echo json_encode(['success' => false, 'message' => 'Database connection failed: ' . $e->getMessage()]);
    exit;
}

$rawInput = file_get_contents("php://input");
$data = json_decode($rawInput);

// Debug logging
error_log("create_course.php - Raw input: " . $rawInput);
error_log("create_course.php - Parsed data: " . print_r($data, true));

if (!$data) {
    echo json_encode(['success' => false, 'message' => 'Invalid JSON', 'debug' => $rawInput]);
    exit;
}

if (!isset($data->instructor_id)) {
    echo json_encode(['success' => false, 'message' => 'Missing instructor_id']);
    exit;
}

if (!isset($data->title)) {
    echo json_encode(['success' => false, 'message' => 'Missing title']);
    exit;
}

try {
    $instructorId = intval($data->instructor_id);
    $title = trim($data->title);
    $description = isset($data->description) ? trim($data->description) : '';
    $category = isset($data->category) ? trim($data->category) : 'Vocal Training';
    $videoCount = isset($data->video_count) ? intval($data->video_count) : 0;

    // Get category ID from categories table
    $categoryStmt = $pdo->prepare("SELECT category_id FROM categories WHERE category_name = ? LIMIT 1");
    $categoryStmt->execute([$category]);
    $categoryRow = $categoryStmt->fetch(PDO::FETCH_ASSOC);
    
    if ($categoryRow) {
        $categoryId = $categoryRow['category_id'];
    } else {
        // Default to first category if not found
        $categoryId = 1;
        error_log("create_course.php - Category not found: $category, using default category_id=1");
    }

    // Insert course into database
    $stmt = $pdo->prepare("
        INSERT INTO courses (teacher_id, category_id, course_title, course_description, is_published, created_at)
        VALUES (?, ?, ?, ?, 1, NOW())
    ");
    
    $stmt->execute([$instructorId, $categoryId, $title, $description]);
    $courseId = $pdo->lastInsertId();

    // Build response
    $course = [
        'course_id' => intval($courseId),
        'instructor_id' => $instructorId,
        'title' => $title,
        'description' => $description,
        'category' => $category,
        'category_id' => $categoryId,
        'video_count' => $videoCount,
        'enrolled_count' => 0,
        'created_at' => date('Y-m-d H:i:s')
    ];

    error_log("create_course.php - Course created successfully with course_id: $courseId");
    
    echo json_encode([
        'success' => true, 
        'message' => 'Course created successfully', 
        'data' => $course
    ]);
    
} catch (PDOException $e) {
    error_log("create_course.php - Database error: " . $e->getMessage());
    echo json_encode(['success' => false, 'message' => 'Database error: ' . $e->getMessage()]);
}
?>

