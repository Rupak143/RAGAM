package com.example.ragamfinal;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

public class TeacherHomeActivity extends AppCompatActivity {
    
    private TextView greetingText;
    private Button createCourseButton, myCoursesButton, viewStudentsButton;
    private ImageView homeIcon, profileIcon;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_home);
        
        // Initialize ApiHelper first
        apiHelper = new ApiHelper(this);
        
        initViews();
        loadUserData();
        setupClickListeners();
        setupBottomNavigation();
    }
    
    private void initViews() {
        greetingText = findViewById(R.id.greeting);
        createCourseButton = findViewById(R.id.btn_create_course);
        myCoursesButton = findViewById(R.id.btn_my_courses);
        viewStudentsButton = findViewById(R.id.btn_view_students);
        
        // Bottom navigation
        homeIcon = findViewById(R.id.ic_home);
        profileIcon = findViewById(R.id.ic_profile);
    }
    
    private void loadUserData() {
        currentUser = apiHelper.getUserSession();
        
        if (currentUser != null) {
            try {
                String fullName = currentUser.getString("full_name");
                String firstName = fullName.split(" ")[0];
                greetingText.setText("Hey " + firstName + "!");
            } catch (Exception e) {
                greetingText.setText("Hey Teacher!");
            }
        } else {
            // Redirect to login if no session
            Intent intent = new Intent(this, UserOptionActivity.class);
            startActivity(intent);
            finish();
        }
    }
    
    private void setupClickListeners() {
        createCourseButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, TeacherCourseSetupActivity.class);
            
            // Pass instructor ID
            if (currentUser != null) {
                try {
                    String instructorId = currentUser.optString("user_id", currentUser.optString("id", ""));
                    if (instructorId.isEmpty()) {
                        throw new Exception("Missing instructor id");
                    }
                    intent.putExtra("instructor_id", instructorId);
                } catch (Exception e) {
                    Toast.makeText(this, "Error: Unable to get instructor ID", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                Toast.makeText(this, "Error: No user session found", Toast.LENGTH_SHORT).show();
                return;
            }
            
            startActivity(intent);
        });
        
        myCoursesButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, TeacherMyCoursesActivity.class);
            startActivity(intent);
        });
        
        viewStudentsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, TeacherViewStudentsByCoursesActivity.class);
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
        
        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, TeacherMainContainerActivity.class);
            intent.putExtra("fragment", "profile");
            startActivity(intent);
        });
    }
    
    private void highlightBottomNavIcon(ImageView selectedIcon) {
        // Reset all icons to default color
        homeIcon.setColorFilter(Color.GRAY);
        profileIcon.setColorFilter(Color.GRAY);
        
        // Highlight selected icon
        selectedIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Highlight home icon when returning to this activity
        highlightBottomNavIcon(homeIcon);
    }
}
