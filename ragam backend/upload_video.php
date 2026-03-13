<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
// Log all incoming requests
error_log("=== Video Upload Request ===");
error_log("Method: " . $_SERVER['REQUEST_METHOD']);
error_log("Content-Type: " . ($_SERVER['CONTENT_TYPE'] ?? 'not set'));
error_log("Content-Length: " . ($_SERVER['CONTENT_LENGTH'] ?? 'not set'));
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit(0);
}
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed. Use POST.']);
    exit;
}
try {
    // Log request details
    error_log("POST data: " . print_r($_POST, true));
    error_log("FILES data: " . print_r($_FILES, true));
    // Check if file was uploaded
    if (!isset($_FILES['video'])) {
        throw new Exception('No video file found in request. Check multipart form-data encoding.');
    }
    $uploadedFile = $_FILES['video'];
    // Check for upload errors
    if ($uploadedFile['error'] !== UPLOAD_ERR_OK) {
        $errorMessages = [
            UPLOAD_ERR_INI_SIZE => 'File exceeds upload_max_filesize in php.ini (' . ini_get('upload_max_filesize') . ')',
            UPLOAD_ERR_FORM_SIZE => 'File exceeds MAX_FILE_SIZE in HTML form',
            UPLOAD_ERR_PARTIAL => 'File was only partially uploaded',
            UPLOAD_ERR_NO_FILE => 'No file was uploaded',
            UPLOAD_ERR_NO_TMP_DIR => 'Missing temporary folder',
            UPLOAD_ERR_CANT_WRITE => 'Failed to write file to disk',
            UPLOAD_ERR_EXTENSION => 'PHP extension stopped the file upload'
        ];
        $errorMsg = $errorMessages[$uploadedFile['error']] ?? 'Unknown upload error: ' . $uploadedFile['error'];
        throw new Exception($errorMsg);
    }
    $courseId = $_POST['course_id'] ?? 'temp';
    $lessonTitle = $_POST['lesson_title'] ?? 'Introduction Video';
    // Validate file type
    $allowedTypes = ['video/mp4', 'video/avi', 'video/mov', 'video/wmv', 'video/quicktime', 'video/x-msvideo', 'application/octet-stream'];
    $fileType = $uploadedFile['type'];
    error_log("File type: " . $fileType);
    error_log("File size: " . $uploadedFile['size'] . " bytes");
    if (!in_array($fileType, $allowedTypes)) {
        // Also check file extension as fallback
        $ext = strtolower(pathinfo($uploadedFile['name'], PATHINFO_EXTENSION));
        if (!in_array($ext, ['mp4', 'avi', 'mov', 'wmv'])) {
            throw new Exception('Invalid file type: ' . $fileType . '. Only MP4, AVI, MOV, and WMV files are allowed');
        }
    }
    // Validate file size (max 100MB)
    $maxSize = 100 * 1024 * 1024; // 100MB
    if ($uploadedFile['size'] > $maxSize) {
        throw new Exception('File size too large: ' . round($uploadedFile['size'] / 1024 / 1024, 2) . 'MB. Maximum size is 100MB');
    }
    if ($uploadedFile['size'] == 0) {
        throw new Exception('File is empty (0 bytes)');
    }
    // Create upload directory
    $uploadDir = 'uploads/videos/';
    if (!is_dir($uploadDir)) {
        if (!mkdir($uploadDir, 0755, true)) {
            throw new Exception('Failed to create upload directory: ' . $uploadDir);
        }
    }
    // Generate unique filename
    $fileExtension = pathinfo($uploadedFile['name'], PATHINFO_EXTENSION);
    $sanitizedLessonTitle = preg_replace('/[^a-zA-Z0-9_-]/', '_', $lessonTitle);
    $filename = 'course_' . $courseId . '_' . $sanitizedLessonTitle . '_' . time() . '.' . $fileExtension;
    $uploadPath = $uploadDir . $filename;
    error_log("Attempting to move file to: " . $uploadPath);
    // Move uploaded file
    if (!move_uploaded_file($uploadedFile['tmp_name'], $uploadPath)) {
        throw new Exception('Failed to move uploaded file. Check directory permissions.');
    }
    // RETURN RELATIVE PATH ONLY - NO IP ADDRESS
    $relativePath = 'uploads/videos/' . $filename;
    // Success log
    error_log("✓ Video uploaded successfully:");
    error_log("  Upload path: " . $uploadPath);
    error_log("  Relative path: " . $relativePath);
    error_log("  File size: " . $uploadedFile['size'] . " bytes");
    echo json_encode([
        'success' => true,
        'message' => 'Video uploaded successfully',
        'data' => [
            'video_url' => $relativePath,
            'filename' => $filename,
            'size' => $uploadedFile['size'],
            'full_path' => $uploadPath
        ]
    ]);
} catch (Exception $e) {
    http_response_code(400);
    $errorMsg = $e->getMessage();
    error_log("✗ Video upload error: " . $errorMsg);
    echo json_encode([
        'success' => false,
        'message' => $errorMsg,
        'debug' => [
            'max_upload' => ini_get('upload_max_filesize'),
            'max_post' => ini_get('post_max_size'),
            'files_received' => isset($_FILES['video']) ? 'yes' : 'no'
        ]
    ]);
}
?>
