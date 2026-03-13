package com.example.ragamfinal;

import android.content.Intent;
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

public class MentorDetailActivity extends AppCompatActivity {
    private static final String TAG = "MentorDetail";
    
    private TextView mentorName, mentorSpecialization, mentorExperience, mentorBio;
    private ListView coursesListView;
    private ImageView backButton, verificationBadge;
    private Button messageButton;
    private ApiHelper apiHelper;
    private JSONObject mentorData;
    
    private List<JSONObject> coursesList;
    private MentorCourseAdapter courseAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mentor_detail);
        
        apiHelper = new ApiHelper(this);
        
        initViews();
        loadMentorData();
        setupClickListeners();
        loadMentorCourses();
    }
    
    private void initViews() {
        mentorName = findViewById(R.id.mentor_name);
        mentorSpecialization = findViewById(R.id.mentor_specialization);
        mentorExperience = findViewById(R.id.mentor_experience);
        mentorBio = findViewById(R.id.mentor_bio);
        coursesListView = findViewById(R.id.mentor_courses_list);
        backButton = findViewById(R.id.back_button);
        verificationBadge = findViewById(R.id.verification_badge);
        messageButton = findViewById(R.id.btn_message_mentor);
        
        coursesList = new ArrayList<>();
        courseAdapter = new MentorCourseAdapter();
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
    
    private void loadMentorData() {
        try {
            String mentorDataString = getIntent().getStringExtra("mentor_data");
            if (mentorDataString != null) {
                mentorData = new JSONObject(mentorDataString);
                
                mentorName.setText(mentorData.getString("full_name"));
                mentorSpecialization.setText(mentorData.getString("specialization"));
                mentorExperience.setText(mentorData.getInt("experience_years") + " years experience");
                
                String bio = mentorData.optString("bio", "No bio available");
                mentorBio.setText(bio);
                
                // Handle is_verified field (can be integer or boolean)
                boolean isVerified = false;
                try {
                    isVerified = mentorData.getBoolean("is_verified");
                } catch (Exception e) {
                    // If it's an integer, convert it
                    int verifiedInt = mentorData.optInt("is_verified", 0);
                    isVerified = (verifiedInt == 1);
                }
                verificationBadge.setVisibility(isVerified ? View.VISIBLE : View.GONE);
                
            } else {
                Toast.makeText(this, "Mentor data not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading mentor data", e);
            Toast.makeText(this, "Error loading mentor details", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        messageButton.setOnClickListener(v -> {
            try {
                // Open chat with this mentor
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("mentor_id", mentorData.getInt("user_id"));
                intent.putExtra("mentor_name", mentorData.getString("full_name"));
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error opening chat", e);
                Toast.makeText(this, "Error opening chat", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadMentorCourses() {
        try {
            int teacherId = mentorData.getInt("user_id");
            
            apiHelper.getTeacherCourses(teacherId, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            Log.d(TAG, "Mentor courses response: " + response);
                            JSONObject jsonResponse = new JSONObject(response);
                            
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
                                    JSONObject course = coursesArray.getJSONObject(i);
                                    // Only show published courses - handle both int and boolean
                                    boolean isPublished = false;
                                    try {
                                        isPublished = course.getBoolean("is_published");
                                    } catch (Exception e) {
                                        int publishedInt = course.optInt("is_published", 0);
                                        isPublished = (publishedInt == 1);
                                    }
                                    
                                    if (isPublished) {
                                        coursesList.add(course);
                                    }
                                }
                                
                                courseAdapter.notifyDataSetChanged();
                                
                                if (coursesList.isEmpty()) {
                                    Toast.makeText(MentorDetailActivity.this, 
                                        "This mentor hasn't published any courses yet", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                String message = jsonResponse.optString("message", "Failed to load courses");
                                Toast.makeText(MentorDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing courses response", e);
                            Toast.makeText(MentorDetailActivity.this, 
                                "Error loading mentor's courses", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(MentorDetailActivity.this, 
                            "Error loading courses: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading mentor courses", e);
            Toast.makeText(this, "Error loading courses", Toast.LENGTH_SHORT).show();
        }
    }
    
    private class MentorCourseAdapter extends BaseAdapter {
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
                convertView = getLayoutInflater().inflate(R.layout.item_mentor_course, parent, false);
            }
            
            try {
                JSONObject course = coursesList.get(position);
                
                TextView courseTitle = convertView.findViewById(R.id.course_title);
                TextView courseDescription = convertView.findViewById(R.id.course_description);
                TextView coursePrice = convertView.findViewById(R.id.course_price);
                TextView enrollmentCount = convertView.findViewById(R.id.enrollment_count);
                TextView courseDifficulty = convertView.findViewById(R.id.course_difficulty);
                
                courseTitle.setText(course.getString("course_title"));
                courseDescription.setText(course.getString("course_description"));
                
                double price = course.getDouble("course_price");
                if (price == 0) {
                    coursePrice.setText("FREE");
                    coursePrice.setTextColor(ContextCompat.getColor(MentorDetailActivity.this, android.R.color.holo_green_dark));
                } else {
                    coursePrice.setText("₹" + String.format("%.0f", price));
                    coursePrice.setTextColor(ContextCompat.getColor(MentorDetailActivity.this, android.R.color.holo_blue_dark));
                }
                
                enrollmentCount.setText(course.getInt("enrollment_count") + " students enrolled");
                courseDifficulty.setText(course.getString("difficulty_level").toUpperCase());
                
            } catch (Exception e) {
                Log.e(TAG, "Error binding course data", e);
            }
            
            return convertView;
        }
    }
}
