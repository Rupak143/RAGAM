package com.example.ragamfinal.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ragamfinal.R;
import com.example.ragamfinal.TeacherCourseSetupActivity;
import com.example.ragamfinal.TeacherMyCoursesActivity;
import com.example.ragamfinal.TeacherViewStudentsByCoursesActivity;
import com.example.ragamfinal.UserOptionActivity;
import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

public class TeacherHomeFragment extends Fragment {
    
    private TextView greetingText;
    private Button createCourseButton, myCoursesButton, viewStudentsButton;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_home, container, false);
        
        apiHelper = new ApiHelper(requireContext());
        
        initViews(view);
        loadUserData();
        setupClickListeners();
        
        return view;
    }
    
    private void initViews(View view) {
        greetingText = view.findViewById(R.id.greeting);
        createCourseButton = view.findViewById(R.id.btn_create_course);
        myCoursesButton = view.findViewById(R.id.btn_my_courses);
        viewStudentsButton = view.findViewById(R.id.btn_view_students);
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
            Intent intent = new Intent(requireContext(), UserOptionActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        }
    }
    
    private void setupClickListeners() {
        createCourseButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TeacherCourseSetupActivity.class);
            
            // Pass instructor ID
            if (currentUser != null) {
                try {
                    String instructorId = currentUser.optString("user_id", currentUser.optString("id", ""));
                    if (instructorId.isEmpty()) {
                        throw new Exception("Missing instructor id");
                    }
                    intent.putExtra("instructor_id", instructorId);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Error: Unable to get instructor ID", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                Toast.makeText(requireContext(), "Error: No user session found", Toast.LENGTH_SHORT).show();
                return;
            }
            
            startActivity(intent);
        });
        
        myCoursesButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TeacherMyCoursesActivity.class);
            startActivity(intent);
        });
        
        viewStudentsButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TeacherViewStudentsByCoursesActivity.class);
            startActivity(intent);
        });
    }
}
