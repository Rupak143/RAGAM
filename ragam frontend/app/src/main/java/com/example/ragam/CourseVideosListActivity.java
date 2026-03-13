package com.example.ragamfinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.example.ragamfinal.config.AppConfig;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

public class CourseVideosListActivity extends AppCompatActivity {
    
    private TextView courseTitleText, courseDescriptionText;
    private LinearLayout videosContainer;
    private ImageView backButton;
    private Button takeQuizButton;
    private int courseId;
    private String courseTitle, courseDescription;
    private JSONArray videosArray;
    private int currentUnlockedVideo = 0; // Track which video is unlocked
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_videos_list);
        
        initViews();
        loadIntentData();
        loadCourseVideos();
        loadSavedProgress(); // Load progress from backend
    }
    
    private void initViews() {
        courseTitleText = findViewById(R.id.course_title);
        courseDescriptionText = findViewById(R.id.course_description);
        videosContainer = findViewById(R.id.videos_container);
        backButton = findViewById(R.id.back_button);
        takeQuizButton = findViewById(R.id.btn_take_quiz);
        
        backButton.setOnClickListener(v -> finish());
        
        // Quiz button initially hidden
        takeQuizButton.setVisibility(View.GONE);
        takeQuizButton.setOnClickListener(v -> openQuiz());
    }
    
    private void loadIntentData() {
        Intent intent = getIntent();
        courseId = intent.getIntExtra("course_id", 0);
        courseTitle = intent.getStringExtra("course_title");
        courseDescription = intent.getStringExtra("course_description");
        
        if (courseTitle != null) {
            courseTitleText.setText(courseTitle);
        }
        if (courseDescription != null) {
            courseDescriptionText.setText(courseDescription);
        }
    }
    
    private void loadCourseVideos() {
        String url = AppConfig.getBaseUrl(this) + "courses.php?action=get_course_videos&course_id=" + courseId;
        
        Log.d("CourseVideosList", "Loading videos from URL: " + url);
        Log.d("CourseVideosList", "Course ID: " + courseId + ", Title: " + courseTitle);
        
        StringRequest request = new StringRequest(Request.Method.GET, url,
            response -> {
                try {
                    Log.d("CourseVideosList", "Videos response: " + response);
                    JSONObject jsonResponse = new JSONObject(response);
                    
                    // Check for either "success" boolean or "status" string field
                    boolean isSuccess = jsonResponse.optBoolean("success", false) || 
                                       "success".equals(jsonResponse.optString("status", ""));
                    
                    if (isSuccess) {
                        videosArray = jsonResponse.getJSONArray("data");
                        Log.d("CourseVideosList", "Found " + videosArray.length() + " videos");
                        
                        if (videosArray.length() == 0) {
                            Toast.makeText(this, "No videos uploaded yet for this course", Toast.LENGTH_LONG).show();
                            Log.w("CourseVideosList", "Empty videos array returned from server");
                        } else {
                            displayVideos();
                        }
                    } else {
                        String message = jsonResponse.optString("message", "No videos found");
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        Log.w("CourseVideosList", "Server returned success=false: " + message);
                    }
                } catch (Exception e) {
                    Log.e("CourseVideosList", "Error parsing videos", e);
                    Toast.makeText(this, "Error loading videos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            },
            error -> {
                Log.e("CourseVideosList", "Error: " + error.toString());
                if (error.networkResponse != null) {
                    String errorBody = new String(error.networkResponse.data);
                    Log.e("CourseVideosList", "Error response: " + errorBody);
                }
                Toast.makeText(this, "Network error loading videos", Toast.LENGTH_SHORT).show();
            }
        );
        
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
    
    private void displayVideos() {
        videosContainer.removeAllViews();
        
        try {
            for (int i = 0; i < videosArray.length(); i++) {
                JSONObject video = videosArray.getJSONObject(i);
                
                View videoItem = LayoutInflater.from(this).inflate(R.layout.item_video_card, videosContainer, false);
                
                TextView videoNumber = videoItem.findViewById(R.id.video_number);
                TextView videoTitle = videoItem.findViewById(R.id.video_title);
                TextView videoDescription = videoItem.findViewById(R.id.video_description);
                ImageView playIcon = videoItem.findViewById(R.id.play_icon);
                ImageView lockIcon = videoItem.findViewById(R.id.lock_icon);
                
                videoNumber.setText(String.valueOf(i + 1));
                videoTitle.setText(video.optString("title", "Video " + (i + 1)));
                videoDescription.setText(video.optString("description", ""));
                
                // Only first video is unlocked initially
                if (i <= currentUnlockedVideo) {
                    // Unlocked - show play icon
                    playIcon.setVisibility(View.VISIBLE);
                    lockIcon.setVisibility(View.GONE);
                    
                    final int videoIndex = i;
                    videoItem.setOnClickListener(v -> {
                        // Open video player
                        Intent intent = new Intent(CourseVideosListActivity.this, SingleVideoPlayerActivity.class);
                        intent.putExtra("course_id", courseId);
                        intent.putExtra("video_index", videoIndex);
                        intent.putExtra("videos_array", videosArray.toString());
                        startActivityForResult(intent, 100);
                    });
                } else {
                    // Locked - show lock icon
                    playIcon.setVisibility(View.GONE);
                    lockIcon.setVisibility(View.VISIBLE);
                    
                    videoItem.setOnClickListener(v -> {
                        Toast.makeText(this, "Complete previous video to unlock", Toast.LENGTH_SHORT).show();
                    });
                }
                
                videosContainer.addView(videoItem);
            }
        } catch (Exception e) {
            Log.e("CourseVideosList", "Error displaying videos", e);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Mark the specific video as completed in backend
            markVideoCompleted(currentUnlockedVideo);
            
            // Video was completed, unlock next video
            currentUnlockedVideo++;
            displayVideos(); // Refresh the list
            
            // Check if all videos are completed
            if (currentUnlockedVideo >= videosArray.length()) {
                Toast.makeText(this, "🎉 All videos completed! You can now take the quiz.", Toast.LENGTH_LONG).show();
                // Show "Take Quiz" button
                takeQuizButton.setVisibility(View.VISIBLE);
            }
        }
    }
    
    // REMOVED: updateCourseProgress() method
    // The mark_video_completed endpoint already returns progress, so we don't need a separate update call
    
    private void loadSavedProgress() {
        try {
            com.example.ragamfinal.utils.ApiHelper apiHelper = new com.example.ragamfinal.utils.ApiHelper(this);
            JSONObject currentUser = apiHelper.getUserSession();
            if (currentUser == null) return;
            
            int studentId = currentUser.getInt("user_id");
            
            String url = AppConfig.getBaseUrl(this) + "courses.php?action=get_video_progress" +
                        "&student_id=" + studentId + "&course_id=" + courseId;
            
            RequestQueue queue = Volley.newRequestQueue(this);
            StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        Log.d("CourseVideosList", "Progress response: " + response);
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.optBoolean("success", false)) {
                            // Get data object first
                            JSONObject data = jsonResponse.getJSONObject("data");
                            
                            // Get completed_lessons array (not completed_videos!)
                            org.json.JSONArray completed = data.getJSONArray("completed_lessons");
                            
                            if (completed.length() > 0) {
                                // Find video with highest video_number
                                int maxCompleted = -1;
                                for (int i = 0; i < videosArray.length(); i++) {
                                    JSONObject video = videosArray.getJSONObject(i);
                                    int videoId = video.getInt("video_id");
                                    
                                    // Check if this video is in completed list
                                    for (int j = 0; j < completed.length(); j++) {
                                        if (completed.getInt(j) == videoId) {
                                            if (i > maxCompleted) {
                                                maxCompleted = i;
                                            }
                                            break;
                                        }
                                    }
                                }
                                
                                // Unlock up to next video after highest completed
                                currentUnlockedVideo = maxCompleted + 1;
                                Log.d("CourseVideosList", "Loaded progress: " + completed.length() + " videos completed, unlocked up to video " + currentUnlockedVideo);
                            }
                            displayVideos(); // Refresh UI with loaded progress
                            
                            // Check if all videos completed
                            if (videosArray != null && currentUnlockedVideo >= videosArray.length()) {
                                takeQuizButton.setVisibility(View.VISIBLE);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("CourseVideosList", "Error parsing progress", e);
                    }
                },
                error -> Log.e("CourseVideosList", "Error loading progress: " + error.toString())
            );
            queue.add(request);
            
        } catch (Exception e) {
            Log.e("CourseVideosList", "Error loading saved progress", e);
        }
    }
    
    private void markVideoCompleted(int videoIndex) {
        try {
            com.example.ragamfinal.utils.ApiHelper apiHelper = new com.example.ragamfinal.utils.ApiHelper(this);
            JSONObject currentUser = apiHelper.getUserSession();
            if (currentUser == null) return;
            
            int studentId = currentUser.getInt("user_id");
            
            // Get the actual video_id from the videos array
            int videoId = videosArray.getJSONObject(videoIndex).getInt("video_id");
            
            String url = AppConfig.getBaseUrl(this) + "courses.php?action=mark_video_completed";
            JSONObject requestBody = new JSONObject();
            requestBody.put("student_id", studentId);
            requestBody.put("course_id", courseId);
            requestBody.put("video_id", videoId);  // Use video_id, not video_index
            
            Log.d("CourseVideosList", "Marking video " + videoId + " as completed for student " + studentId);
            
            RequestQueue queue = Volley.newRequestQueue(this);
            com.android.volley.toolbox.JsonObjectRequest jsonRequest = 
                new com.android.volley.toolbox.JsonObjectRequest(
                    Request.Method.POST, url, requestBody,
                    response -> {
                        Log.d("CourseVideosList", "Video marked complete: " + response.toString());
                        Toast.makeText(this, "Video " + (videoIndex + 1) + " progress saved!", Toast.LENGTH_SHORT).show();
                    },
                    error -> Log.e("CourseVideosList", "Error marking video: " + error.toString())
                );
            queue.add(jsonRequest);
            
        } catch (Exception e) {
            Log.e("CourseVideosList", "Error marking video completed", e);
        }
    }
    
    private void openQuiz() {
        try {
            com.example.ragamfinal.utils.ApiHelper apiHelper = new com.example.ragamfinal.utils.ApiHelper(this);
            JSONObject currentUser = apiHelper.getUserSession();
            if (currentUser == null) {
                Toast.makeText(this, "Please log in to take quiz", Toast.LENGTH_SHORT).show();
                return;
            }
            
            int studentId = currentUser.getInt("user_id");
            
            Intent intent = new Intent(this, StudentQuizActivity.class);
            intent.putExtra("course_id", courseId);
            intent.putExtra("course_title", courseTitle);
            intent.putExtra("student_id", studentId);  // Pass student_id!
            startActivity(intent);
        } catch (Exception e) {
            Log.e("CourseVideosList", "Error opening quiz", e);
            Toast.makeText(this, "Error opening quiz", Toast.LENGTH_SHORT).show();
        }
    }
}
