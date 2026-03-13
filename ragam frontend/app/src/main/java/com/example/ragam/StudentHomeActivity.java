package com.example.ragamfinal;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

public class StudentHomeActivity extends AppCompatActivity {
    
    private ImageView homeIcon, coursesIcon, profileIcon;
    private TextView greetingText;
    private Button enrolledCoursesButton, completedCoursesButton;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);
        
        initViews();
        setupBottomNavigation();
        loadUserData();
        
        apiHelper = new ApiHelper(this);
    }
    
    private void initViews() {
        homeIcon = findViewById(R.id.ic_home);
        coursesIcon = findViewById(R.id.ic_courses);
        profileIcon = findViewById(R.id.ic_profile);
        greetingText = findViewById(R.id.greeting_text);
        enrolledCoursesButton = findViewById(R.id.enrolled_courses_button);
        completedCoursesButton = findViewById(R.id.completed_courses_button);
        
        // Setup enrolled courses button click listener
        enrolledCoursesButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentEnrolledCoursesActivity.class);
            startActivity(intent);
        });
        
        // Setup completed courses button click listener
        completedCoursesButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentCompletedCoursesActivity.class);
            startActivity(intent);
        });
    }
    
    private void setupBottomNavigation() {
        // Highlight home icon (current page)
        highlightBottomNavIcon(homeIcon);
        
        homeIcon.setOnClickListener(v -> {
            // Already on home
            highlightBottomNavIcon(homeIcon);
        });
        
        coursesIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, CoursesActivity.class);
            startActivity(intent);
        });
        
        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });
        
        // Handle category clicks
        setupCategoryClicks();
    }
    
    private void setupCategoryClicks() {
        // Find all category LinearLayouts and add click listeners
        LinearLayout vocalTrainingCategory = findCategoryByPosition(0);
        LinearLayout instrumentalCategory = findCategoryByPosition(1);
        LinearLayout bhaktiDevotionalCategory = findCategoryByPosition(2);
        LinearLayout kidsSpecialCategory = findCategoryByPosition(3);
        
        if (vocalTrainingCategory != null) {
            vocalTrainingCategory.setOnClickListener(v -> openCoursesByCategory(1, "Vocal Training"));
        }
        if (instrumentalCategory != null) {
            instrumentalCategory.setOnClickListener(v -> openCoursesByCategory(2, "Instrumental Music"));
        }
        if (bhaktiDevotionalCategory != null) {
            // FIXED: Changed ID from 3 to 6, and name from "Bhakti/Devotional Music" to "Devotional Music"
            bhaktiDevotionalCategory.setOnClickListener(v -> openCoursesByCategory(6, "Devotional Music"));
        }
        if (kidsSpecialCategory != null) {
            // FIXED: Changed ID from 4 to 8, and name from "Kid's Special Music Training" to "Kids Special"
            kidsSpecialCategory.setOnClickListener(v -> openCoursesByCategory(8, "Kids Special"));
        }
    }
    
    private LinearLayout findCategoryByPosition(int position) {
        GridLayout categoriesGrid = findViewById(R.id.categories_grid);
        if (categoriesGrid != null && position < categoriesGrid.getChildCount()) {
            return (LinearLayout) categoriesGrid.getChildAt(position);
        }
        return null;
    }
    
    private void openCoursesByCategory(int categoryId, String categoryName) {
        Intent intent = new Intent(this, CoursesActivity.class);
        intent.putExtra("category_id", categoryId);
        intent.putExtra("category_name", categoryName);
        startActivity(intent);
    }
    
    private void highlightBottomNavIcon(ImageView selectedIcon) {
        // Reset all icons to default color
        homeIcon.setColorFilter(Color.GRAY);
        coursesIcon.setColorFilter(Color.GRAY);
        profileIcon.setColorFilter(Color.GRAY);
        
        // Highlight selected icon
        selectedIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
    }
    
    private void loadUserData() {
        apiHelper = new ApiHelper(this);
        currentUser = apiHelper.getUserSession();
        
        if (currentUser != null) {
            try {
                String fullName = currentUser.getString("full_name");
                String firstName = fullName.split(" ")[0];
                greetingText.setText("Hey " + firstName + "!!");
            } catch (Exception e) {
                greetingText.setText("Hey there!!");
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Highlight home icon when returning to this activity
        highlightBottomNavIcon(homeIcon);
    }
}
