package com.example.ragamfinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ragamfinal.utils.ApiHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StudentCompletedCoursesActivity extends AppCompatActivity {
    
    private LinearLayout coursesContainer;
    private TextView emptyStateText;
    private TextView headerTitle;
    private TextView tvTotalCourses;
    private ApiHelper apiHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_completed_courses);
        
        initViews();
        setupClickListeners();
        loadCompletedCourses();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadCompletedCourses();
    }
    
    private void initViews() {
        coursesContainer = findViewById(R.id.rv_completed_courses);
        headerTitle = findViewById(R.id.header_title);
        tvTotalCourses = findViewById(R.id.tv_total_courses);
        emptyStateText = findViewById(R.id.empty_state_text);
        
        apiHelper = new ApiHelper(this);
    }
    
    private void setupClickListeners() {
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        
        Button browseCourses = findViewById(R.id.btn_browse_courses);
        if (browseCourses != null) {
            browseCourses.setOnClickListener(v -> {
                Intent intent = new Intent(this, CoursesActivity.class);
                startActivity(intent);
            });
        }
    }
    
    private void loadCompletedCourses() {
        JSONObject currentUser = apiHelper.getUserSession();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int studentId = currentUser.getInt("user_id");
            
            apiHelper.getCompletedCourses(studentId, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            
                            boolean isSuccess = jsonResponse.has("status") ? 
                                "success".equals(jsonResponse.getString("status")) : 
                                jsonResponse.optBoolean("success", false);
                            
                            if (isSuccess && jsonResponse.has("data")) {
                                JSONArray completedCourses = jsonResponse.getJSONArray("data");
                                
                                if (completedCourses.length() > 0) {
                                    displayCompletedCourses(completedCourses);
                                    emptyStateText.setVisibility(View.GONE);
                                } else {
                                    coursesContainer.removeAllViews();
                                    tvTotalCourses.setText("Total Completed: 0");
                                    emptyStateText.setVisibility(View.VISIBLE);
                                    emptyStateText.setText("No completed courses yet.\nComplete courses to see them here!");
                                }
                            } else {
                                coursesContainer.removeAllViews();
                                tvTotalCourses.setText("Total Completed: 0");
                                emptyStateText.setVisibility(View.VISIBLE);
                                emptyStateText.setText("No completed courses yet.\nComplete courses to see them here!");
                            }
                        } catch (Exception e) {
                            Toast.makeText(StudentCompletedCoursesActivity.this, 
                                "Error loading courses", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(StudentCompletedCoursesActivity.this, 
                            "Network error: " + error, Toast.LENGTH_SHORT).show();
                        emptyStateText.setText("Error loading courses");
                        emptyStateText.setVisibility(View.VISIBLE);
                    });
                }
            });
            
        } catch (JSONException e) {
            Toast.makeText(this, "Error loading courses", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void displayCompletedCourses(JSONArray completedCourses) {
        coursesContainer.removeAllViews();
        
        try {
            int totalCourses = completedCourses.length();
            
            for (int i = 0; i < completedCourses.length(); i++) {
                JSONObject course = completedCourses.getJSONObject(i);
                
                String title = course.optString("course_title", course.optString("title", "Course"));
                String description = course.optString("course_description", course.optString("description", ""));
                String instructor = course.optString("teacher_name", course.optString("instructor_name", "Unknown"));
                int courseId = course.optInt("course_id", 0);
                int totalVideos = course.optInt("total_videos", 0);
                int completedVideos = course.optInt("completed_videos", 0);
                
                addCompletedCourse(title, description, instructor, totalVideos, completedVideos, courseId, course);
            }
            
            tvTotalCourses.setText("Total Completed: " + totalCourses);
            
        } catch (Exception e) {
            Toast.makeText(this, "Error displaying courses", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void addCompletedCourse(String title, String description, String instructor, 
                                  int totalVideos, int completedVideos, int courseId, JSONObject courseData) {
        
        View courseView = LayoutInflater.from(this).inflate(R.layout.item_completed_course, 
                                                           coursesContainer, false);
        
        TextView titleText = courseView.findViewById(R.id.course_title);
        TextView descriptionText = courseView.findViewById(R.id.course_description);
        TextView instructorText = courseView.findViewById(R.id.instructor_name);
        TextView completionBadge = courseView.findViewById(R.id.completion_badge);
        TextView videosCompletedText = courseView.findViewById(R.id.videos_completed_text);
        
        titleText.setText(title);
        descriptionText.setText(description);
        instructorText.setText("Instructor: " + instructor);
        completionBadge.setText("✓ COMPLETED");
        videosCompletedText.setText(completedVideos + "/" + totalVideos + " videos completed");

        coursesContainer.addView(courseView);
    }
}
