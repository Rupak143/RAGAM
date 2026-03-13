<?php
// Download sample videos for testing

$videosDir = 'C:\xampp\htdocs\ragamfinal\uploads\videos\\';

// Sample video URLs (smaller files for testing)
$sampleVideos = [
    'piano_intro.mp4' => 'https://sample-videos.com/zip/10/mp4/SampleVideo_360x240_1mb.mp4',
    'guitar_intro.mp4' => 'https://sample-videos.com/zip/10/mp4/SampleVideo_640x360_1mb.mp4', 
    'violin_intro.mp4' => 'https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4'
];

foreach ($sampleVideos as $filename => $url) {
    $destination = $videosDir . $filename;
    
    echo "Downloading $filename from $url...\n";
    
    try {
        $videoContent = file_get_contents($url);
        if ($videoContent !== false) {
            file_put_contents($destination, $videoContent);
            echo "✅ Downloaded $filename successfully\n";
        } else {
            echo "❌ Failed to download $filename\n";
        }
    } catch (Exception $e) {
        echo "❌ Error downloading $filename: " . $e->getMessage() . "\n";
    }
}

echo "\n📁 Video files location: $videosDir\n";
echo "🔗 Access videos via: http://10.206.163.64/ragamfinal/uploads/videos/[filename]\n";
?>
