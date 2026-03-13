<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type");
header("Content-Type: application/json");

// Database configuration
$host = 'localhost';
$dbname = 'ragam';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    echo "=== Fixing Content URI Entries ===\n";
    
    // Get all lessons with content:// URIs
    $stmt = $pdo->query("SELECT lesson_id, course_id, lesson_title, video_url FROM course_lessons WHERE video_url LIKE 'content://%'");
    $contentUriLessons = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo "Found " . count($contentUriLessons) . " lessons with content:// URIs\n\n";
    
    // Demo video URLs for different instruments
    $demoVideos = [
        'guitar' => 'http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4',
        'violin' => 'http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4',
        'piano' => 'http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
        'drums' => 'http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4',
        'flute' => 'http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4',
        'default' => 'http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'
    ];
    
    $updateCount = 0;
    
    foreach ($contentUriLessons as $lesson) {
        $lessonId = $lesson['lesson_id'];
        $lessonTitle = strtolower($lesson['lesson_title']);
        
        // Determine which demo video to use based on title
        $demoUrl = $demoVideos['default'];
        foreach ($demoVideos as $instrument => $url) {
            if (strpos($lessonTitle, $instrument) !== false) {
                $demoUrl = $url;
                break;
            }
        }
        
        // Update the lesson with demo video URL
        $updateStmt = $pdo->prepare("UPDATE course_lessons SET video_url = ? WHERE lesson_id = ?");
        $result = $updateStmt->execute([$demoUrl, $lessonId]);
        
        if ($result) {
            echo "✓ Updated lesson ID $lessonId: '$lessonTitle' -> $demoUrl\n";
            $updateCount++;
        } else {
            echo "✗ Failed to update lesson ID $lessonId\n";
        }
    }
    
    echo "\n=== Summary ===\n";
    echo "Successfully updated $updateCount lessons\n";
    
    // Verify the changes
    $stmt = $pdo->query("SELECT lesson_id, lesson_title, video_url FROM course_lessons ORDER BY lesson_id");
    $allLessons = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo "\n=== Current Video URLs ===\n";
    foreach ($allLessons as $lesson) {
        $status = (strpos($lesson['video_url'], 'content://') !== false) ? '❌' : '✅';
        echo "$status ID {$lesson['lesson_id']}: {$lesson['lesson_title']} -> {$lesson['video_url']}\n";
    }
    
    echo "\n=== Complete ===\n";
    
} catch (PDOException $e) {
    echo "Database Error: " . $e->getMessage() . "\n";
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>
