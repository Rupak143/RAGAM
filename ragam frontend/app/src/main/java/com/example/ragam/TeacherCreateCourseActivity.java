package com.example.ragamfinal;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

public class TeacherCreateCourseActivity extends AppCompatActivity {
    private static final String TAG = "CreateCourse";
    
    private EditText courseTitleInput, courseDescriptionInput;
    private Button uploadVideoButton, launchCourseButton, deleteCourseButton;
    private ImageView backButton, homeIcon, profileIcon;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    
    private String selectedVideoPath;
    private ActivityResultLauncher<Intent> videoPickerLauncher;
    
    // Edit mode variables
    private boolean isEditMode = false;
    private JSONObject courseData;
    private int courseId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_createcourse);
        
        apiHelper = new ApiHelper(this);
        currentUser = apiHelper.getUserSession();
        
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Check if this is edit mode
        checkEditMode();
        
        initViews();
        setupVideoPickerLauncher();
        setupClickListeners();
        setupBottomNavigation();
        
        // Load course data if in edit mode
        if (isEditMode) {
            loadCourseDataForEdit();
        }
    }
    
    private void initViews() {
        courseTitleInput = findViewById(R.id.course_title);
        courseDescriptionInput = findViewById(R.id.course_description);
        uploadVideoButton = findViewById(R.id.btn_upload_video);
        launchCourseButton = findViewById(R.id.btn_launch_course);
        deleteCourseButton = findViewById(R.id.btn_delete_course);
        backButton = findViewById(R.id.back_button);
        
        // Bottom navigation
        homeIcon = findViewById(R.id.ic_home);
        profileIcon = findViewById(R.id.ic_profile);
    }
    
    private void setupVideoPickerLauncher() {
        videoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri videoUri = result.getData().getData();
                    if (videoUri != null) {
                        selectedVideoPath = videoUri.toString();
                        uploadVideoButton.setText("Video Selected ✓");
                        uploadVideoButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
                        Toast.makeText(this, "Video selected successfully", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        uploadVideoButton.setOnClickListener(v -> selectVideo());
        
        launchCourseButton.setOnClickListener(v -> {
            // Redirect to new multi-video course setup
            Intent intent = new Intent(this, TeacherCourseSetupActivity.class);
            if (currentUser != null) {
                intent.putExtra("instructor_id", currentUser.optString("user_id", ""));
            }
            startActivity(intent);
        });
        
        deleteCourseButton.setOnClickListener(v -> showDeleteConfirmation());
        
        // Add long press for debug mode
        launchCourseButton.setOnLongClickListener(v -> {
            testConnection();
            return true;
        });
    }
    
    private void selectVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        videoPickerLauncher.launch(Intent.createChooser(intent, "Select Video"));
    }
    
    private void createCourse() {
        String title = courseTitleInput.getText().toString().trim();
        String description = courseDescriptionInput.getText().toString().trim();
        
        if (title.isEmpty()) {
            courseTitleInput.setError("Course title is required");
            return;
        }
        
        if (description.isEmpty()) {
            courseDescriptionInput.setError("Course description is required");
            return;
        }
        
        // For edit mode, video is optional (can keep existing video)
        if (!isEditMode && selectedVideoPath == null) {
            Toast.makeText(this, "Please select a video", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Log the data being sent for debugging
        Log.d(TAG, isEditMode ? "Starting course update..." : "Starting course creation...");
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Description: " + description);
        Log.d(TAG, "Video Path: " + selectedVideoPath);
        
        // Disable button during operation
        launchCourseButton.setEnabled(false);
        launchCourseButton.setText(isEditMode ? "Updating Course..." : "Creating Course...");
        
        if (isEditMode) {
            // For now, we'll use the create method but add edit support later
            // You might want to implement updateCourse in ApiHelper
            Toast.makeText(this, "Course edit not fully implemented yet", Toast.LENGTH_SHORT).show();
            launchCourseButton.setEnabled(true);
            launchCourseButton.setText("Update Course");
            return;
        }
        
        try {
            int teacherId = currentUser.getInt("user_id");
            Log.d(TAG, "Teacher ID: " + teacherId);
            
            apiHelper.createCourse(teacherId, title, description, selectedVideoPath, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            Log.d(TAG, "Create course response: " + response);
                            JSONObject jsonResponse = new JSONObject(response);
                            String status = jsonResponse.getString("status");
                            
                            if ("success".equals(status)) {
                                Toast.makeText(TeacherCreateCourseActivity.this, 
                                    "Course created successfully!", Toast.LENGTH_LONG).show();
                                
                                // Reset form
                                courseTitleInput.setText("");
                                courseDescriptionInput.setText("");
                                selectedVideoPath = null;
                                uploadVideoButton.setText("Upload Video");
                                uploadVideoButton.setBackgroundColor(ContextCompat.getColor(
                                    TeacherCreateCourseActivity.this, android.R.color.holo_blue_light));
                                
                                // Navigate back to teacher home
                                Intent intent = new Intent(TeacherCreateCourseActivity.this, TeacherMainContainerActivity.class);
                                intent.putExtra("fragment", "home");
                                startActivity(intent);
                                finish();
                            } else {
                                String message = jsonResponse.optString("message", "Course creation failed");
                                Toast.makeText(TeacherCreateCourseActivity.this, 
                                    message, Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Course creation failed: " + message);
                                Log.e(TAG, "Full response: " + response);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing create course response: " + response, e);
                            Toast.makeText(TeacherCreateCourseActivity.this, 
                                "Course creation failed: Invalid response format", Toast.LENGTH_SHORT).show();
                        }
                        
                        launchCourseButton.setEnabled(true);
                        launchCourseButton.setText("Launch Course");
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Course creation network error: " + error);
                        Toast.makeText(TeacherCreateCourseActivity.this, 
                            "Network Error: " + error, Toast.LENGTH_LONG).show();
                        launchCourseButton.setEnabled(true);
                        launchCourseButton.setText("Launch Course");
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error creating course", e);
            Toast.makeText(this, "Error creating course: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            launchCourseButton.setEnabled(true);
            launchCourseButton.setText("Launch Course");
        }
    }
    
    private void testConnection() {
        Toast.makeText(this, "Testing backend connection...", Toast.LENGTH_SHORT).show();
        
        // First test basic connectivity
        apiHelper.testConnection(new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Basic connection test successful: " + response);
                    
                    // Now test the debug endpoint
                    apiHelper.debugCourseCreation(new ApiHelper.ApiCallback() {
                        @Override
                        public void onSuccess(String debugResponse) {
                            runOnUiThread(() -> {
                                Log.d(TAG, "Debug test successful: " + debugResponse);
                                Toast.makeText(TeacherCreateCourseActivity.this, 
                                    "✅ Backend connection successful! Ready to create courses.", 
                                    Toast.LENGTH_LONG).show();
                            });
                        }
                        
                        @Override
                        public void onError(String debugError) {
                            runOnUiThread(() -> {
                                Log.e(TAG, "Debug test failed: " + debugError);
                                Toast.makeText(TeacherCreateCourseActivity.this, 
                                    "⚠️ Basic connection OK, but API has issues: " + debugError, 
                                    Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Basic connection test failed: " + error);
                    Toast.makeText(TeacherCreateCourseActivity.this, 
                        "❌ Backend connection failed: " + error + 
                        "\n\nCheck:\n1. XAMPP is running\n2. IP address is correct\n3. Same WiFi network", 
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void checkEditMode() {
        // Check if course data was passed for editing
        String courseDataString = getIntent().getStringExtra("course_data");
        isEditMode = getIntent().getBooleanExtra("edit_mode", false);
        
        if (isEditMode && courseDataString != null) {
            try {
                courseData = new JSONObject(courseDataString);
                courseId = courseData.getInt("course_id");
                Log.d(TAG, "Edit mode enabled for course ID: " + courseId);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing course data for edit", e);
                isEditMode = false;
            }
        }
    }
    
    private void loadCourseDataForEdit() {
        try {
            if (courseData != null) {
                // Load existing course data into the form
                courseTitleInput.setText(courseData.getString("course_title"));
                courseDescriptionInput.setText(courseData.getString("course_description"));
                
                // Update UI elements for edit mode
                updateUIForEditMode();
                
                Log.d(TAG, "Course data loaded for editing: " + courseData.getString("course_title"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading course data for edit", e);
            Toast.makeText(this, "Error loading course data", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateUIForEditMode() {
        // Update title and button text for edit mode
        TextView titleText = findViewById(R.id.page_title);
        if (titleText != null) {
            titleText.setText("Edit Course");
        }
        launchCourseButton.setText("Update Course");
        
        // Show delete button only in edit mode
        if (deleteCourseButton != null) {
            deleteCourseButton.setVisibility(android.view.View.VISIBLE);
        }
    }
    
    private void showDeleteConfirmation() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete this course? This action cannot be undone. All student enrollments will be removed.")
            .setPositiveButton("Delete", (dialog, which) -> deleteCourse())
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
    
    private void deleteCourse() {
        if (!isEditMode || courseId == 0) {
            Toast.makeText(this, "Cannot delete course", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Disable delete button during operation
        deleteCourseButton.setEnabled(false);
        deleteCourseButton.setText("Deleting...");
        
        apiHelper.deleteCourse(courseId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        
                        // Handle both "status" and "success" fields for compatibility
                        boolean isSuccess = false;
                        if (jsonResponse.has("status")) {
                            isSuccess = "success".equals(jsonResponse.getString("status"));
                        } else if (jsonResponse.has("success")) {
                            isSuccess = jsonResponse.getBoolean("success");
                        }
                        
                        if (isSuccess) {
                            Toast.makeText(TeacherCreateCourseActivity.this, 
                                "Course deleted successfully", Toast.LENGTH_SHORT).show();
                            
                            // Set result to indicate course was deleted
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            String message = jsonResponse.optString("message", "Failed to delete course");
                            Toast.makeText(TeacherCreateCourseActivity.this, 
                                message, Toast.LENGTH_SHORT).show();
                            resetDeleteButton();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing delete response", e);
                        Toast.makeText(TeacherCreateCourseActivity.this, 
                            "Error deleting course", Toast.LENGTH_SHORT).show();
                        resetDeleteButton();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error deleting course: " + error);
                    Toast.makeText(TeacherCreateCourseActivity.this, 
                        "Failed to delete course: " + error, Toast.LENGTH_SHORT).show();
                    resetDeleteButton();
                });
            }
        });
    }
    
    private void resetDeleteButton() {
        deleteCourseButton.setEnabled(true);
        deleteCourseButton.setText("Delete Course");
    }
    
    private void setupBottomNavigation() {
        homeIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, TeacherMainContainerActivity.class);
            intent.putExtra("fragment", "home");
            startActivity(intent);
            finish();
        });
        
        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });
    }
}
