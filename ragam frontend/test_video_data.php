<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "=== Testing Video Data for Mohan ===\n\n";

try {
    $pdo = new PDO('mysql:host=localhost;port=3305;dbname=ragam', 'root', '');
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    echo "✓ Connected to database\n\n";
    
    // Get mohan's user ID
    $stmt = $pdo->prepare('SELECT user_id, full_name, email FROM users WHERE email = ?');
    $stmt->execute(['mohan@gmail.com']);
    $mohan = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$mohan) {
        echo "✗ Mohan user not found!\n";
        exit;
    }
    
    echo "Student: {$mohan['full_name']} (ID: {$mohan['user_id']}, Email: {$mohan['email']})\n\n";
    
    // Get all teachers
    $stmt = $pdo->prepare('SELECT user_id, full_name, email FROM users WHERE user_type = ?');
    $stmt->execute(['teacher']);
    $teachers = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo "=== Teachers in Database ===\n";
    foreach ($teachers as $teacher) {
        echo "  - {$teacher['full_name']} ({$teacher['email']}) - ID: {$teacher['user_id']}\n";
    }
    echo "\n";
    
    // Get all courses
    echo "=== All Courses ===\n";
    $stmt = $pdo->query('SELECT course_id, course_title, teacher_id FROM courses');
    $allCourses = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    foreach ($allCourses as $course) {
        echo "\nCourse ID: {$course['course_id']}\n";
        echo "  Title: {$course['course_title']}\n";
        echo "  Teacher ID: {$course['teacher_id']}\n";
        
        // Check if mohan is enrolled
        $stmt2 = $pdo->prepare('SELECT enrollment_id FROM enrollments WHERE course_id = ? AND student_id = ?');
        $stmt2->execute([$course['course_id'], $mohan['user_id']]);
        $enrollment = $stmt2->fetch(PDO::FETCH_ASSOC);
        
        if ($enrollment) {
            echo "  ✓ Mohan IS enrolled (enrollment_id: {$enrollment['enrollment_id']})\n";
            
            // Check videos for this course
            $stmt3 = $pdo->prepare('SELECT lesson_id, lesson_title, lesson_description, video_url, lesson_order FROM course_lessons WHERE course_id = ? ORDER BY lesson_order');
            $stmt3->execute([$course['course_id']]);
            $videos = $stmt3->fetchAll(PDO::FETCH_ASSOC);
            
            echo "  Videos in course: " . count($videos) . "\n";
            
            if (count($videos) > 0) {
                foreach ($videos as $idx => $video) {
                    echo "    Video " . ($idx + 1) . ":\n";
                    echo "      - ID: {$video['lesson_id']}\n";
                    echo "      - Title: {$video['lesson_title']}\n";
                    echo "      - Description: {$video['lesson_description']}\n";
                    echo "      - URL: {$video['video_url']}\n";
                    echo "      - Order: {$video['lesson_order']}\n";
                }
            } else {
                echo "    ⚠ NO VIDEOS FOUND - This is the problem!\n";
                echo "\n    Let's add a test video...\n";
                
                // Insert a test video
                $insertStmt = $pdo->prepare('INSERT INTO course_lessons (course_id, lesson_title, lesson_description, video_url, lesson_order, duration_minutes, is_free) VALUES (?, ?, ?, ?, ?, ?, ?)');
                $testVideoUrl = 'uploads/videos/test_video_' . time() . '.mp4';
                $insertStmt->execute([
                    $course['course_id'],
                    'Introduction to the Course',
                    'This is a test video for demonstration purposes',
                    $testVideoUrl,
                    1,
                    10,
                    1
                ]);
                
                $newVideoId = $pdo->lastInsertId();
                echo "    ✓ Test video created with ID: $newVideoId\n";
                echo "    URL: $testVideoUrl\n";
            }
        } else {
            echo "  ✗ Mohan is NOT enrolled\n";
        }
    }
    
    echo "\n=== Test API Call ===\n";
    // Test the API endpoint
    foreach ($allCourses as $course) {
        $stmt2 = $pdo->prepare('SELECT enrollment_id FROM enrollments WHERE course_id = ? AND student_id = ?');
        $stmt2->execute([$course['course_id'], $mohan['user_id']]);
        if ($stmt2->fetch(PDO::FETCH_ASSOC)) {
            $testUrl = "http://192.168.1.7/ragamfinal/courses.php?action=get_course_videos&course_id={$course['course_id']}";
            echo "Test URL: $testUrl\n";
            break;
        }
    }
    
} catch (PDOException $e) {
    echo "✗ Database error: " . $e->getMessage() . "\n";
}
?>
