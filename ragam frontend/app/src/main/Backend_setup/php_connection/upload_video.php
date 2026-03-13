<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit;
}

try {
    // Check if file was uploaded
    if (!isset($_FILES['video']) || $_FILES['video']['error'] !== UPLOAD_ERR_OK) {
        throw new Exception('No video file uploaded or upload error occurred');
    }

    $uploadedFile = $_FILES['video'];
    $courseId = $_POST['course_id'] ?? 'temp';
    $lessonTitle = $_POST['lesson_title'] ?? 'Introduction Video';
    
    // Validate file type
    $allowedTypes = ['video/mp4', 'video/avi', 'video/mov', 'video/wmv'];
    $fileType = $uploadedFile['type'];
    
    if (!in_array($fileType, $allowedTypes)) {
        throw new Exception('Invalid file type. Only MP4, AVI, MOV, and WMV files are allowed');
    }

    // Validate file size (max 100MB)
    $maxSize = 100 * 1024 * 1024; // 100MB in bytes
    if ($uploadedFile['size'] > $maxSize) {
        throw new Exception('File size too large. Maximum size is 100MB');
    }

    // CORRECTED: Create uploads directory in htdocs/ragamfinal/uploads/videos/
    $uploadDir = 'uploads/videos/';
    if (!is_dir($uploadDir)) {
        mkdir($uploadDir, 0755, true);
    }

    // Generate unique filename
    $fileExtension = pathinfo($uploadedFile['name'], PATHINFO_EXTENSION);
    $sanitizedLessonTitle = preg_replace('/[^a-zA-Z0-9_-]/', '_', $lessonTitle);
    $filename = 'course_' . $courseId . '_' . $sanitizedLessonTitle . '_' . time() . '.' . $fileExtension;
    $uploadPath = $uploadDir . $filename;

    // Move uploaded file
    if (!move_uploaded_file($uploadedFile['tmp_name'], $uploadPath)) {
        throw new Exception('Failed to move uploaded file to: ' . $uploadPath);
    }

    // CORRECTED: Return the correct relative path for video playback
    $relativePath = 'videos/' . $filename;

    // Log for debugging
    error_log("Video uploaded successfully:");
    error_log("Upload path: " . $uploadPath);
    error_log("Relative path: " . $relativePath);
    error_log("File size: " . $uploadedFile['size']);

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
    error_log("Video upload error: " . $e->getMessage());
    echo json_encode([
        'success' => false,
        'message' => $e->getMessage()
    ]);
}
?>