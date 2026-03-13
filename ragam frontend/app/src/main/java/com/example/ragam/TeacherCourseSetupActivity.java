package com.example.ragamfinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TeacherCourseSetupActivity extends AppCompatActivity {
    
    // UI Components
    private EditText etCourseTitle, etCourseDescription, etVideoCountInput;
    private Spinner spinnerCourseCategory;
    private Button btnNext, btnNextToVideoDetails;
    private LinearLayout courseDetailsSection, videoCountSection;
    
    // Course data
    private String instructorId;
    private int currentStep = 1;
    private String courseTitle, courseDescription, courseCategory;
    private int videoCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_new_course_simple);
        
        instructorId = getIntent().getStringExtra("instructor_id");
        if (instructorId == null || instructorId.isEmpty()) {
            Toast.makeText(this, "Error: No instructor ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupClickListeners();
    }
    
    private void initViews() {
        courseDetailsSection = findViewById(R.id.course_details_section);
        etCourseTitle = findViewById(R.id.course_title);
        etCourseDescription = findViewById(R.id.course_description);
        spinnerCourseCategory = findViewById(R.id.course_category);
        btnNext = findViewById(R.id.btn_next_to_videos);
        
        // Setup category spinner with predefined categories
        // IMPORTANT: These MUST match the exact category_name values in the database
        String[] categories = {
            "Select Category",
            "Vocal Training",          // ID: 1 - Exact match
            "Instrumental Music",      // ID: 2 - Exact match
            "Devotional Music",        // ID: 6 - FIXED: was "Bhakti/Devotional Music"
            "Kids Special"             // ID: 8 - FIXED: was "Kid's Special Music Training"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourseCategory.setAdapter(adapter);
        
        videoCountSection = findViewById(R.id.video_count_section);
        etVideoCountInput = findViewById(R.id.video_count_input);
        btnNextToVideoDetails = findViewById(R.id.btn_next_to_video_details);
    }
    
    private void setupClickListeners() {
        btnNext.setOnClickListener(v -> {
            validateCourseDetailsAndProceed();
        });
        
        btnNextToVideoDetails.setOnClickListener(v -> {
            validateVideoCountAndProceed();
        });
    }
    
    private void validateCourseDetailsAndProceed() {
        courseTitle = etCourseTitle.getText().toString().trim();
        courseDescription = etCourseDescription.getText().toString().trim();
        courseCategory = spinnerCourseCategory.getSelectedItem().toString();
        
        if (courseTitle.isEmpty()) {
            etCourseTitle.setError("Course title is required");
            return;
        }
        
        if (courseDescription.isEmpty()) {
            etCourseDescription.setError("Course description is required");
            return;
        }
        
        if (courseCategory.equals("Select Category")) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show video count section
        courseDetailsSection.setVisibility(View.GONE);
        videoCountSection.setVisibility(View.VISIBLE);
        Toast.makeText(this, "✅ Course details saved! Now enter video count.", Toast.LENGTH_SHORT).show();
    }
    
    private void validateVideoCountAndProceed() {
        String videoCountStr = etVideoCountInput.getText().toString().trim();
        
        if (videoCountStr.isEmpty()) {
            etVideoCountInput.setError("Number of videos is required");
            return;
        }
        
        try {
            videoCount = Integer.parseInt(videoCountStr);
            if (videoCount <= 0 || videoCount > 20) {
                etVideoCountInput.setError("Enter 1-20 videos");
                return;
            }
        } catch (NumberFormatException e) {
            etVideoCountInput.setError("Please enter a valid number");
            return;
        }
        
        // Go to video details
        Intent intent = new Intent(this, TeacherVideoDetailsActivity.class);
        intent.putExtra("course_title", courseTitle);
        intent.putExtra("course_description", courseDescription);
        intent.putExtra("course_category", courseCategory);
        intent.putExtra("instructor_id", instructorId);
        intent.putExtra("video_count", videoCount);
        intent.putExtra("current_video_index", 0);
        startActivity(intent);
        finish();
    }
}