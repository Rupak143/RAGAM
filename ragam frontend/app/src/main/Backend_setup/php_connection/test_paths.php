<?php
// Test script to verify upload path

// Check current working directory
echo "Current working directory: " . getcwd() . "\n";

// Check if upload directories exist
$paths = [
    '../uploads/videos/',
    '../../uploads/videos/',
    'uploads/videos/',
    'C:\\xampp\\htdocs\\ragamfinal\\uploads\\videos\\'
];

foreach ($paths as $path) {
    if (is_dir($path)) {
        echo "✅ Path exists: $path\n";
        echo "   Absolute path: " . realpath($path) . "\n";
    } else {
        echo "❌ Path does not exist: $path\n";
    }
}

// Show current script location
echo "\nCurrent script location: " . __FILE__ . "\n";
echo "Script directory: " . dirname(__FILE__) . "\n";

// Test creating a directory
$testDir = '../../uploads/videos/';
if (!is_dir($testDir)) {
    if (mkdir($testDir, 0755, true)) {
        echo "✅ Successfully created directory: $testDir\n";
    } else {
        echo "❌ Failed to create directory: $testDir\n";
    }
} else {
    echo "✅ Directory already exists: $testDir\n";
}
?>
