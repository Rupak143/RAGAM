package com.example.ragamfinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TeacherViewStudentsActivity extends AppCompatActivity {
    private static final String TAG = "TeacherViewStudents";
    
    private ListView studentsListView;
    private ImageView backButton, homeIcon;
    private TextView titleText, noStudents;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    private int courseId;
    private String courseTitle;
    
    private List<JSONObject> studentsList;
    private StudentAdapter studentAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_viewstudents);
        
        apiHelper = new ApiHelper(this);
        currentUser = apiHelper.getUserSession();
        
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Get course data from intent
        Intent intent = getIntent();
        courseId = intent.getIntExtra("course_id", 0);
        courseTitle = intent.getStringExtra("course_title");
        
        if (courseId == 0) {
            Toast.makeText(this, "Invalid course data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupClickListeners();
        setupBottomNavigation();
        loadStudents();
    }
    
    private void initViews() {
        studentsListView = findViewById(R.id.students_list);
        backButton = findViewById(R.id.back_button);
        titleText = findViewById(R.id.page_title);
        noStudents = findViewById(R.id.no_students);
        
        // Set title with course name
        if (courseTitle != null) {
            titleText.setText("Students in: " + courseTitle);
        } else {
            titleText.setText("Enrolled Students");
        }
        
        // Bottom navigation
        homeIcon = findViewById(R.id.ic_home);
        
        studentsList = new ArrayList<>();
        studentAdapter = new StudentAdapter();
        studentsListView.setAdapter(studentAdapter);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
    }
    
    private void loadStudents() {
        try {
            Log.d(TAG, "Loading students for course ID: " + courseId);
            // Load students for specific course
            apiHelper.getCourseStudents(courseId, new ApiHelper.ApiCallback() {
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
                                JSONArray studentsArray = jsonResponse.getJSONArray("data");
                                Log.d(TAG, "Number of students found: " + studentsArray.length());
                                studentsList.clear();
                                
                                for (int i = 0; i < studentsArray.length(); i++) {
                                    JSONObject student = studentsArray.getJSONObject(i);
                                    Log.d(TAG, "Student " + i + ": " + student.toString());
                                    studentsList.add(student);
                                }
                                
                                studentAdapter.notifyDataSetChanged();
                                
                                if (studentsList.isEmpty()) {
                                    Log.d(TAG, "No students found - showing empty state");
                                    studentsListView.setVisibility(View.GONE);
                                    noStudents.setVisibility(View.VISIBLE);
                                    noStudents.setText("No students enrolled in this course yet.");
                                } else {
                                    Log.d(TAG, "Showing " + studentsList.size() + " students");
                                    studentsListView.setVisibility(View.VISIBLE);
                                    noStudents.setVisibility(View.GONE);
                                }
                            } else {
                                String message = jsonResponse.optString("message", "Failed to load students");
                                Log.e(TAG, "API returned failure: " + message);
                                Toast.makeText(TeacherViewStudentsActivity.this, message, Toast.LENGTH_SHORT).show();
                                studentsListView.setVisibility(View.GONE);
                                noStudents.setVisibility(View.VISIBLE);
                                noStudents.setText("No students enrolled in this course yet.");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing students response", e);
                            Toast.makeText(TeacherViewStudentsActivity.this, 
                                "Failed to load students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            studentsListView.setVisibility(View.GONE);
                            noStudents.setVisibility(View.VISIBLE);
                            noStudents.setText("Error loading students. Please try again.");
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Log.e(TAG, "API Error: " + error);
                        Toast.makeText(TeacherViewStudentsActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                        studentsListView.setVisibility(View.GONE);
                        noStudents.setVisibility(View.VISIBLE);
                        noStudents.setText("Failed to load students. Please try again.");
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading students", e);
            Toast.makeText(this, "Error loading students", Toast.LENGTH_SHORT).show();
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
    
    private class StudentAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return studentsList.size();
        }
        
        @Override
        public Object getItem(int position) {
            return studentsList.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_student, parent, false);
            }
            
            try {
                JSONObject student = studentsList.get(position);
                Log.d(TAG, "Binding student at position " + position + ": " + student.toString());
                
                TextView studentName = convertView.findViewById(R.id.student_name);
                TextView studentEmail = convertView.findViewById(R.id.student_email);
                TextView courseName = convertView.findViewById(R.id.course_name);
                TextView enrollmentDate = convertView.findViewById(R.id.enrollment_date);
                
                // Set student name
                String name = student.optString("full_name", "Unknown Student");
                studentName.setText(name);
                Log.d(TAG, "Set name: " + name);
                
                // Set student email
                String email = student.optString("email", "No email");
                studentEmail.setText(email);
                Log.d(TAG, "Set email: " + email);
                
                // Set course name - hide it since we're viewing students for one specific course
                courseName.setVisibility(View.GONE);
                
                // Set enrollment date
                String date = student.optString("enrollment_date", "Unknown date");
                enrollmentDate.setText("Enrolled: " + date);
                Log.d(TAG, "Set enrollment date: " + date);
                
            } catch (Exception e) {
                Log.e(TAG, "Error binding student data at position " + position, e);
            }
            
            return convertView;
        }
    }
}
