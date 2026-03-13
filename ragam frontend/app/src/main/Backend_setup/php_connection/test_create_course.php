<?php
/**
 * Test script for course creation functionality
 * Access this via: http://your-server/ragamfinal/test_create_course.php
 */

include_once 'config.php';getUserProfile

header('Content-Type: application/json');

echo "<h2>Testing Course Creation</h2>";

// Test 1: Database Connection
echo "<h3>1. Testing Database Connection</h3>";
$database = new DatabaseConfig();
$db = $database->getConnection();

if ($db === null) {
    echo "<p style='color: red;'>❌ Database connection failed!</p>";
    exit;
} else {
    echo "<p style='color: green;'>✅ Database connection successful!</p>";
}

// Test 2: Check if required tables exist
echo "<h3>2. Checking Database Tables</h3>";
$tables = ['users', 'courses', 'categories', 'course_lessons'];
foreach ($tables as $table) {
    try {
        $stmt = $db->query("SHOW TABLES LIKE '$table'");
        if ($stmt->rowCount() > 0) {
            echo "<p style='color: green;'>✅ Table '$table' exists</p>";
        } else {
            echo "<p style='color: red;'>❌ Table '$table' missing</p>";
        }
    } catch (Exception $e) {
        echo "<p style='color: red;'>❌ Error checking table '$table': " . $e->getMessage() . "</p>";
    }
}

// Test 3: Check if there are any users
echo "<h3>3. Checking Users Table</h3>";
try {
    $stmt = $db->query("SELECT COUNT(*) as count FROM users WHERE user_type = 'teacher'");
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    echo "<p>Found " . $result['count'] . " teacher(s) in database</p>";
    
    if ($result['count'] == 0) {
        echo "<p style='color: orange;'>⚠️ No teachers found. Creating a test teacher...</p>";
        $insertStmt = $db->prepare("INSERT INTO users (email, password, full_name, phone, user_type, bio, experience_years, specialization, is_verified) VALUES (?, ?, ?, ?, 'teacher', ?, ?, ?, 1)");
        $insertStmt->execute([
            'test.teacher@ragam.com',
            password_hash('testpass123', PASSWORD_DEFAULT),
            'Test Teacher',
            '+91-9999999999',
            'Experienced music teacher for testing',
            5,
            'Carnatic Music'
        ]);
        echo "<p style='color: green;'>✅ Test teacher created with ID: " . $db->lastInsertId() . "</p>";
    }
} catch (Exception $e) {
    echo "<p style='color: red;'>❌ Error checking users: " . $e->getMessage() . "</p>";
}

// Test 4: Check categories
echo "<h3>4. Checking Categories Table</h3>";
try {
    $stmt = $db->query("SELECT COUNT(*) as count FROM categories");
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    echo "<p>Found " . $result['count'] . " categories in database</p>";
    
    if ($result['count'] == 0) {
        echo "<p style='color: orange;'>⚠️ No categories found. This could cause course creation to fail.</p>";
    }
} catch (Exception $e) {
    echo "<p style='color: red;'>❌ Error checking categories: " . $e->getMessage() . "</p>";
}

// Test 5: Simulate course creation
echo "<h3>5. Testing Course Creation</h3>";
try {
    // Get a teacher ID
    $stmt = $db->query("SELECT user_id FROM users WHERE user_type = 'teacher' LIMIT 1");
    $teacher = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($teacher) {
        $teacherId = $teacher['user_id'];
        
        // Test course data
        $courseData = [
            'teacher_id' => $teacherId,
            'category_id' => 1,
            'course_title' => 'Test Course - ' . date('Y-m-d H:i:s'),
            'course_description' => 'This is a test course created by the diagnostic script',
            'course_price' => 0,
            'difficulty_level' => 'beginner',
            'video_path' => 'test_video_path.mp4'
        ];
        
        // Insert course
        $query = "INSERT INTO courses (teacher_id, category_id, course_title, course_description, 
                                     course_price, difficulty_level, is_published, is_free) 
                  VALUES (:teacher_id, :category_id, :course_title, :course_description, 
                         :course_price, :difficulty_level, 1, :is_free)";
        
        $stmt = $db->prepare($query);
        $stmt->bindParam(":teacher_id", $courseData['teacher_id']);
        $stmt->bindParam(":category_id", $courseData['category_id']);
        $stmt->bindParam(":course_title", $courseData['course_title']);
        $stmt->bindParam(":course_description", $courseData['course_description']);
        $stmt->bindParam(":course_price", $courseData['course_price']);
        $stmt->bindParam(":difficulty_level", $courseData['difficulty_level']);
        $is_free = ($courseData['course_price'] == 0) ? 1 : 0;
        $stmt->bindParam(":is_free", $is_free);
        
        if ($stmt->execute()) {
            $courseId = $db->lastInsertId();
            echo "<p style='color: green;'>✅ Test course created successfully with ID: " . $courseId . "</p>";
            
            // Test lesson creation
            $lessonQuery = "INSERT INTO course_lessons (course_id, lesson_title, lesson_description, 
                                                      video_url, lesson_order, is_free) 
                           VALUES (:course_id, :lesson_title, :lesson_description, 
                                  :video_url, 1, 1)";
            
            $lessonStmt = $db->prepare($lessonQuery);
            $lessonStmt->bindParam(":course_id", $courseId);
            $lessonTitle = $courseData['course_title'] . " - Introduction";
            $lessonStmt->bindParam(":lesson_title", $lessonTitle);
            $lessonStmt->bindParam(":lesson_description", $courseData['course_description']);
            $lessonStmt->bindParam(":video_url", $courseData['video_path']);
            
            if ($lessonStmt->execute()) {
                echo "<p style='color: green;'>✅ Test lesson created successfully with ID: " . $db->lastInsertId() . "</p>";
            } else {
                echo "<p style='color: orange;'>⚠️ Course created but lesson creation failed</p>";
            }
            
        } else {
            echo "<p style='color: red;'>❌ Test course creation failed</p>";
        }
        
    } else {
        echo "<p style='color: red;'>❌ No teacher found to test course creation</p>";
    }
    
} catch (Exception $e) {
    echo "<p style='color: red;'>❌ Error during course creation test: " . $e->getMessage() . "</p>";
}

echo "<h3>6. Summary</h3>";
echo "<p>If all tests passed with ✅, your course creation should work.</p>";
echo "<p>If you see any ❌ or ⚠️, please fix those issues first.</p>";
echo "<p><strong>Common fixes:</strong></p>";
echo "<ul>";
echo "<li>Make sure your database server (MySQL/XAMPP) is running</li>";
echo "<li>Check that the database 'ragamfinal' exists</li>";
echo "<li>Run the database_schema.sql script to create tables</li>";
echo "<li>Verify your database credentials in config.php</li>";
echo "</ul>";
?>
