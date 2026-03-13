package com.example.ragamfinal;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TeacherViewStudentsByCoursesActivity extends AppCompatActivity {
    private static final String TAG = "TeacherViewStudentsByCourses";
    
    private ImageView backButton, homeIcon;
    private TextView titleText;
    private LinearLayout courseContainer;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    private List<JSONObject> coursesList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_viewstudentsbycourses);
        
        apiHelper = new ApiHelper(this);
        currentUser = apiHelper.getUserSession();
        
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupClickListeners();
        setupBottomNavigation();
        loadTeacherCourses();
    }
    
    private void initViews() {
        backButton = findViewById(R.id.back_button);
        titleText = findViewById(R.id.page_title);
        courseContainer = findViewById(R.id.course_container);
        
        // Bottom navigation
        homeIcon = findViewById(R.id.ic_home);
        
        coursesList = new ArrayList<>();
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
    }
    
    private void loadTeacherCourses() {
        try {
            int teacherId = resolveTeacherId();
            if (teacherId == 0) {
                throw new Exception("Invalid teacher session");
            }
            
            apiHelper.getTeacherCourses(teacherId, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            Log.d(TAG, "Raw response: " + response);
                            JSONObject jsonResponse = new JSONObject(response);
                            
                            // Handle both "status" and "success" fields for compatibility
                            boolean isSuccess = false;
                            if (jsonResponse.has("status")) {
                                isSuccess = "success".equals(jsonResponse.getString("status"));
                            } else if (jsonResponse.has("success")) {
                                isSuccess = jsonResponse.getBoolean("success");
                            }
                            
                            if (isSuccess) {
                                JSONArray coursesArray = jsonResponse.getJSONArray("data");
                                coursesList.clear();
                                courseContainer.removeAllViews();
                                
                                for (int i = 0; i < coursesArray.length(); i++) {
                                    JSONObject course = coursesArray.getJSONObject(i);
                                    coursesList.add(course);
                                    createCourseCard(course);
                                }
                                
                                if (coursesList.isEmpty()) {
                                    showNoCourses();
                                }
                            } else {
                                String message = jsonResponse.optString("message", "Failed to load courses");
                                Toast.makeText(TeacherViewStudentsByCoursesActivity.this, message, Toast.LENGTH_SHORT).show();
                                showNoCourses();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing courses response", e);
                            Toast.makeText(TeacherViewStudentsByCoursesActivity.this, 
                                "Failed to load courses", Toast.LENGTH_SHORT).show();
                            showNoCourses();
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "Error loading courses: " + error);
                        Toast.makeText(TeacherViewStudentsByCoursesActivity.this, 
                            "Error loading courses", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading courses", e);
            Toast.makeText(this, "Error loading courses", Toast.LENGTH_SHORT).show();
        }
    }

    private int resolveTeacherId() {
        if (currentUser == null) {
            return 0;
        }
        return currentUser.optInt("user_id", currentUser.optInt("id", 0));
    }
    
    private void createCourseCard(JSONObject course) {
        try {
            LayoutInflater inflater = LayoutInflater.from(this);
            View courseCard = inflater.inflate(R.layout.item_course_card, courseContainer, false);
            
            TextView courseTitle = courseCard.findViewById(R.id.course_title);
            TextView courseDescription = courseCard.findViewById(R.id.course_description);
            Button viewStudentsButton = courseCard.findViewById(R.id.btn_action);
            
            String title = course.optString("course_title", course.optString("title", "Untitled Course"));
            String description = course.optString("course_description", course.optString("description", "No description"));

            courseTitle.setText(title);
            courseDescription.setText(description);
            viewStudentsButton.setText("View Students");
            
            viewStudentsButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, TeacherViewStudentsActivity.class);
                intent.putExtra("course_id", course.optInt("course_id"));
                intent.putExtra("course_title", title);
                startActivity(intent);
            });
            
            courseContainer.addView(courseCard);
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating course card", e);
        }
    }
    
    private void showNoCourses() {
        TextView noCourses = new TextView(this);
        noCourses.setText("No courses found. Create your first course!");
        noCourses.setTextSize(16);
        noCourses.setTextColor(Color.GRAY);
        noCourses.setGravity(android.view.Gravity.CENTER);
        noCourses.setPadding(16, 40, 16, 16);
        courseContainer.addView(noCourses);
    }
    
    private void setupBottomNavigation() {
        homeIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, TeacherMainContainerActivity.class);
            intent.putExtra("fragment", "home");
            startActivity(intent);
            finish();
        });
    }
}
