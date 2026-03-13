package com.example.ragamfinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONArray;
import org.json.JSONObject;

public class CourseDetailActivity extends AppCompatActivity {
    
    private TextView courseTitle, courseDescription, teacherName, teacherBio, readMoreText, courseStatusText;
    private Button enrollButton, playCourseButton;
    private ImageView backButton, playIcon;
    private ApiHelper apiHelper;
    private JSONObject courseData, currentUser;
    private int courseId;
    private boolean isEnrolled = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course);
        
        initViews();
        loadCourseData();
        setupClickListeners();
        
        apiHelper = new ApiHelper(this);
        currentUser = apiHelper.getUserSession();
        
        // Check enrollment status
        checkEnrollmentStatus();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Re-check enrollment status when user returns to this activity
        checkEnrollmentStatus();
    }
    
    private void initViews() {
        courseTitle = findViewById(R.id.course_title);
        courseDescription = findViewById(R.id.course_description);
        readMoreText = findViewById(R.id.read_more);
        enrollButton = findViewById(R.id.enroll_button);
        playCourseButton = findViewById(R.id.play_course_button);
        backButton = findViewById(R.id.back_button);
        playIcon = findViewById(R.id.play_icon);
        courseStatusText = findViewById(R.id.course_status_text);
        
        // Find teacher info TextViews - they might be in the instructor section
        try {
            teacherName = findViewById(R.id.teacher_name);
            teacherBio = findViewById(R.id.teacher_bio);
        } catch (Exception e) {
            Log.e("CourseDetail", "Teacher views not found", e);
        }
    }
    
    private void loadCourseData() {
        Intent intent = getIntent();
        String courseDataString = intent.getStringExtra("course_data");
        
        if (courseDataString != null) {
            try {
                courseData = new JSONObject(courseDataString);
                courseId = courseData.getInt("course_id");
                
                // Populate UI with course data
                courseTitle.setText(courseData.optString("course_title", courseData.optString("title", "Course")));
                courseDescription.setText(courseData.optString("course_description", courseData.optString("description", "No description available")));
                
                // Update teacher information if views exist
                if (teacherName != null) {
                    teacherName.setText(courseData.optString("teacher_name", "Unknown Teacher"));
                }
                
                // Update enroll button text based on price
                double price = courseData.optDouble("course_price", 0);
                if (price == 0) {
                    enrollButton.setText("ENROLL FOR FREE");
                } else {
                    enrollButton.setText("ENROLL - ₹" + String.format("%.0f", price));
                }
                
            } catch (Exception e) {
                Log.e("CourseDetail", "Error parsing course data", e);
                Toast.makeText(this, "Error loading course details", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Course data not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        enrollButton.setOnClickListener(v -> handleEnrollment());
        
        playCourseButton.setOnClickListener(v -> openCourseContent());
        
        playIcon.setOnClickListener(v -> {
            if (isEnrolled) {
                openCourseContent();
            } else {
                Toast.makeText(this, "Please enroll first to access course content", Toast.LENGTH_SHORT).show();
            }
        });
        
        readMoreText.setOnClickListener(v -> {
            // Toggle description expansion
            if (courseDescription.getMaxLines() == Integer.MAX_VALUE) {
                courseDescription.setMaxLines(3);
                readMoreText.setText("→ Read More");
            } else {
                courseDescription.setMaxLines(Integer.MAX_VALUE);
                readMoreText.setText("← Read Less");
            }
        });
    }
    
    private void handleEnrollment() {
        if (currentUser == null) {
            Toast.makeText(this, "Please login to enroll", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int studentId = currentUser.getInt("user_id");
            
            // Disable button during enrollment
            enrollButton.setEnabled(false);
            enrollButton.setText("Enrolling...");
            
            apiHelper.enrollCourse(studentId, courseId, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            Log.d("CourseDetail", "Enrollment response: " + response);
                            JSONObject jsonResponse = new JSONObject(response);
                            
                            // Handle both "status" and "success" fields for compatibility
                            boolean isSuccess = false;
                            if (jsonResponse.has("status")) {
                                isSuccess = "success".equals(jsonResponse.getString("status"));
                            } else if (jsonResponse.has("success")) {
                                isSuccess = jsonResponse.getBoolean("success");
                            }
                            
                            if (isSuccess) {
                                Toast.makeText(CourseDetailActivity.this, "Enrollment successful!", Toast.LENGTH_SHORT).show();
                                
                                // Update UI to show enrolled status
                                updateUIForEnrollmentStatus(true);
                                // Don't re-enable or show the enroll button - it's now hidden
                                
                            } else {
                                String message = jsonResponse.optString("message", "Enrollment failed");
                                Toast.makeText(CourseDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                                
                                // Reset enroll button
                                enrollButton.setEnabled(true);
                                try {
                                    double price = courseData.getDouble("course_price");
                                    if (price == 0) {
                                        enrollButton.setText("ENROLL FOR FREE");
                                    } else {
                                        enrollButton.setText("ENROLL - ₹" + String.format("%.0f", price));
                                    }
                                } catch (Exception e) {
                                    enrollButton.setText("ENROLL COURSE");
                                }
                            }
                        } catch (Exception e) {
                            Log.e("CourseDetail", "Error parsing enrollment response", e);
                            Toast.makeText(CourseDetailActivity.this, "Enrollment failed", Toast.LENGTH_SHORT).show();
                            enrollButton.setEnabled(true);
                            try {
                                double price = courseData.getDouble("course_price");
                                if (price == 0) {
                                    enrollButton.setText("ENROLL FOR FREE");
                                } else {
                                    enrollButton.setText("ENROLL - ₹" + String.format("%.0f", price));
                                }
                            } catch (Exception e2) {
                                enrollButton.setText("ENROLL COURSE");
                            }
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(CourseDetailActivity.this, error, Toast.LENGTH_SHORT).show();
                        enrollButton.setEnabled(true);
                        try {
                            double price = courseData.getDouble("course_price");
                            if (price == 0) {
                                enrollButton.setText("ENROLL FOR FREE");
                            } else {
                                enrollButton.setText("ENROLL - ₹" + String.format("%.0f", price));
                            }
                        } catch (Exception e) {
                            enrollButton.setText("ENROLL COURSE");
                        }
                    });
                }
            });
            
        } catch (Exception e) {
            Log.e("CourseDetail", "Error during enrollment", e);
            Toast.makeText(this, "Error during enrollment", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openCourseContent() {
        Intent intent = new Intent(this, CourseVideosListActivity.class);
        intent.putExtra("course_id", courseId);
        try {
            intent.putExtra("course_title", courseData.optString("course_title", courseData.optString("title", "Course")));
            intent.putExtra("course_description", courseData.optString("course_description", courseData.optString("description", "")));
        } catch (Exception e) {
            intent.putExtra("course_title", "Course");
            intent.putExtra("course_description", "");
        }
        startActivity(intent);
    }
    
    private void checkEnrollmentStatus() {
        if (currentUser == null) {
            Log.d("CourseDetail", "No user session - showing enroll button");
            updateUIForEnrollmentStatus(false);
            return;
        }
        
        try {
            int studentId = currentUser.getInt("user_id");
            Log.d("CourseDetail", "Checking enrollment for student: " + studentId + ", course: " + courseId);
            
            // Check if student is enrolled by getting enrolled courses
            apiHelper.getEnrolledCourses(studentId, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            Log.d("CourseDetail", "Enrollment check response: " + response);
                            JSONObject jsonResponse = new JSONObject(response);
                            
                            boolean isSuccess = false;
                            if (jsonResponse.has("status")) {
                                isSuccess = "success".equals(jsonResponse.getString("status"));
                            } else if (jsonResponse.has("success")) {
                                isSuccess = jsonResponse.getBoolean("success");
                            }
                            
                            if (isSuccess) {
                                JSONArray enrolledCourses = jsonResponse.getJSONArray("data");
                                boolean enrolled = false;
                                
                                Log.d("CourseDetail", "Found " + enrolledCourses.length() + " enrolled courses");
                                
                                // Check if current course is in enrolled courses
                                for (int i = 0; i < enrolledCourses.length(); i++) {
                                    JSONObject course = enrolledCourses.getJSONObject(i);
                                    int enrolledCourseId = course.getInt("course_id");
                                    Log.d("CourseDetail", "Checking enrolled course ID: " + enrolledCourseId + " vs current: " + courseId);
                                    if (enrolledCourseId == courseId) {
                                        enrolled = true;
                                        Log.d("CourseDetail", "Student IS enrolled in this course!");
                                        break;
                                    }
                                }
                                
                                if (!enrolled) {
                                    Log.d("CourseDetail", "Student NOT enrolled in this course");
                                }
                                
                                updateUIForEnrollmentStatus(enrolled);
                            } else {
                                Log.d("CourseDetail", "API returned failure status");
                                updateUIForEnrollmentStatus(false);
                            }
                        } catch (Exception e) {
                            Log.e("CourseDetail", "Error checking enrollment", e);
                            updateUIForEnrollmentStatus(false);
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e("CourseDetail", "Error checking enrollment: " + error);
                        updateUIForEnrollmentStatus(false);
                    });
                }
            });
        } catch (Exception e) {
            Log.e("CourseDetail", "Error getting user data", e);
            updateUIForEnrollmentStatus(false);
        }
    }
    
    private void updateUIForEnrollmentStatus(boolean enrolled) {
        isEnrolled = enrolled;
        
        Log.d("CourseDetail", "Updating UI - enrolled: " + enrolled);
        
        if (enrolled) {
            // Student is enrolled - show play button and hide enroll button
            Log.d("CourseDetail", "Showing PLAY button, hiding ENROLL button");
            playCourseButton.setVisibility(View.VISIBLE);
            enrollButton.setVisibility(View.GONE);
            courseStatusText.setText("Ready to play • Tap to start learning");
            courseStatusText.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            // Student not enrolled - show enroll button and hide play button
            Log.d("CourseDetail", "Showing ENROLL button, hiding PLAY button");
            playCourseButton.setVisibility(View.GONE);
            enrollButton.setVisibility(View.VISIBLE);
            courseStatusText.setText("Enroll to access course content");
            courseStatusText.setTextColor(getResources().getColor(android.R.color.white));
        }
    }
}
