<?php
// Database update script to fix video URLs

// Direct database connection
$host = 'localhost';
$dbname = 'ragam';
$username = 'root';
$password = '';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    echo "Starting database update...\n";
    
    // Update Guitar lesson with working video URL
    $stmt = $pdo->prepare("UPDATE course_lessons SET video_url = ? WHERE lesson_id = 5 AND course_id = 2");
    $result1 = $stmt->execute(['https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4']);
    
    if ($result1) {
        echo "✅ Updated Guitar lesson with sample video URL\n";
    } else {
        echo "❌ Failed to update Guitar lesson\n";
    }
    
    // Update Violin lesson with working video URL
    $stmt = $pdo->prepare("UPDATE course_lessons SET video_url = ? WHERE lesson_id = 6 AND course_id = 3");
    $result2 = $stmt->execute(['https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_2mb.mp4']);
    
    if ($result2) {
        echo "✅ Updated Violin lesson with sample video URL\n";
    } else {
        echo "❌ Failed to update Violin lesson\n";
    }
    
    // Verify the updates
    echo "\nVerifying updates:\n";
    $stmt = $pdo->query("SELECT lesson_id, course_id, lesson_title, video_url FROM course_lessons WHERE lesson_id IN (5, 6)");
    $lessons = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    foreach ($lessons as $lesson) {
        echo "Lesson ID: {$lesson['lesson_id']}, Course ID: {$lesson['course_id']}, Title: {$lesson['lesson_title']}, Video URL: {$lesson['video_url']}\n";
    }
    
    echo "\n✅ Database update completed successfully!\n";
    
} catch (Exception $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
}