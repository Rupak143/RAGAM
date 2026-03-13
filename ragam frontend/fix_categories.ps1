# Fix Category Issues - Complete Solution
# Date: October 16, 2025

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Category Filtering Fix Script" -ForegroundColor Cyan
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""

$htdocsPath = "C:\xampp\htdocs\ragamfinal"

# Create SQL fix script
$sqlFix = @'
-- Fix Categories Table and Data
-- Run this in phpMyAdmin or MySQL command line

-- Step 1: Create categories table if it doesn't exist
CREATE TABLE IF NOT EXISTS `categories` (
  `category_id` int(11) NOT NULL AUTO_INCREMENT,
  `category_name` varchar(100) NOT NULL,
  `description` text,
  `is_active` tinyint(1) DEFAULT 1,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Step 2: Insert default categories (ignore if they exist)
INSERT IGNORE INTO `categories` (`category_id`, `category_name`, `description`, `is_active`) VALUES
(1, 'Vocal Training', 'Voice and singing lessons', 1),
(2, 'Instrumental Music', 'Learn to play musical instruments', 1),
(3, 'Bhakti/Devotional Music', 'Devotional and spiritual music training', 1),
(4, 'Kid''s Special Music Training', 'Music lessons specially designed for children', 1);

-- Step 3: Check if courses table has category_id column
ALTER TABLE `courses` 
ADD COLUMN IF NOT EXISTS `category_id` int(11) DEFAULT 1,
ADD INDEX IF NOT EXISTS `idx_category` (`category_id`);

-- Step 4: Update any courses with invalid category_id
UPDATE `courses` SET `category_id` = 1 WHERE `category_id` IS NULL OR `category_id` = 0;

-- Step 5: Add foreign key constraint (if not exists)
-- Note: This might fail if there's already a constraint or if there are orphaned records
-- ALTER TABLE `courses` 
-- ADD CONSTRAINT `fk_course_category` 
-- FOREIGN KEY (`category_id`) REFERENCES `categories`(`category_id`) 
-- ON DELETE SET DEFAULT ON UPDATE CASCADE;

-- Step 6: Verify the setup
SELECT 
    c.category_id,
    cat.category_name,
    COUNT(*) as total_courses,
    SUM(CASE WHEN c.is_published = 1 THEN 1 ELSE 0 END) as published_courses
FROM courses c
LEFT JOIN categories cat ON c.category_id = cat.category_id
GROUP BY c.category_id, cat.category_name
ORDER BY c.category_id;

-- If you see NULL in category_name, it means some courses have invalid category_id
'@

# Save SQL script
$sqlFilePath = "$htdocsPath\fix_categories.sql"
Set-Content -Path $sqlFilePath -Value $sqlFix -Encoding UTF8
Write-Host "SQL fix script created: $sqlFilePath" -ForegroundColor Green
Write-Host ""

# Create PHP fix script
$phpFix = @'
<?php
/**
 * Category Fix Script - Run this once to fix all category issues
 * Access via: http://localhost/ragamfinal/fix_categories_run.php
 */

require_once 'config.php';

header('Content-Type: text/html; charset=utf-8');
echo "<h2>Category Fix Script</h2><hr>";

$db = new DatabaseConfig();
$conn = $db->getConnection();

if (!$conn) {
    die("<p style='color:red;'>❌ Database connection failed!</p>");
}

echo "<p style='color:green;'>✅ Connected to database</p>";

// Step 1: Create categories table
echo "<h3>Step 1: Creating categories table...</h3>";
try {
    $createTable = "CREATE TABLE IF NOT EXISTS `categories` (
      `category_id` int(11) NOT NULL AUTO_INCREMENT,
      `category_name` varchar(100) NOT NULL,
      `description` text,
      `is_active` tinyint(1) DEFAULT 1,
      `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`category_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    
    $conn->exec($createTable);
    echo "<p style='color:green;'>✅ Categories table ready</p>";
} catch (PDOException $e) {
    echo "<p style='color:red;'>❌ Error: " . $e->getMessage() . "</p>";
}

// Step 2: Insert default categories
echo "<h3>Step 2: Adding default categories...</h3>";
try {
    $categories = [
        [1, 'Vocal Training', 'Voice and singing lessons'],
        [2, 'Instrumental Music', 'Learn to play musical instruments'],
        [3, 'Bhakti/Devotional Music', 'Devotional and spiritual music training'],
        [4, "Kid's Special Music Training", 'Music lessons specially designed for children']
    ];
    
    foreach ($categories as $cat) {
        $checkQuery = "SELECT COUNT(*) FROM categories WHERE category_id = ?";
        $stmt = $conn->prepare($checkQuery);
        $stmt->execute([$cat[0]]);
        
        if ($stmt->fetchColumn() == 0) {
            $insertQuery = "INSERT INTO categories (category_id, category_name, description, is_active) 
                           VALUES (?, ?, ?, 1)";
            $stmt = $conn->prepare($insertQuery);
            $stmt->execute([$cat[0], $cat[1], $cat[2]]);
            echo "<p>➕ Added: {$cat[1]}</p>";
        } else {
            echo "<p>✓ Exists: {$cat[1]}</p>";
        }
    }
    echo "<p style='color:green;'>✅ Categories setup complete</p>";
} catch (PDOException $e) {
    echo "<p style='color:red;'>❌ Error: " . $e->getMessage() . "</p>";
}

// Step 3: Check and fix courses table
echo "<h3>Step 3: Checking courses table...</h3>";
try {
    // Check if category_id column exists
    $stmt = $conn->query("SHOW COLUMNS FROM courses LIKE 'category_id'");
    if ($stmt->rowCount() == 0) {
        $conn->exec("ALTER TABLE courses ADD COLUMN category_id int(11) DEFAULT 1");
        echo "<p>➕ Added category_id column to courses table</p>";
    } else {
        echo "<p>✓ Courses table has category_id column</p>";
    }
    
    // Update null or invalid category_id values
    $updateQuery = "UPDATE courses SET category_id = 1 WHERE category_id IS NULL OR category_id = 0";
    $affected = $conn->exec($updateQuery);
    if ($affected > 0) {
        echo "<p>🔧 Fixed $affected course(s) with invalid category_id</p>";
    }
    
    echo "<p style='color:green;'>✅ Courses table ready</p>";
} catch (PDOException $e) {
    echo "<p style='color:red;'>❌ Error: " . $e->getMessage() . "</p>";
}

// Step 4: Verify setup
echo "<h3>Step 4: Verification</h3>";
try {
    $query = "SELECT 
                c.category_id,
                cat.category_name,
                COUNT(*) as total_courses,
                SUM(CASE WHEN c.is_published = 1 THEN 1 ELSE 0 END) as published_courses
              FROM courses c
              LEFT JOIN categories cat ON c.category_id = cat.category_id
              GROUP BY c.category_id, cat.category_name
              ORDER BY c.category_id";
    
    $stmt = $conn->query($query);
    $results = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo "<table border='1' cellpadding='10' style='border-collapse:collapse;'>";
    echo "<tr><th>Category ID</th><th>Category Name</th><th>Total Courses</th><th>Published</th></tr>";
    
    foreach ($results as $row) {
        $catName = $row['category_name'] ?? '<span style="color:red;">NULL - INVALID!</span>';
        echo "<tr>";
        echo "<td>{$row['category_id']}</td>";
        echo "<td>$catName</td>";
        echo "<td>{$row['total_courses']}</td>";
        echo "<td>{$row['published_courses']}</td>";
        echo "</tr>";
    }
    echo "</table>";
    
} catch (PDOException $e) {
    echo "<p style='color:red;'>❌ Error: " . $e->getMessage() . "</p>";
}

echo "<hr>";
echo "<h3>✅ Fix Complete!</h3>";
echo "<p>Now test the app by opening different categories.</p>";
echo "<p><a href='test_categories.php'>Run Category Tests</a></p>";

?>
'@

$phpFilePath = "$htdocsPath\fix_categories_run.php"
Set-Content -Path $phpFilePath -Value $phpFix -Encoding UTF8
Write-Host "PHP fix script created: $phpFilePath" -ForegroundColor Green
Write-Host ""

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "Fix Scripts Created!" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Open your browser and visit:" -ForegroundColor White
Write-Host "   http://localhost/ragamfinal/fix_categories_run.php" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. After running the fix, test categories at:" -ForegroundColor White
Write-Host "   http://localhost/ragamfinal/test_categories.php" -ForegroundColor Cyan
Write-Host ""
Write-Host "3. Then test in your Android app!" -ForegroundColor White
Write-Host ""
Write-Host "Files created:" -ForegroundColor Yellow
Write-Host "  - $sqlFilePath" -ForegroundColor White
Write-Host "  - $phpFilePath" -ForegroundColor White
Write-Host ""
