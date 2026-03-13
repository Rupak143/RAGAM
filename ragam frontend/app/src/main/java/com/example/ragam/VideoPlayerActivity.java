package com.example.ragamfinal;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONArray;
import org.json.JSONObject;

public class VideoPlayerActivity extends AppCompatActivity {
    
    private VideoView videoView;
    private TextView courseTitleText, lessonTitleText, timeDisplay;
    private ImageView backButton, playButtonOverlay, fullscreenButton, playPauseButton, previousButton, nextButton;
    private View controlOverlay;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    private int courseId;
    private String courseTitle;
    
    private boolean isFullscreen = true;
    private boolean isControlsVisible = true;
    private Handler hideControlsHandler = new Handler();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set fullscreen mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        setContentView(R.layout.activity_video_player);
        
        // Initialize API helper and user session first
        apiHelper = new ApiHelper(this);
        currentUser = apiHelper.getUserSession();
        
        initViews();
        loadIntentData();
        setupClickListeners();
        loadCourseContent();
        
        // Auto-hide controls after 3 seconds
        scheduleHideControls();
    }
    
    private void initViews() {
        videoView = findViewById(R.id.video_view);
        courseTitleText = findViewById(R.id.course_title);
        lessonTitleText = findViewById(R.id.lesson_title);
        timeDisplay = findViewById(R.id.time_display);
        backButton = findViewById(R.id.back_button);
        playButtonOverlay = findViewById(R.id.play_button_overlay);
        fullscreenButton = findViewById(R.id.fullscreen_button);
        playPauseButton = findViewById(R.id.play_pause_button);
        previousButton = findViewById(R.id.previous_button);
        nextButton = findViewById(R.id.next_button);
        controlOverlay = findViewById(R.id.control_overlay);
        
        // Enable MediaController for timeline and scrubbing controls
        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        mediaController.setAnchorView(videoView);
    }
    
    private void loadIntentData() {
        Intent intent = getIntent();
        courseId = intent.getIntExtra("course_id", 0);
        courseTitle = intent.getStringExtra("course_title");
        
        if (courseTitle != null) {
            courseTitleText.setText(courseTitle);
        }
        
        if (courseId == 0) {
            Toast.makeText(this, "Invalid course", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        // Main play button overlay
        playButtonOverlay.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                playButtonOverlay.setImageResource(android.R.drawable.ic_media_play);
                playButtonOverlay.setVisibility(View.VISIBLE);
                playPauseButton.setImageResource(android.R.drawable.ic_media_play);
            } else {
                videoView.start();
                playButtonOverlay.setVisibility(View.GONE);
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            }
        });
        
        // Bottom control play/pause button
        playPauseButton.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                playButtonOverlay.setImageResource(android.R.drawable.ic_media_play);
                playButtonOverlay.setVisibility(View.VISIBLE);
            } else {
                videoView.start();
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                playButtonOverlay.setVisibility(View.GONE);
            }
        });
        
        // Video view click to show/hide controls
        videoView.setOnClickListener(v -> toggleControls());
        
        // Control overlay click to hide controls
        controlOverlay.setOnClickListener(v -> toggleControls());
        
        // Fullscreen button
        fullscreenButton.setOnClickListener(v -> toggleFullscreen());
        
        // Previous/Next buttons (placeholder for now)
        previousButton.setOnClickListener(v -> {
            Toast.makeText(this, "Previous lesson", Toast.LENGTH_SHORT).show();
        });
        
        nextButton.setOnClickListener(v -> {
            Toast.makeText(this, "Next lesson", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void loadCourseContent() {
        if (currentUser == null) {
            Toast.makeText(this, "Please login to access course content", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        try {
            int studentId = currentUser.getInt("user_id");
            
            // Get course lessons and check enrollment
            apiHelper.getCourseDetails(courseId, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            
                            boolean isSuccess = false;
                            if (jsonResponse.has("status")) {
                                isSuccess = "success".equals(jsonResponse.getString("status"));
                            } else if (jsonResponse.has("success")) {
                                isSuccess = jsonResponse.getBoolean("success");
                            }
                            
                            if (isSuccess) {
                                JSONObject courseData = jsonResponse.getJSONObject("data");
                                
                                // Update course title
                                String title = courseData.optString("course_title", "Course Video");
                                courseTitleText.setText(title);
                                
                                // Load the first video from course lessons
                                loadCourseVideos(studentId);
                            } else {
                                String message = jsonResponse.optString("message", "Failed to load course");
                                Toast.makeText(VideoPlayerActivity.this, message, Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } catch (Exception e) {
                            Log.e("VideoPlayer", "Error parsing course response", e);
                            Toast.makeText(VideoPlayerActivity.this, "Error loading course", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(VideoPlayerActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            });
            
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error loading course content", e);
            Toast.makeText(this, "Error loading course content", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadCourseVideos(int studentId) {
        // Get course videos from backend - FIXED: Use AppConfig.BASE_URL
        String url = com.example.ragamfinal.config.AppConfig.BASE_URL + "courses.php?action=get_course_videos&course_id=" + courseId;
        
        StringRequest request = new StringRequest(Request.Method.GET, url,
            response -> {
                runOnUiThread(() -> {
                    try {
                        Log.d("VideoPlayer", "Videos response: " + response);
                        JSONObject jsonResponse = new JSONObject(response);
                        
                        boolean isSuccess = jsonResponse.optBoolean("success", false);
                        
                        if (isSuccess && jsonResponse.has("data")) {
                            JSONArray videos = jsonResponse.getJSONArray("data");
                            
                            if (videos.length() > 0) {
                                // Load the first video - FIXED: Load actual video URL
                                JSONObject firstVideo = videos.getJSONObject(0);
                                String videoTitle = firstVideo.optString("title", "Video 1");
                                String videoUrl = firstVideo.optString("video_url", "");
                                
                                lessonTitleText.setText(videoTitle);
                                
                                // Construct full video URL from relative path
                                String fullVideoUrl;
                                if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
                                    // Already a full URL (demo videos or external links)
                                    fullVideoUrl = videoUrl;
                                } else if (!videoUrl.isEmpty() && !videoUrl.equals("local_storage") && !videoUrl.equals("NULL")) {
                                    // Relative path from database - convert to full URL
                                    fullVideoUrl = com.example.ragamfinal.config.AppConfig.BASE_URL + videoUrl;
                                } else {
                                    // Fallback to demo video if no valid URL
                                    fullVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
                                    Toast.makeText(VideoPlayerActivity.this, 
                                        "No video file uploaded for this lesson. Playing demo.", Toast.LENGTH_SHORT).show();
                                }
                                
                                Log.d("VideoPlayer", "Loading video: " + fullVideoUrl);
                                loadVideo(fullVideoUrl, videoTitle);
                            } else {
                                // No videos yet
                                Toast.makeText(VideoPlayerActivity.this, 
                                    "No videos uploaded yet. Loading demo video.", Toast.LENGTH_LONG).show();
                                loadVideo("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", 
                                         "Introduction to " + courseTitle);
                            }
                        } else {
                            String message = jsonResponse.optString("message", "Failed to load videos");
                            Toast.makeText(VideoPlayerActivity.this, message, Toast.LENGTH_SHORT).show();
                            // Fallback to sample video for testing
                            loadVideo("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", 
                                     "Introduction to " + courseTitle);
                        }
                    } catch (Exception e) {
                        Log.e("VideoPlayer", "Error parsing lessons response", e);
                        Toast.makeText(VideoPlayerActivity.this, "Error loading lessons", Toast.LENGTH_SHORT).show();
                        // Fallback to sample video for testing
                        loadVideo("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", 
                                 "Introduction to " + courseTitle);
                    }
                });
            },
            error -> {
                runOnUiThread(() -> {
                    Log.e("VideoPlayer", "Error loading videos: " + error.toString());
                    // For now, load sample video
                    loadVideo("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", 
                             "Introduction to " + courseTitle);
                    Toast.makeText(VideoPlayerActivity.this, "Loading sample video for demonstration", Toast.LENGTH_SHORT).show();
                });
            }
        );
        
        // Add request to queue using Volley directly
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
    
    private void loadVideo(String videoUrl, String lessonTitle) {
        lessonTitleText.setText(lessonTitle);
        
        try {
            // Handle different types of video URLs
            Uri videoUri;
            
            if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
                // This is already a complete web URL
                videoUri = Uri.parse(videoUrl);
                Log.d("VideoPlayer", "Loading web URL: " + videoUri.toString());
            } else if (videoUrl.startsWith("videos/") || videoUrl.endsWith(".mp4") || videoUrl.endsWith(".avi")) {
                // This is a relative path to server video - FIXED PATH
                    String baseUrl = "http://192.168.48.1/ragamfinal/uploads/";
                videoUri = Uri.parse(baseUrl + videoUrl);
                Log.d("VideoPlayer", "Loading server video: " + videoUri.toString());
            } else {
                // Fallback: try as relative path anyway - FIXED PATH
                    String baseUrl = "http://192.168.48.1/ragamfinal/uploads/";
                videoUri = Uri.parse(baseUrl + videoUrl);
                Log.d("VideoPlayer", "Loading fallback URL: " + videoUri.toString());
            }
            
            videoView.setVideoURI(videoUri);
            
            videoView.setOnPreparedListener(mp -> {
                Toast.makeText(VideoPlayerActivity.this, "Video ready to play", Toast.LENGTH_SHORT).show();
                playButtonOverlay.setVisibility(View.VISIBLE);
                playButtonOverlay.setImageResource(android.R.drawable.ic_media_play);
                
                // Set up listener for when video starts/stops
                mp.setOnInfoListener((mediaPlayer, what, extra) -> {
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        playButtonOverlay.setVisibility(View.GONE);
                    }
                    return false;
                });
            });
            
            videoView.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(VideoPlayerActivity.this, "Error loading video. Trying demo video...", Toast.LENGTH_LONG).show();
                Log.e("VideoPlayer", "Video error: " + what + ", " + extra + ", URL: " + videoUri.toString());
                
                // Show play button even on error, will load fallback
                playButtonOverlay.setVisibility(View.VISIBLE);
                
                // Load fallback demo video
                loadFallbackVideo();
                return true;
            });
            
            videoView.setOnCompletionListener(mp -> {
                Toast.makeText(VideoPlayerActivity.this, "Video completed", Toast.LENGTH_SHORT).show();
                // Here you could mark the lesson as completed in your API
            });
            
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error setting up video", e);
            Toast.makeText(this, "Error setting up video player", Toast.LENGTH_SHORT).show();
            loadFallbackVideo();
        }
    }
    
    private void loadFallbackVideo() {
        // Load a sample video for demo purposes
        try {
            String sampleVideoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
            Uri sampleUri = Uri.parse(sampleVideoUrl);
            
            Log.d("VideoPlayer", "Loading fallback video: " + sampleVideoUrl);
            Toast.makeText(this, "Loading demo video (original video unavailable)", Toast.LENGTH_LONG).show();
            
            videoView.setVideoURI(sampleUri);
            lessonTitleText.setText(lessonTitleText.getText() + " (Demo Video)");
            
            videoView.setOnPreparedListener(mp -> {
                Toast.makeText(VideoPlayerActivity.this, "Demo video ready to play", Toast.LENGTH_SHORT).show();
                videoView.start();
            });
            
        } catch (Exception e) {
            Log.e("VideoPlayer", "Failed to load fallback video: " + e.getMessage());
            Toast.makeText(this, "Unable to load any video content", Toast.LENGTH_LONG).show();
        }
    }
    
    private void toggleControls() {
        if (isControlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }
    
    private void showControls() {
        isControlsVisible = true;
        controlOverlay.setVisibility(View.VISIBLE);
        scheduleHideControls();
    }
    
    private void hideControls() {
        isControlsVisible = false;
        if (videoView.isPlaying()) {
            controlOverlay.setVisibility(View.GONE);
        }
    }
    
    private void scheduleHideControls() {
        hideControlsHandler.removeCallbacksAndMessages(null);
        hideControlsHandler.postDelayed(() -> {
            if (videoView.isPlaying()) {
                hideControls();
            }
        }, 3000); // Hide after 3 seconds
    }
    
    private void toggleFullscreen() {
        if (isFullscreen) {
            // Exit fullscreen (not implemented - this is already fullscreen)
            Toast.makeText(this, "Already in fullscreen mode", Toast.LENGTH_SHORT).show();
        } else {
            // Enter fullscreen
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            isFullscreen = true;
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        hideControlsHandler.removeCallbacksAndMessages(null);
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}
