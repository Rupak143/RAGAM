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

public class StudentEnrolledCoursesActivity extends AppCompatActivity {
    
    private LinearLayout coursesContainer;
    private TextView emptyStateText;
    private TextView headerTitle;
    private TextView tvTotalCourses;
    private TextView tvCompletedCourses;
    private TextView tvOverallProgress;
    private ApiHelper apiHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_enrolled_courses);
        
        initViews();
        setupClickListeners();
        loadEnrolledCourses();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh enrolled courses when returning to this activity
        loadEnrolledCourses();
    }
    
    private void initViews() {
        coursesContainer = findViewById(R.id.rv_enrolled_courses);
        headerTitle = findViewById(R.id.header_title);
        
        // Initialize statistics TextViews
        tvTotalCourses = findViewById(R.id.tv_total_courses);
        tvCompletedCourses = findViewById(R.id.tv_completed_courses);
        tvOverallProgress = findViewById(R.id.tv_overall_progress);
        
        apiHelper = new ApiHelper(this);
    }
    
    private void setupClickListeners() {
        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        
        findViewById(R.id.btn_browse_courses).setOnClickListener(v -> {
            Intent intent = new Intent(this, CoursesActivity.class);
            startActivity(intent);
        });
    }
    
    private void loadEnrolledCourses() {
        JSONObject currentUser = apiHelper.getUserSession();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int studentId = currentUser.getInt("user_id");
            
            // Fetch real enrolled courses from backend
            apiHelper.getEnrolledCourses(studentId, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            
                            boolean isSuccess = jsonResponse.has("status") ? 
                                "success".equals(jsonResponse.getString("status")) : 
                                jsonResponse.optBoolean("success", false);
                            
                            if (isSuccess && jsonResponse.has("data")) {
                                JSONArray enrolledCourses = jsonResponse.getJSONArray("data");
                                
                                if (enrolledCourses.length() > 0) {
                                    displayEnrolledCourses(enrolledCourses);
                                } else {
                                    coursesContainer.removeAllViews();
                                    updateStatistics(0, 0, 0, 0, 0);
                                }
                            } else {
                                coursesContainer.removeAllViews();
                                updateStatistics(0, 0, 0, 0, 0);
                            }
                        } catch (Exception e) {
                            Toast.makeText(StudentEnrolledCoursesActivity.this, 
                                "Error loading courses", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(StudentEnrolledCoursesActivity.this, 
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
    
    private void displayEnrolledCourses(JSONArray enrolledCourses) {
        coursesContainer.removeAllViews();
        
        try {
            int totalCourses = enrolledCourses.length();
            int completedCoursesCount = 0;
            int totalProgressSum = 0;
            int totalVideosAll = 0;
            int completedVideosAll = 0;
            
            for (int i = 0; i < enrolledCourses.length(); i++) {
                JSONObject course = enrolledCourses.getJSONObject(i);
                
                String title = course.optString("course_title", course.optString("title", "Course"));
                String description = course.optString("course_description", course.optString("description", ""));
                String instructor = course.optString("teacher_name", course.optString("instructor_name", "Unknown"));
                int courseId = course.optInt("course_id", 0);
                int totalVideos = course.optInt("video_count", 0);
                int completedVideos = course.optInt("completed_videos", 0);
                
                // Get progress from backend - Show actual progress
                int progressPercent = (int) course.optDouble("progress", 0.0);
                
                // Calculate statistics
                totalProgressSum += progressPercent;
                totalVideosAll += totalVideos;
                completedVideosAll += completedVideos;
                
                // Count courses that are 100% complete
                if (progressPercent >= 100) {
                    completedCoursesCount++;
                }
                
                addEnrolledCourse(title, description, instructor, totalVideos, completedVideos, progressPercent, courseId, course);
            }
            
            // Update statistics display
            updateStatistics(totalCourses, completedCoursesCount, totalProgressSum, totalVideosAll, completedVideosAll);
            
        } catch (Exception e) {
            Toast.makeText(this, "Error displaying courses", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void addEnrolledCourse(String title, String description, String instructor, 
                                  int totalVideos, int completedVideos, int progressPercent, 
                                  int courseId, JSONObject courseData) {
        
        View courseView = LayoutInflater.from(this).inflate(R.layout.item_enrolled_course, 
                                                           coursesContainer, false);
        
        TextView titleText = courseView.findViewById(R.id.course_title);
        TextView descriptionText = courseView.findViewById(R.id.course_description);
        TextView instructorText = courseView.findViewById(R.id.instructor_name);
        TextView progressText = courseView.findViewById(R.id.progress_text);
        ProgressBar progressBar = courseView.findViewById(R.id.progress_bar);
        TextView videosCompletedText = courseView.findViewById(R.id.videos_completed_text);
        
        titleText.setText(title);
        descriptionText.setText(description);
        instructorText.setText("Instructor: " + instructor);
        progressText.setText(progressPercent + "% Complete");
        progressBar.setProgress(progressPercent);
        videosCompletedText.setText(completedVideos + "/" + totalVideos + " videos completed");
        
        // Click to view course videos
        courseView.setOnClickListener(v -> {
            Intent intent = new Intent(this, CourseVideosListActivity.class);
            intent.putExtra("course_id", courseId);
            intent.putExtra("course_title", title);
            intent.putExtra("course_description", description);
            startActivity(intent);
        });
        
        coursesContainer.addView(courseView);
    }
    
    private void updateStatistics(int totalCourses, int completedCourses, int totalProgressSum, int totalVideos, int completedVideos) {
        // Update total courses
        tvTotalCourses.setText("Total Courses: " + totalCourses);
        
        // Update completed courses
        tvCompletedCourses.setText("Completed: " + completedCourses + "/" + totalCourses);
        
        // Calculate overall progress percentage
        int overallProgress = 0;
        if (totalCourses > 0) {
            overallProgress = totalProgressSum / totalCourses;
        }
        
        // Alternative: Calculate based on videos
        int videoProgress = 0;
        if (totalVideos > 0) {
            videoProgress = (completedVideos * 100) / totalVideos;
        }
        
        // Display progress with video details
        tvOverallProgress.setText("Overall Progress: " + overallProgress + "% (" + completedVideos + "/" + totalVideos + " videos)");
    }
}
