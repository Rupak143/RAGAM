package com.example.ragamfinal;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class CoursesActivity extends AppCompatActivity {
    
    private ImageView homeIcon, coursesIcon, profileIcon;
    private TextView pageTitle;
    private EditText searchInput;
    private ListView coursesList;
    private ApiHelper apiHelper;
    private String categoryName;
    private int categoryId;
    private List<JSONObject> coursesDataList = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courses);
        
        // Get category data from intent
        categoryId = getIntent().getIntExtra("category_id", 0);
        categoryName = getIntent().getStringExtra("category_name");
        
        // Initialize API helper first
        apiHelper = new ApiHelper(this);
        
        initViews();
        setupBottomNavigation();
        setupToggleButtons();
        loadCourses();
    }
    
    private void initViews() {
        homeIcon = findViewById(R.id.ic_home);
        coursesIcon = findViewById(R.id.ic_courses);
        profileIcon = findViewById(R.id.ic_profile);
        pageTitle = findViewById(R.id.page_title);
        searchInput = findViewById(R.id.search_input);
        coursesList = findViewById(R.id.courses_list);
        
        // Set page title based on category
        if (categoryName != null && !categoryName.isEmpty()) {
            pageTitle.setText(categoryName + " Courses");
        } else {
            pageTitle.setText("All Courses");
        }
    }
    
    private void setupBottomNavigation() {
        // Highlight courses icon (current page)
        highlightBottomNavIcon(coursesIcon);
        
        homeIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainContainerActivity.class);
            intent.putExtra("fragment", "home");
            startActivity(intent);
            finish();
        });
        
        coursesIcon.setOnClickListener(v -> {
            // Already on courses
            highlightBottomNavIcon(coursesIcon);
        });
        
        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });
    }
    
    private void setupToggleButtons() {
        // Hide toggle buttons since we only have courses now
        findViewById(R.id.toggle_buttons).setVisibility(View.GONE);
    }
    
    private void loadCourses() {
        // Show a loading message first
        List<Course> loadingCourses = new ArrayList<>();
        loadingCourses.add(new Course("Loading...", "Connecting to server", "", "SYSTEM", "⏳"));
        CourseAdapter adapter = new CourseAdapter(loadingCourses);
        coursesList.setAdapter(adapter);
        pageTitle.setText("Loading Courses...");
        
        // Now load actual courses
        loadActualCourses();
    }
    
    private void loadActualCourses() {
        // Load real courses from the backend
        Log.d("CoursesActivity", "Starting to load courses... Category ID: " + categoryId);
        
        // Use category filter if provided
        if (categoryId > 0) {
            apiHelper.getAllCourses(categoryId, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    handleCoursesResponse(response);
                }
                
                @Override
                public void onError(String error) {
                    handleCoursesError(error);
                }
            });
        } else {
            apiHelper.getAllCourses(new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    handleCoursesResponse(response);
                }
                
                @Override
                public void onError(String error) {
                    handleCoursesError(error);
                }
            });
        }
    }
    
    private void handleCoursesResponse(String response) {
        runOnUiThread(() -> {
            try {
                Log.d("CoursesActivity", "Courses response: " + response);
                JSONObject jsonResponse = new JSONObject(response);
                
                // Check for success status - handle both string "success" and boolean true
                boolean isSuccess = false;
                if (jsonResponse.has("status")) {
                    String status = jsonResponse.getString("status");
                    isSuccess = "success".equals(status);
                } else if (jsonResponse.has("success")) {
                    isSuccess = jsonResponse.getBoolean("success");
                }
                
                Log.d("CoursesActivity", "Is success: " + isSuccess);
                
                if (isSuccess && jsonResponse.has("data")) {
                    JSONArray coursesArray = jsonResponse.getJSONArray("data");
                    Log.d("CoursesActivity", "Found " + coursesArray.length() + " courses");
                    List<Course> courses = new ArrayList<>();
                    coursesDataList.clear();
                    
                    for (int i = 0; i < coursesArray.length(); i++) {
                        JSONObject courseJson = coursesArray.getJSONObject(i);
                        coursesDataList.add(courseJson);
                        
                        // Get course title - handle both 'course_title' and 'title' fields
                        String courseTitle = courseJson.optString("course_title", 
                                            courseJson.optString("title", "Untitled Course"));
                        
                        // Get teacher name
                        String teacherName = courseJson.optString("teacher_name", "Unknown Teacher");
                        
                        // Get price - check both course_price and price fields
                        int coursePrice = courseJson.optInt("course_price", 
                                        courseJson.optInt("price", 0));
                        String priceStr = coursePrice == 0 || courseJson.optInt("is_free", 0) == 1 
                                        ? "Free" : "₹" + coursePrice;
                        
                        // Get difficulty level
                        String difficultyLevel = courseJson.optString("difficulty_level", "beginner");
                        
                        // Get rating
                        String rating = courseJson.optString("rating_formatted", 
                                      String.format("%.1f★", courseJson.optDouble("rating", 4.5)));
                        
                        Course course = new Course(
                            courseTitle,
                            teacherName,
                            priceStr,
                            difficultyLevel,
                            rating
                        );
                        courses.add(course);
                    }
                    
                    if (courses.isEmpty()) {
                        showErrorMessage("No courses found in this category");
                    } else {
                        // Create and set adapter
                        CourseAdapter adapter = new CourseAdapter(courses);
                        coursesList.setAdapter(adapter);
                        
                        // Add click listener to open course details
                        coursesList.setOnItemClickListener((parent, view, position, id) -> {
                            JSONObject courseData = coursesDataList.get(position);
                            Intent intent = new Intent(CoursesActivity.this, CourseDetailActivity.class);
                            intent.putExtra("course_data", courseData.toString());
                            startActivity(intent);
                        });
                        
                        // Update page title
                        if (categoryName != null && !categoryName.isEmpty()) {
                            pageTitle.setText(categoryName + " (" + courses.size() + ")");
                        } else {
                            pageTitle.setText("Courses (" + courses.size() + ")");
                        }
                        Log.d("CoursesActivity", "Loaded " + courses.size() + " courses");
                    }
                    
                } else {
                    showErrorMessage("API returned error status");
                }
            } catch (Exception e) {
                Log.e("CoursesActivity", "Error parsing courses", e);
                showErrorMessage("Error parsing response: " + e.getMessage());
            }
        });
    }
    
    private void handleCoursesError(String error) {
        runOnUiThread(() -> {
            Log.e("CoursesActivity", "Error loading courses: " + error);
            showErrorMessage("Network error: " + error);
        });
    }
    
    private void showErrorMessage(String message) {
        List<Course> courses = new ArrayList<>();
        Course errorMessage = new Course(
            "Unable to Load Courses",
            message,
            "",
            "SYSTEM",
            "⚠ Error"
        );
        courses.add(errorMessage);
        
        CourseAdapter adapter = new CourseAdapter(courses);
        coursesList.setAdapter(adapter);
        pageTitle.setText("Courses - Error");
    }
    
    // Course data class
    private static class Course {
        String title, teacher, price, level, rating;
        
        Course(String title, String teacher, String price, String level, String rating) {
            this.title = title;
            this.teacher = teacher;
            this.price = price;
            this.level = level;
            this.rating = rating;
        }
    }
    
    // Course adapter for ListView
    private class CourseAdapter extends BaseAdapter {
        private List<Course> courses;
        
        CourseAdapter(List<Course> courses) {
            this.courses = courses;
        }
        
        @Override
        public int getCount() {
            return courses.size();
        }
        
        @Override
        public Object getItem(int position) {
            return courses.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_course, parent, false);
            }
            
            Course course = courses.get(position);
            
            TextView titleView = convertView.findViewById(R.id.course_title);
            TextView teacherView = convertView.findViewById(R.id.teacher_name);
            TextView priceView = convertView.findViewById(R.id.course_price);
            TextView levelView = convertView.findViewById(R.id.difficulty_level);
            
            titleView.setText(course.title);
            teacherView.setText(course.teacher);
            priceView.setText(course.price);
            levelView.setText(course.level);
            
            return convertView;
        }
    }
    
    private void highlightBottomNavIcon(ImageView selectedIcon) {
        // Reset all icons to default color
        homeIcon.setColorFilter(Color.GRAY);
        coursesIcon.setColorFilter(Color.GRAY);
        profileIcon.setColorFilter(Color.GRAY);
        
        // Highlight selected icon
        selectedIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Highlight courses icon when returning to this activity
        highlightBottomNavIcon(coursesIcon);
    }
}
