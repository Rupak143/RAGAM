package com.example.ragamfinal.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ragamfinal.CoursesActivity;
import com.example.ragamfinal.R;
import com.example.ragamfinal.StudentCompletedCoursesActivity;
import com.example.ragamfinal.StudentEnrolledCoursesActivity;
import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

public class StudentHomeFragment extends Fragment {
    
    private TextView greetingText;
    private Button enrolledCoursesButton, completedCoursesButton;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_home, container, false);
        
        initViews(view);
        loadUserData();
        setupCategoryClicks(view);
        
        return view;
    }
    
    private void initViews(View view) {
        greetingText = view.findViewById(R.id.greeting_text);
        enrolledCoursesButton = view.findViewById(R.id.enrolled_courses_button);
        completedCoursesButton = view.findViewById(R.id.completed_courses_button);
        
        apiHelper = new ApiHelper(requireContext());
        
        // Setup enrolled courses button click listener
        enrolledCoursesButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), StudentEnrolledCoursesActivity.class);
            startActivity(intent);
        });
        
        // Setup completed courses button click listener
        completedCoursesButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), StudentCompletedCoursesActivity.class);
            startActivity(intent);
        });
    }
    
    private void setupCategoryClicks(View view) {
        // Find all category LinearLayouts and add click listeners
        LinearLayout vocalTrainingCategory = findCategoryByPosition(view, 0);
        LinearLayout instrumentalCategory = findCategoryByPosition(view, 1);
        LinearLayout bhaktiDevotionalCategory = findCategoryByPosition(view, 2);
        LinearLayout kidsSpecialCategory = findCategoryByPosition(view, 3);
        
        if (vocalTrainingCategory != null) {
            vocalTrainingCategory.setOnClickListener(v -> openCoursesByCategory(1, "Vocal Training"));
        }
        if (instrumentalCategory != null) {
            instrumentalCategory.setOnClickListener(v -> openCoursesByCategory(2, "Instrumental Music"));
        }
        if (bhaktiDevotionalCategory != null) {
            bhaktiDevotionalCategory.setOnClickListener(v -> openCoursesByCategory(6, "Devotional Music"));
        }
        if (kidsSpecialCategory != null) {
            kidsSpecialCategory.setOnClickListener(v -> openCoursesByCategory(8, "Kids Special"));
        }
    }
    
    private LinearLayout findCategoryByPosition(View view, int position) {
        GridLayout categoriesGrid = view.findViewById(R.id.categories_grid);
        if (categoriesGrid != null && position < categoriesGrid.getChildCount()) {
            return (LinearLayout) categoriesGrid.getChildAt(position);
        }
        return null;
    }
    
    private void openCoursesByCategory(int categoryId, String categoryName) {
        Intent intent = new Intent(requireContext(), CoursesActivity.class);
        intent.putExtra("category_id", categoryId);
        intent.putExtra("category_name", categoryName);
        startActivity(intent);
    }
    
    private void loadUserData() {
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
}
