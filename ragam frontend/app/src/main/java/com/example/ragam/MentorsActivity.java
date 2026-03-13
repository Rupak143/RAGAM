package com.example.ragamfinal;

import android.content.Context;
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

public class MentorsActivity extends AppCompatActivity {
    
    private ListView mentorsListView;
    private ImageView homeIcon, coursesIcon, profileIcon;
    private TextView titleText;
    private Button btnCourses, btnMentors;
    private ApiHelper apiHelper;
    private MentorAdapter mentorAdapter;
    private List<JSONObject> mentorsList;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mentors);
        
        apiHelper = new ApiHelper(this);
        initViews();
        setupToggleButtons();
        setupBottomNavigation();
        loadMentors();
    }
    
    private void initViews() {
        mentorsListView = findViewById(R.id.mentors_list);
        homeIcon = findViewById(R.id.ic_home);
        coursesIcon = findViewById(R.id.ic_courses);
        profileIcon = findViewById(R.id.ic_profile);
        titleText = findViewById(R.id.page_title);
        btnCourses = findViewById(R.id.btn_courses);
        btnMentors = findViewById(R.id.btn_mentors);
        
        mentorsList = new ArrayList<>();
        mentorAdapter = new MentorAdapter(this, mentorsList);
        mentorsListView.setAdapter(mentorAdapter);
        
        // Handle mentor item clicks
        mentorsListView.setOnItemClickListener((parent, view, position, id) -> {
            try {
                JSONObject mentor = mentorsList.get(position);
                // Open mentor details with their courses
                Intent intent = new Intent(this, MentorDetailActivity.class);
                intent.putExtra("mentor_data", mentor.toString());
                startActivity(intent);
            } catch (Exception e) {
                Log.e("MentorsActivity", "Error opening mentor", e);
            }
        });
    }
    
    private void setupToggleButtons() {
        // Set initial state - Mentors button is selected
        updateToggleButtonState(false);
        
        btnCourses.setOnClickListener(v -> {
            updateToggleButtonState(true);
            // Navigate to CoursesActivity
            Intent intent = new Intent(this, CoursesActivity.class);
            startActivity(intent);
        });
        
        btnMentors.setOnClickListener(v -> {
            updateToggleButtonState(false);
            titleText.setText("All Mentors");
            // Already showing mentors, no need to reload
        });
    }
    
    private void updateToggleButtonState(boolean isCoursesSelected) {
        if (isCoursesSelected) {
            btnCourses.setBackgroundResource(R.drawable.rounded_button_orange);
            btnCourses.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            btnMentors.setBackgroundResource(R.drawable.rounded_button_white);
            btnMentors.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        } else {
            btnCourses.setBackgroundResource(R.drawable.rounded_button_white);
            btnCourses.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            btnMentors.setBackgroundResource(R.drawable.rounded_button_orange);
            btnMentors.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }
    
    private void setupBottomNavigation() {
        // Setup navigation for all users
        homeIcon.setOnClickListener(v -> {
            navigateToHome();
        });
        
        coursesIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, CoursesActivity.class);
            startActivity(intent);
        });
        
        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });
    }
    
    private void navigateToHome() {
        ApiHelper tempApiHelper = new ApiHelper(this);
        JSONObject currentUser = tempApiHelper.getUserSession();
        if (currentUser != null) {
            try {
                String userType = currentUser.getString("user_type");
                Intent intent;
                if ("teacher".equals(userType)) {
                    intent = new Intent(this, TeacherHomeActivity.class);
                } else {
                    intent = new Intent(this, MainContainerActivity.class);
                    intent.putExtra("fragment", "home");
                }
                startActivity(intent);
            } catch (Exception e) {
                // Default to student home if error
                Intent intent = new Intent(this, MainContainerActivity.class);
                intent.putExtra("fragment", "home");
                startActivity(intent);
            }
        } else {
            // No user session, redirect to login
            Intent intent = new Intent(this, UserOptionActivity.class);
            startActivity(intent);
            finish();
        }
    }
    
    private void loadMentors() {
        // Get category from intent if provided
        int categoryId = getIntent().getIntExtra("category_id", 0);
        String categoryName = getIntent().getStringExtra("category_name");
        
        // Load mentors filtered by category if provided
        if (categoryId > 0) {
            apiHelper.getMentors(categoryId, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    handleMentorsResponse(response, categoryName);
                }
                
                @Override
                public void onError(String error) {
                    handleMentorsError(error);
                }
            });
        } else {
            apiHelper.getMentors(new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    handleMentorsResponse(response, null);
                }
                
                @Override
                public void onError(String error) {
                    handleMentorsError(error);
                }
            });
        }
    }
    
    private void handleMentorsResponse(String response, String categoryName) {
        runOnUiThread(() -> {
            try {
                Log.d("MentorsActivity", "Response: " + response);
                JSONObject jsonResponse = new JSONObject(response);
                
                // Check both 'success' (boolean) and 'status' (string) fields
                boolean success = jsonResponse.optBoolean("success", false) || 
                                "success".equals(jsonResponse.optString("status", ""));
                
                if (success && jsonResponse.has("data")) {
                    JSONArray teachersArray = jsonResponse.getJSONArray("data");
                    mentorsList.clear();
                    
                    for (int i = 0; i < teachersArray.length(); i++) {
                        JSONObject teacher = teachersArray.getJSONObject(i);
                        mentorsList.add(teacher);
                    }
                    
                    mentorAdapter.notifyDataSetChanged();
                    
                    if (mentorsList.isEmpty()) {
                        if (categoryName != null && !categoryName.isEmpty()) {
                            titleText.setText("No Teachers in " + categoryName);
                        } else {
                            titleText.setText("No Teachers Available");
                        }
                        showNoTeachersMessage();
                    } else {
                        if (categoryName != null && !categoryName.isEmpty()) {
                            titleText.setText(categoryName + " Teachers (" + mentorsList.size() + ")");
                        } else {
                            titleText.setText("Available Teachers (" + mentorsList.size() + ")");
                        }
                    }
                    
                } else {
                    showNoTeachersMessage();
                }
            } catch (Exception e) {
                Log.e("MentorsActivity", "Error parsing teachers", e);
                showNoTeachersMessage();
            }
        });
    }
    
    private void handleMentorsError(String error) {
        runOnUiThread(() -> {
            Log.e("MentorsActivity", "Error loading teachers: " + error);
            showNoTeachersMessage();
        });
    }
    
    private void showNoTeachersMessage() {
        try {
            mentorsList.clear();
            
            // Show message that no teachers are registered yet
            JSONObject noTeacher = new JSONObject();
            noTeacher.put("user_id", 0);
            noTeacher.put("full_name", "No Teachers Registered Yet");
            noTeacher.put("specialization", "Be the first to join as a teacher!");
            noTeacher.put("experience_years", 0);
            noTeacher.put("course_count", 0);
            noTeacher.put("avg_rating", 0.0);
            noTeacher.put("is_verified", false);
            noTeacher.put("bio", "No teachers have registered on the platform yet. If you're a music teacher, sign up to start teaching!");
            mentorsList.add(noTeacher);
            
            mentorAdapter.notifyDataSetChanged();
            titleText.setText("No Teachers Available");
            
        } catch (Exception e) {
            Log.e("MentorsActivity", "Error showing no teachers message", e);
        }
    }
    
    private void loadDemoMentors() {
        try {
            mentorsList.clear();
            
            // Demo Mentor 1 - Classical Music Expert
            JSONObject mentor1 = new JSONObject();
            mentor1.put("user_id", 101);
            mentor1.put("full_name", "Priya Sharma");
            mentor1.put("specialization", "Classical Music & Vocal Training");
            mentor1.put("experience_years", 12);
            mentor1.put("course_count", 8);
            mentor1.put("avg_rating", 4.8);
            mentor1.put("is_verified", true);
            mentor1.put("bio", "Renowned classical music teacher with expertise in Indian classical vocal music and instrumental training. Holds a Master's degree in Hindustani Classical Music.");
            mentorsList.add(mentor1);
            
            // Demo Mentor 2 - Guitar Specialist
            JSONObject mentor2 = new JSONObject();
            mentor2.put("user_id", 102);
            mentor2.put("full_name", "Ravi Kumar");
            mentor2.put("specialization", "Guitar & Contemporary Music");
            mentor2.put("experience_years", 8);
            mentor2.put("course_count", 5);
            mentor2.put("avg_rating", 4.6);
            mentor2.put("is_verified", true);
            mentor2.put("bio", "Professional guitarist and music producer with experience in rock, blues, and contemporary music styles. Specializes in electric and acoustic guitar techniques.");
            mentorsList.add(mentor2);
            
            // Demo Mentor 3 - Piano Teacher
            JSONObject mentor3 = new JSONObject();
            mentor3.put("user_id", 103);
            mentor3.put("full_name", "Anjali Mehta");
            mentor3.put("specialization", "Piano & Keyboard");
            mentor3.put("experience_years", 15);
            mentor3.put("course_count", 12);
            mentor3.put("avg_rating", 4.9);
            mentor3.put("is_verified", true);
            mentor3.put("bio", "Expert piano instructor with classical training and contemporary expertise. Teaches students from beginner to advanced levels in both classical and modern piano techniques.");
            mentorsList.add(mentor3);
            
            // Demo Mentor 4 - Drums & Percussion
            JSONObject mentor4 = new JSONObject();
            mentor4.put("user_id", 104);
            mentor4.put("full_name", "Arjun Singh");
            mentor4.put("specialization", "Drums & Percussion");
            mentor4.put("experience_years", 10);
            mentor4.put("course_count", 6);
            mentor4.put("avg_rating", 4.7);
            mentor4.put("is_verified", false);
            mentor4.put("bio", "Professional drummer with experience in various music genres. Specializes in both traditional and modern drumming techniques with focus on rhythm and timing.");
            mentorsList.add(mentor4);
            
            // Demo Mentor 5 - Music Theory
            JSONObject mentor5 = new JSONObject();
            mentor5.put("user_id", 105);
            mentor5.put("full_name", "Dr. Sunita Rao");
            mentor5.put("specialization", "Music Theory & Composition");
            mentor5.put("experience_years", 20);
            mentor5.put("course_count", 15);
            mentor5.put("avg_rating", 4.9);
            mentor5.put("is_verified", true);
            mentor5.put("bio", "PhD in Music Theory with extensive experience in teaching composition, harmony, and music analysis. Published researcher in contemporary music theory.");
            mentorsList.add(mentor5);
            
            mentorAdapter.notifyDataSetChanged();
            titleText.setText("All Mentors (" + mentorsList.size() + ")");
            
        } catch (Exception e) {
            Log.e("MentorsActivity", "Error creating demo mentors", e);
        }
    }
    
    private static class MentorAdapter extends BaseAdapter {
        private Context context;
        private List<JSONObject> mentors;
        private LayoutInflater inflater;
        
        public MentorAdapter(Context context, List<JSONObject> mentors) {
            this.context = context;
            this.mentors = mentors;
            this.inflater = LayoutInflater.from(context);
        }
        
        @Override
        public int getCount() {
            return mentors.size();
        }
        
        @Override
        public Object getItem(int position) {
            return mentors.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_mentor, parent, false);
                holder = new ViewHolder();
                holder.mentorName = convertView.findViewById(R.id.mentor_name);
                holder.specialization = convertView.findViewById(R.id.mentor_specialization);
                holder.experience = convertView.findViewById(R.id.mentor_experience);
                holder.courseCount = convertView.findViewById(R.id.course_count);
                holder.verificationBadge = convertView.findViewById(R.id.verification_badge);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            
            try {
                JSONObject mentor = mentors.get(position);
                holder.mentorName.setText(mentor.getString("full_name"));
                holder.specialization.setText(mentor.getString("specialization"));
                holder.experience.setText(mentor.getInt("experience_years") + " years experience");
                holder.courseCount.setText(mentor.getInt("course_count") + " courses");
                
                double avgRating = mentor.getDouble("avg_rating");
                if (avgRating > 0) {
                    holder.rating.setText(String.format("%.1f ★", avgRating));
                } else {
                    holder.rating.setText("New");
                }
                
                // Handle verification badge - can be integer or boolean
                boolean isVerified = false;
                try {
                    isVerified = mentor.getBoolean("is_verified");
                } catch (Exception e) {
                    // If it's an integer, convert it
                    int verifiedInt = mentor.optInt("is_verified", 0);
                    isVerified = (verifiedInt == 1);
                }
                holder.verificationBadge.setVisibility(isVerified ? View.VISIBLE : View.GONE);
                
            } catch (Exception e) {
                Log.e("MentorAdapter", "Error binding mentor data", e);
            }
            
            return convertView;
        }
        
        private static class ViewHolder {
            TextView mentorName;
            TextView specialization;
            TextView experience;
            TextView courseCount;
            TextView rating;
            ImageView verificationBadge;
        }
    }
}
