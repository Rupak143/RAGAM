package com.example.ragamfinal;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TeacherMyCoursesActivity extends AppCompatActivity {
    private static final String TAG = "TeacherMyCourses";
    
    private ListView coursesListView;
    private ImageView backButton, homeIcon;
    private TextView titleText, noCourses;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    
    private List<JSONObject> coursesList;
    private CourseAdapter courseAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_mycourses);
        
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
        loadMyCourses();
    }
    
    private void initViews() {
        coursesListView = findViewById(R.id.courses_list);
        backButton = findViewById(R.id.back_button);
        titleText = findViewById(R.id.page_title);
        noCourses = findViewById(R.id.no_courses);
        
        // Bottom navigation
        homeIcon = findViewById(R.id.ic_home);
        
        coursesList = new ArrayList<>();
        courseAdapter = new CourseAdapter();
        coursesListView.setAdapter(courseAdapter);
        
        coursesListView.setOnItemClickListener((parent, view, position, id) -> {
            try {
                JSONObject course = coursesList.get(position);
                Intent intent = new Intent(this, CourseDetailActivity.class);
                intent.putExtra("course_data", course.toString());
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening course", e);
            }
        });
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
    }
    
    private void loadMyCourses() {
        try {
            int teacherId = currentUser.optInt("user_id", currentUser.optInt("id", 0));
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
                                
                                for (int i = 0; i < coursesArray.length(); i++) {
                                    coursesList.add(coursesArray.getJSONObject(i));
                                }
                                
                                courseAdapter.notifyDataSetChanged();
                                
                                if (coursesList.isEmpty()) {
                                    coursesListView.setVisibility(View.GONE);
                                    noCourses.setVisibility(View.VISIBLE);
                                    noCourses.setText("No courses created yet. Create your first course!");
                                } else {
                                    coursesListView.setVisibility(View.VISIBLE);
                                    noCourses.setVisibility(View.GONE);
                                }
                            } else {
                                String message = jsonResponse.optString("message", "Failed to load courses");
                                Toast.makeText(TeacherMyCoursesActivity.this, message, Toast.LENGTH_SHORT).show();
                                coursesListView.setVisibility(View.GONE);
                                noCourses.setVisibility(View.VISIBLE);
                                noCourses.setText("Failed to load courses. Please try again.");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing courses response", e);
                            Toast.makeText(TeacherMyCoursesActivity.this, 
                                "Failed to load courses", Toast.LENGTH_SHORT).show();
                            coursesListView.setVisibility(View.GONE);
                            noCourses.setVisibility(View.VISIBLE);
                            noCourses.setText("Error loading courses. Please try again.");
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(TeacherMyCoursesActivity.this, error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading courses", e);
            Toast.makeText(this, "Error loading courses", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupBottomNavigation() {
        homeIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, TeacherMainContainerActivity.class);
            intent.putExtra("fragment", "home");
            startActivity(intent);
            finish();
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            // Course was deleted, reload the list
            loadMyCourses();
        }
    }
    
    private class CourseAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return coursesList.size();
        }
        
        @Override
        public Object getItem(int position) {
            return coursesList.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_teacher_course, parent, false);
            }
            
            try {
                JSONObject course = coursesList.get(position);
                
                TextView courseTitle = convertView.findViewById(R.id.course_title);
                TextView courseDescription = convertView.findViewById(R.id.course_description);
                TextView enrollmentCount = convertView.findViewById(R.id.enrollment_count);
                TextView coursePrice = convertView.findViewById(R.id.course_price);
                TextView publishStatus = convertView.findViewById(R.id.publish_status);
                Button viewCourseButton = convertView.findViewById(R.id.btn_view_course);
                
                // Support both old and current backend field names.
                String title = course.optString("title", course.optString("course_title", "Untitled Course"));
                String description = course.optString("description", course.optString("course_description", "No description"));
                int students = course.optInt("enrolled_count", course.optInt("enrollment_count", 0));

                courseTitle.setText(title);
                courseDescription.setText(description);
                enrollmentCount.setText(students + " Students");
                
                // These fields might not exist in our simple backend, so use defaults
                coursePrice.setText("FREE"); // Default since we don't have price field
                
                publishStatus.setText("Published"); // Default since we don't have status field
                publishStatus.setTextColor(ContextCompat.getColor(TeacherMyCoursesActivity.this, android.R.color.holo_green_dark));
                
                // Handle View Course button click
                viewCourseButton.setOnClickListener(v -> {
                    Intent intent = new Intent(TeacherMyCoursesActivity.this, TeacherViewCourseActivity.class);
                    intent.putExtra("course_data", course.toString());
                    startActivityForResult(intent, 100); // Request code for course deletion
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error binding course data", e);
            }
            
            return convertView;
        }
    }
}
