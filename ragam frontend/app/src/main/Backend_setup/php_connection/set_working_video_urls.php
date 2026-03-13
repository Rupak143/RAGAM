<?php
// Use direct web URLs for videos since downloads failed

// Direct database connection
$host = 'localhost';
$dbname = 'ragam';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    echo "Updating video URLs to use working web links...\n";
    
    // Update with working video URLs
    $updates = [
        // Piano lesson
        [9, 1, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4'],
        // Guitar lesson  
        [5, 2, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4'],
        // Violin lesson
        [6, 3, 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4']
    ];
    
    $stmt = $pdo->prepare("UPDATE course_lessons SET video_url = ? WHERE lesson_id = ? AND course_id = ?");
    
    foreach ($updates as $update) {
        $result = $stmt->execute([$update[2], $update[0], $update[1]]);
        if ($result) {
            echo "✅ Updated lesson {$update[0]} with working video URL\n";
        } else {
            echo "❌ Failed to update lesson {$update[0]}\n";
        }
    }
    
    // Clean up invalid content:// URLs
    $stmt2 = $pdo->prepare("UPDATE course_lessons SET video_url = ? WHERE video_url LIKE 'content://%'");
    $result2 = $stmt2->execute(['https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4']);
    if ($result2) {
        echo "✅ Cleaned up invalid content:// URLs\n";
    }
    
    // Verify updates
    echo "\nFinal video URLs:\n";
    $stmt = $pdo->query("SELECT lesson_id, course_id, lesson_title, video_url FROM course_lessons WHERE video_url IS NOT NULL AND video_url != '' ORDER BY course_id");
    $lessons = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    foreach ($lessons as $lesson) {
        echo "Course {$lesson['course_id']} - Lesson {$lesson['lesson_id']}: {$lesson['lesson_title']}\n";
        echo "   Video: {$lesson['video_url']}\n\n";
    }
    
    echo "✅ All videos now use working web URLs!\n";
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>
