package com.example.ragamfinal;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

public class SingleVideoPlayerActivity extends AppCompatActivity {
    
    private VideoView videoView;
    private TextView videoTitleText;
    private ImageView backButton;
    private ProgressBar progressBar;
    
    private boolean videoCompletedWatching = false;
    private JSONArray videosArray;
    private int videoIndex;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_video_player);
        
        initViews();
        loadIntentData();
        
        if (videosArray != null && videosArray.length() > videoIndex) {
            setupVideoPlayer();
        } else {
            Toast.makeText(this, "No video data available", Toast.LENGTH_SHORT).show();
            finish();
        }
        
        setupClickListeners();
    }
    
    private void initViews() {
        videoView = findViewById(R.id.video_view);
        videoTitleText = findViewById(R.id.video_title);
        backButton = findViewById(R.id.back_button);
        progressBar = findViewById(R.id.progress_bar);
        
        if (videoView == null) {
            Toast.makeText(this, "Error: Video view not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }
    
    private void setupClickListeners() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackPressed());
        }
    }
    
    private void loadIntentData() {
        Intent intent = getIntent();
        videoIndex = intent.getIntExtra("video_index", 0);
        String videosArrayString = intent.getStringExtra("videos_array");
        
        if (videosArrayString != null) {
            try {
                videosArray = new JSONArray(videosArrayString);
            } catch (Exception e) {
                Log.e("SingleVideoPlayer", "Error parsing videos array", e);
                Toast.makeText(this, "Error loading video data", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
    private void setupVideoPlayer() {
        try {
            JSONObject video = videosArray.getJSONObject(videoIndex);
            String videoTitle = video.optString("title", "Video");
            videoTitleText.setText(videoTitle);
            
            String videoUrl = video.optString("video_url", "");
            String fullVideoUrl;
            
            Log.d("SingleVideoPlayer", "Raw video_url from database: '" + videoUrl + "'");
            Log.d("SingleVideoPlayer", "BASE_URL: " + com.example.ragamfinal.config.AppConfig.BASE_URL);
            
            if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
                fullVideoUrl = videoUrl;
                Log.d("SingleVideoPlayer", "Using full URL as-is");
            } else if (!videoUrl.isEmpty() && !videoUrl.equals("local_storage") && !videoUrl.equals("NULL")) {
                if (videoUrl.startsWith("ragamfinal/")) {
                    videoUrl = videoUrl.substring("ragamfinal/".length());
                    Log.d("SingleVideoPlayer", "Removed 'ragamfinal/' prefix from path");
                }
                
                fullVideoUrl = com.example.ragamfinal.config.AppConfig.BASE_URL + videoUrl;
                Log.d("SingleVideoPlayer", "Constructed URL from relative path");
            } else {
                Log.w("SingleVideoPlayer", "No video URL found, using demo video");
                fullVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
                Toast.makeText(this, "No video file uploaded. Playing demo.", Toast.LENGTH_SHORT).show();
            }
            
            Log.d("SingleVideoPlayer", "Final video URL: " + fullVideoUrl);
            
            if (!fullVideoUrl.startsWith("http://") && !fullVideoUrl.startsWith("https://")) {
                Log.e("SingleVideoPlayer", "Invalid URL format: " + fullVideoUrl);
                Toast.makeText(this, "Invalid video URL: " + fullVideoUrl, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            Uri videoUri = Uri.parse(fullVideoUrl);
            videoView.setVideoURI(videoUri);
            
            MediaController mediaController = new CustomMediaController(this);
            videoView.setMediaController(mediaController);
            mediaController.setAnchorView(videoView);
            
            videoView.setOnPreparedListener(mp -> {
                progressBar.setVisibility(View.GONE);
                mp.setOnSeekCompleteListener(mediaPlayer -> {
                    if (mediaPlayer.getCurrentPosition() > mp.getCurrentPosition()) {
                        mediaPlayer.seekTo(mp.getCurrentPosition());
                        Toast.makeText(SingleVideoPlayerActivity.this, 
                            "Skipping not allowed. Watch complete video.", Toast.LENGTH_SHORT).show();
                    }
                });
                videoView.start();
            });
            
            videoView.setOnCompletionListener(mp -> {
                videoCompletedWatching = true;
                Toast.makeText(this, "Video completed!", Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            });
            
            final String finalVideoUrl = fullVideoUrl;
            videoView.setOnErrorListener((mp, what, extra) -> {
                String errorMsg = "Error playing video";
                if (what == 1) {
                    errorMsg = "Server error - Cannot access video file";
                } else if (what == 100) {
                    errorMsg = "Video not found or format error";
                }
                
                Toast.makeText(this, errorMsg + "\n\nURL: " + finalVideoUrl, Toast.LENGTH_LONG).show();
                Log.e("SingleVideoPlayer", "Video error: " + what + ", " + extra);
                Log.e("SingleVideoPlayer", "Attempted URL: " + finalVideoUrl);
                finish();
                return true;
            });
            
        } catch (Exception e) {
            Log.e("SingleVideoPlayer", "Error setting up video player", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    @Override
    public void onBackPressed() {
        if (videoCompletedWatching) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, "Please complete the video first", Toast.LENGTH_SHORT).show();
        }
    }
    
    private class CustomMediaController extends MediaController {
        public CustomMediaController(android.content.Context context) {
            super(context);
        }
        
        @Override
        public void show(int timeout) {
            super.show(0);
        }
        
        @Override
        public void hide() {
        }
    }
}
