<?php
// Simple test script
$pdo = new PDO('mysql:host=localhost;port=3305;dbname=ragam', 'root', '');

echo "Users:\n";
$users = $pdo->query('SELECT user_id, email, user_type FROM users')->fetchAll();
foreach($users as $u) {
    echo "{$u['user_id']} - {$u['email']} ({$u['user_type']})\n";
}

echo "\nCourses:\n";
$courses = $pdo->query('SELECT course_id, course_title, teacher_id FROM courses')->fetchAll();
foreach($courses as $c) {
    echo "{$c['course_id']} - {$c['course_title']} (teacher: {$c['teacher_id']})\n";
}

echo "\nCourse Lessons:\n";
$lessons = $pdo->query('SELECT lesson_id, course_id, lesson_title, video_url FROM course_lessons')->fetchAll();
echo "Total lessons: " . count($lessons) . "\n";
foreach($lessons as $l) {
    echo "Lesson {$l['lesson_id']} - Course {$l['course_id']}: {$l['lesson_title']} -> {$l['video_url']}\n";
}

echo "\nEnrollments:\n";
$enrollments = $pdo->query('SELECT enrollment_id, student_id, course_id FROM enrollments')->fetchAll();
foreach($enrollments as $e) {
    echo "Enrollment {$e['enrollment_id']}: Student {$e['student_id']} -> Course {$e['course_id']}\n";
}
?>
