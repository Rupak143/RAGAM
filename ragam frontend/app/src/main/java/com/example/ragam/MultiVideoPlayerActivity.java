package com.example.ragamfinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ragamfinal.utils.ApiHelper;
import org.json.JSONException;
import org.json.JSONObject;

public class MultiVideoPlayerActivity extends AppCompatActivity {
    
    private TextView courseTitle, instructorName, courseProgress;
    private LinearLayout videosContainer;
    private ApiHelper apiHelper;
    
    private String courseTitleStr, instructorNameStr;
    private int totalVideos, completedVideos;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_video_player);
        
        getCourseDataFromIntent();
        initViews();
        setupCourseInfo();
        loadVideoList();
    }
    
    private void getCourseDataFromIntent() {
        courseTitleStr = getIntent().getStringExtra("course_title");
        instructorNameStr = getIntent().getStringExtra("instructor_name");
        totalVideos = getIntent().getIntExtra("total_videos", 1);
        completedVideos = getIntent().getIntExtra("completed_videos", 0);
    }
    
    private void initViews() {
        courseTitle = findViewById(R.id.course_title);
        instructorName = findViewById(R.id.instructor_name);
        courseProgress = findViewById(R.id.course_progress);
        videosContainer = findViewById(R.id.videos_container);
        
        apiHelper = new ApiHelper(this);
    }
    
    private void setupCourseInfo() {
        courseTitle.setText(courseTitleStr);
        instructorName.setText("Instructor: " + instructorNameStr);
        
        int progressPercent = totalVideos > 0 ? (completedVideos * 100) / totalVideos : 0;
        courseProgress.setText("Progress: " + completedVideos + "/" + totalVideos + 
                              " videos (" + progressPercent + "% complete)");
    }
    
    private void loadVideoList() {
        videosContainer.removeAllViews();
        
        // Sample video data based on the course
        for (int i = 1; i <= totalVideos; i++) {
            boolean isCompleted = i <= completedVideos;
            addVideoItem(i, isCompleted);
        }
    }
    
    private void addVideoItem(int videoNumber, boolean isCompleted) {
        View videoItem = LayoutInflater.from(this).inflate(R.layout.item_video_player, 
                                                          videosContainer, false);
        
        TextView videoNumberText = videoItem.findViewById(R.id.video_number);
        TextView videoTitle = videoItem.findViewById(R.id.video_title);
        TextView videoDescription = videoItem.findViewById(R.id.video_description);
        Button playButton = videoItem.findViewById(R.id.btn_play_video);
        TextView completedStatus = videoItem.findViewById(R.id.completed_status);
        
        // Set video info based on course and video number
        videoNumberText.setText(String.valueOf(videoNumber));
        
        String title = getVideoTitle(videoNumber);
        String description = getVideoDescription(videoNumber);
        
        videoTitle.setText(title);
        videoDescription.setText(description);
        
        if (isCompleted) {
            completedStatus.setText("✓ Completed");
            completedStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            playButton.setText("Replay");
        } else {
            completedStatus.setText("Not completed");
            completedStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
            playButton.setText("Play");
        }
        
        playButton.setOnClickListener(v -> playVideo(videoNumber, title, isCompleted));
        
        videosContainer.addView(videoItem);
    }
    
    private String getVideoTitle(int videoNumber) {
        if (courseTitleStr.contains("Vocal")) {
            switch (videoNumber) {
                case 1: return "Introduction to Vocal Techniques";
                case 2: return "Breathing Exercises";
                case 3: return "Advanced Vocal Control";
                default: return "Vocal Training Video " + videoNumber;
            }
        } else if (courseTitleStr.contains("Guitar")) {
            switch (videoNumber) {
                case 1: return "Guitar Basics & Posture";
                case 2: return "Basic Chords";
                case 3: return "Strumming Patterns";
                case 4: return "Fingerpicking Techniques";
                case 5: return "Playing Your First Song";
                default: return "Guitar Lesson " + videoNumber;
            }
        } else if (courseTitleStr.contains("Theory")) {
            switch (videoNumber) {
                case 1: return "Notes and Scales";
                case 2: return "Intervals and Chords";
                case 3: return "Rhythm and Time Signatures";
                case 4: return "Key Signatures";
                default: return "Music Theory Lesson " + videoNumber;
            }
        }
        return "Video " + videoNumber + ": " + courseTitleStr;
    }
    
    private String getVideoDescription(int videoNumber) {
        if (courseTitleStr.contains("Vocal")) {
            switch (videoNumber) {
                case 1: return "Learn the fundamentals of proper vocal technique and warm-up exercises.";
                case 2: return "Master breathing techniques essential for powerful vocal performance.";
                case 3: return "Develop advanced control over pitch, tone, and vocal dynamics.";
                default: return "Essential vocal training techniques for singers.";
            }
        } else if (courseTitleStr.contains("Guitar")) {
            switch (videoNumber) {
                case 1: return "Learn proper guitar holding position and basic setup.";
                case 2: return "Master essential chords: C, G, D, Em, Am.";
                case 3: return "Learn different strumming patterns and rhythms.";
                case 4: return "Introduction to fingerpicking and fingerstyle techniques.";
                case 5: return "Put it all together and play your first complete song.";
                default: return "Essential guitar playing techniques for beginners.";
            }
        } else if (courseTitleStr.contains("Theory")) {
            switch (videoNumber) {
                case 1: return "Understanding musical notes, scales, and their relationships.";
                case 2: return "Learn about intervals and how chords are constructed.";
                case 3: return "Master rhythm notation and different time signatures.";
                case 4: return "Understanding key signatures and their practical applications.";
                default: return "Essential music theory concepts for musicians.";
            }
        }
        return "Learn important concepts in this " + courseTitleStr + " lesson.";
    }
    
    private void playVideo(int videoNumber, String title, boolean wasCompleted) {
        // Launch video player
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("video_title", title);
        intent.putExtra("video_number", videoNumber);
        intent.putExtra("course_title", courseTitleStr);
        startActivity(intent);
        
        // Mark as completed if it wasn't already
        if (!wasCompleted) {
            markVideoAsCompleted(videoNumber);
        }
        
        Toast.makeText(this, "Playing: " + title, Toast.LENGTH_SHORT).show();
    }
    
    private void markVideoAsCompleted(int videoNumber) {
        // Update completion status
        if (completedVideos < videoNumber) {
            completedVideos = videoNumber;
            setupCourseInfo(); // Refresh progress display
            
            // Update the specific video item to show as completed
            refreshVideoList();
        }
        
        // Call API to mark video as completed
        try {
            JSONObject currentUser = apiHelper.getUserSession();
            if (currentUser != null) {
                int userId = Integer.parseInt(currentUser.getString("id"));
                
                apiHelper.markVideoAsCompleted(userId, videoNumber, new ApiHelper.ApiCallback() {
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            Toast.makeText(MultiVideoPlayerActivity.this, 
                                "Video " + videoNumber + " marked as completed!", 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        // Silent fail for demo
                    }
                });
            }
        } catch (Exception e) {
            // Silent fail for demo
        }
    }
    
    private void refreshVideoList() {
        loadVideoList(); // Reload the video list to update completion status
    }
}
