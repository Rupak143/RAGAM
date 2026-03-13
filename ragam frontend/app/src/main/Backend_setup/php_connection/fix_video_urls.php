<?php
// Manual script to update video URLs in database
// Run this once to fix existing content URI issues

require_once 'config.php';

try {
    // Update existing lessons with proper video URLs
    $updates = [
        [
            'lesson_id' => 5,
            'course_id' => 2,
            'video_url' => 'videos/guitar_intro_sample.mp4',
            'lesson_title' => 'Guitar - Introduction'
        ],
        [
            'lesson_id' => 6,
            'course_id' => 3,
            'video_url' => 'videos/violin_intro_sample.mp4',
            'lesson_title' => 'Violin - Introduction'
        ]
    ];

    foreach ($updates as $update) {
        $stmt = $pdo->prepare("UPDATE course_lessons SET video_url = ? WHERE lesson_id = ? AND course_id = ?");
        $result = $stmt->execute([$update['video_url'], $update['lesson_id'], $update['course_id']]);
        
        if ($result) {
            echo "Updated lesson {$update['lesson_title']} with video URL: {$update['video_url']}\n";
        } else {
            echo "Failed to update lesson {$update['lesson_title']}\n";
        }
    }

    echo "\nDatabase update completed!\n";
    echo "\nNOTE: You need to upload actual video files to:\n";
    echo "- C:\\xampp\\htdocs\\ragamfinal\\uploads\\videos\\guitar_intro_sample.mp4\n";
    echo "- C:\\xampp\\htdocs\\ragamfinal\\uploads\\videos\\violin_intro_sample.mp4\n";
    echo "\nOr use the sample URLs provided in the documentation.\n";

} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
?>
