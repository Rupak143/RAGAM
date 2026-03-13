package com.example.ragamfinal;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

public class TeacherViewCourseActivity extends AppCompatActivity {
    private static final String TAG = "TeacherViewCourse";
    
    private ImageView backButton;
    private TextView courseTitle, courseDescription, courseCategory, videoCount, enrolledCount;
    private Button deleteCourseButton;
    private ApiHelper apiHelper;
    private JSONObject courseData;
    private int courseId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_viewcourse);
        
        apiHelper = new ApiHelper(this);
        
        // Get course data from intent
        String courseDataString = getIntent().getStringExtra("course_data");
        if (courseDataString != null) {
            try {
                courseData = new JSONObject(courseDataString);
                courseId = courseData.getInt("course_id");
            } catch (Exception e) {
                Log.e(TAG, "Error parsing course data", e);
                Toast.makeText(this, "Error loading course data", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            Toast.makeText(this, "No course data provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupClickListeners();
        loadCourseData();
    }
    
    private void initViews() {
        backButton = findViewById(R.id.back_button);
        courseTitle = findViewById(R.id.course_title);
        courseDescription = findViewById(R.id.course_description);
        courseCategory = findViewById(R.id.course_category);
        videoCount = findViewById(R.id.video_count);
        enrolledCount = findViewById(R.id.enrolled_count);
        deleteCourseButton = findViewById(R.id.btn_launch_video); // Repurposed button
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        deleteCourseButton.setOnClickListener(v -> showDeleteConfirmation());
    }
    
    private void loadCourseData() {
        try {
            String title = courseData.optString("course_title", courseData.optString("title", "Untitled Course"));
            String description = courseData.optString("course_description", courseData.optString("description", "No description"));

            courseTitle.setText(title);
            courseDescription.setText(description);
            courseCategory.setText(courseData.optString("category", "Music"));
            
            int videos = courseData.optInt("video_count", 0);
            videoCount.setText(videos + " videos");
            
            int enrolled = courseData.optInt("enrolled_count", 0);
            enrolledCount.setText(enrolled + " students");
        } catch (Exception e) {
            Log.e(TAG, "Error loading course data", e);
            Toast.makeText(this, "Error displaying course data", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete this course? This action cannot be undone. All student enrollments will be removed.")
            .setPositiveButton("Delete", (dialog, which) -> deleteCourse())
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
    
    private void deleteCourse() {
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
                            Toast.makeText(TeacherViewCourseActivity.this, 
                                "Course deleted successfully", Toast.LENGTH_SHORT).show();
                            
                            // Set result to indicate course was deleted
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            String message = jsonResponse.optString("message", "Failed to delete course");
                            Toast.makeText(TeacherViewCourseActivity.this, 
                                message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing delete response", e);
                        Toast.makeText(TeacherViewCourseActivity.this, 
                            "Error deleting course", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Error deleting course: " + error);
                    Toast.makeText(TeacherViewCourseActivity.this, 
                        "Failed to delete course: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
