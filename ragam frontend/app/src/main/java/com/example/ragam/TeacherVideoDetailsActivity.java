package com.example.ragamfinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TeacherVideoDetailsActivity extends AppCompatActivity {
    
    private TextView tvHeader, tvProgress;
    private EditText etVideoTitle, etVideoDescription;
    private Button btnNext;

    private String instructorId, courseTitle, courseDescription, courseCategory;
    private int videoCount, currentVideoIndex;
    private String[] videoTitles, videoDescriptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_video_details);
        
        // Get data from intent
        instructorId = getIntent().getStringExtra("instructor_id");
        courseTitle = getIntent().getStringExtra("course_title");
        courseDescription = getIntent().getStringExtra("course_description");
        courseCategory = getIntent().getStringExtra("course_category");
        videoCount = getIntent().getIntExtra("video_count", 0);
        currentVideoIndex = getIntent().getIntExtra("current_video_index", 0);
        
        // Initialize or get existing video arrays
        videoTitles = getIntent().getStringArrayExtra("video_titles");
        videoDescriptions = getIntent().getStringArrayExtra("video_descriptions");
        
        if (videoTitles == null) {
            videoTitles = new String[videoCount];
            videoDescriptions = new String[videoCount];
        }
        
        initViews();
        setupUI();
        setupClickListeners();
    }
    
    private void initViews() {
        tvHeader = findViewById(R.id.tv_header);
        tvProgress = findViewById(R.id.tv_progress);
        etVideoTitle = findViewById(R.id.et_video_title);
        etVideoDescription = findViewById(R.id.et_video_description);
        btnNext = findViewById(R.id.btn_next);
    }
    
    private void setupUI() {
        // Update header and progress
        tvHeader.setText("Video Details");
        tvProgress.setText("Video " + (currentVideoIndex + 1) + " of " + videoCount);
        
        // Set hints
        etVideoTitle.setHint("Enter title for video " + (currentVideoIndex + 1));
        etVideoDescription.setHint("Enter description for video " + (currentVideoIndex + 1));
        
        // Update button text
        if (currentVideoIndex < videoCount - 1) {
            btnNext.setText("Next Video (" + (currentVideoIndex + 2) + "/" + videoCount + ")");
        } else {
            btnNext.setText("Create Test Paper");
        }
        
        // Pre-fill if data exists
        if (videoTitles[currentVideoIndex] != null) {
            etVideoTitle.setText(videoTitles[currentVideoIndex]);
        }
        if (videoDescriptions[currentVideoIndex] != null) {
            etVideoDescription.setText(videoDescriptions[currentVideoIndex]);
        }
        
        // Update action bar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Video " + (currentVideoIndex + 1) + " Details");
        }
    }
    
    private void setupClickListeners() {
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleNext();
            }
        });
    }
    
    private void handleNext() {
        String videoTitle = etVideoTitle.getText().toString().trim();
        String videoDescription = etVideoDescription.getText().toString().trim();
        
        if (videoTitle.isEmpty()) {
            etVideoTitle.setError("Video title is required");
            etVideoTitle.requestFocus();
            return;
        }
        
        if (videoDescription.isEmpty()) {
            etVideoDescription.setError("Video description is required");
            etVideoDescription.requestFocus();
            return;
        }
        
        // Save current video details
        videoTitles[currentVideoIndex] = videoTitle;
        videoDescriptions[currentVideoIndex] = videoDescription;
        
        Toast.makeText(this, "✅ Video " + (currentVideoIndex + 1) + " saved!", Toast.LENGTH_SHORT).show();
        
        if (currentVideoIndex < videoCount - 1) {
            // Go to next video
            Intent intent = new Intent(this, TeacherVideoDetailsActivity.class);
            intent.putExtra("instructor_id", instructorId);
            intent.putExtra("course_title", courseTitle);
            intent.putExtra("course_description", courseDescription);
            intent.putExtra("course_category", courseCategory);
            intent.putExtra("video_count", videoCount);
            intent.putExtra("current_video_index", currentVideoIndex + 1);
            intent.putExtra("video_titles", videoTitles);
            intent.putExtra("video_descriptions", videoDescriptions);
            startActivity(intent);
            finish();
        } else {
            // All videos done, go to test paper
            Intent intent = new Intent(this, TeacherTestPaperActivity.class);
            intent.putExtra("instructor_id", instructorId);
            intent.putExtra("course_title", courseTitle);
            intent.putExtra("course_description", courseDescription);
            intent.putExtra("course_category", courseCategory);
            intent.putExtra("video_count", videoCount);
            intent.putExtra("video_titles", videoTitles);
            intent.putExtra("video_descriptions", videoDescriptions);
            startActivity(intent);
            finish();
        }
    }
}