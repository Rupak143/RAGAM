<?php
// Fix video paths in database - remove content URIs and add proper server URLs

// Direct database connection
$host = 'localhost';
$dbname = 'ragam';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    echo "Starting video path fix...\n";
    
    // First, check current video URLs
    echo "\nCurrent video URLs:\n";
    $stmt = $pdo->query("SELECT lesson_id, course_id, lesson_title, video_url FROM course_lessons WHERE video_url IS NOT NULL AND video_url != ''");
    $lessons = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    foreach ($lessons as $lesson) {
        echo "Lesson ID: {$lesson['lesson_id']}, Title: {$lesson['lesson_title']}, Current URL: {$lesson['video_url']}\n";
    }
    
    // Update any content:// URLs to proper web URLs
    $stmt = $pdo->prepare("UPDATE course_lessons SET video_url = ? WHERE lesson_id = ? AND course_id = ?");
    
    // Guitar course - add working video
    $result1 = $stmt->execute(['videos/guitar_intro.mp4', 5, 2]);
    if ($result1) {
        echo "✅ Updated Guitar lesson video path\n";
    }
    
    // Violin course - add working video  
    $result2 = $stmt->execute(['videos/violin_intro.mp4', 6, 3]);
    if ($result2) {
        echo "✅ Updated Violin lesson video path\n";
    }
    
    // Piano course - add working video
    $stmt2 = $pdo->prepare("INSERT INTO course_lessons (course_id, lesson_title, lesson_description, video_url, lesson_order) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE video_url = VALUES(video_url)");
    $result3 = $stmt2->execute([1, 'Piano Basics', 'Learn the fundamentals of piano playing', 'videos/piano_intro.mp4', 1]);
    if ($result3) {
        echo "✅ Added/Updated Piano lesson\n";
    }
    
    // Verify the updates
    echo "\nUpdated video URLs:\n";
    $stmt = $pdo->query("SELECT lesson_id, course_id, lesson_title, video_url FROM course_lessons WHERE video_url IS NOT NULL AND video_url != ''");
    $lessons = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    foreach ($lessons as $lesson) {
        echo "Lesson ID: {$lesson['lesson_id']}, Course ID: {$lesson['course_id']}, Title: {$lesson['lesson_title']}, Video URL: {$lesson['video_url']}\n";
    }
    
    echo "\n✅ Video paths fixed successfully!\n";
    echo "Note: Make sure to place actual video files in C:\\xampp\\htdocs\\ragamfinal\\uploads\\videos\\ folder\n";
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}
?>
