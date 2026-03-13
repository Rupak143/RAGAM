package com.example.ragamfinal;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {
    
    private ImageView homeIcon, coursesIcon, profileIcon, profilePhotoImage;
    private TextView userNameText, userEmailText, userTypeText, enrolledCoursesText, memberSinceText;
    private LinearLayout editProfileButton, certificatesOption, termsOption;
    private Button logoutButton;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        // Initialize ApiHelper first
        apiHelper = new ApiHelper(this);
        currentUser = apiHelper.getUserSession();
        
        initViews();
        setupBottomNavigation();
        loadUserProfile();
        setupClickListeners();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("ProfileActivity", "onResume called - refreshing profile");
        // Refresh profile data when returning from edit profile
        currentUser = apiHelper.getUserSession();
        if (currentUser != null) {
            loadUserProfile();
        }
    }
    
    private void initViews() {
        homeIcon = findViewById(R.id.ic_home);
        coursesIcon = findViewById(R.id.ic_courses);
        profileIcon = findViewById(R.id.ic_profile);
        profilePhotoImage = findViewById(R.id.profile_photo_image);
        
        // Debug log to check if ImageView is found
        Log.d("ProfileActivity", "ProfilePhotoImage found: " + (profilePhotoImage != null));
        
        userNameText = findViewById(R.id.user_name);
        userEmailText = findViewById(R.id.user_email);
        userTypeText = findViewById(R.id.user_type);
        enrolledCoursesText = findViewById(R.id.enrolled_courses);
        memberSinceText = findViewById(R.id.member_since);
        
        editProfileButton = findViewById(R.id.edit_profile_button);
        certificatesOption = findViewById(R.id.certificates_option);
        termsOption = findViewById(R.id.terms_option);
        logoutButton = findViewById(R.id.logout_button);
    }
    
    private void setupBottomNavigation() {
        // Check user type and hide courses icon and certificates for teachers
        try {
            if (currentUser != null) {
                String userType = currentUser.getString("user_type");
                if ("teacher".equals(userType)) {
                    // Hide courses icon for teachers (only 2 icons: home and profile)
                    coursesIcon.setVisibility(View.GONE);
                    // Hide certificates option for teachers
                    certificatesOption.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error checking user type", e);
        }
        
        // Highlight profile icon (current page)
        highlightBottomNavIcon(profileIcon);
        
        // Setup navigation for all users
        homeIcon.setOnClickListener(v -> {
            navigateToHome();
        });

        coursesIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, CoursesActivity.class);
            startActivity(intent);
        });

        profileIcon.setOnClickListener(v -> {
            // Already on profile
            highlightBottomNavIcon(profileIcon);
        });
    }
    
    private void navigateToHome() {
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
            redirectToLogin();
        }
    }
    
    private void setupClickListeners() {
        editProfileButton.setOnClickListener(v -> {
            try {
                String userType = currentUser.getString("user_type");
                Intent intent;
                if ("teacher".equals(userType)) {
                    intent = new Intent(this, TeacherEditProfileActivity.class);
                } else {
                    intent = new Intent(this, EditProfileActivity.class);
                }
                startActivity(intent);
            } catch (Exception e) {
                Log.e("ProfileActivity", "Error navigating to edit profile", e);
                Toast.makeText(this, "Error opening edit profile", Toast.LENGTH_SHORT).show();
            }
        });
        
        certificatesOption.setOnClickListener(v -> {
            Intent intent = new Intent(this, CertificatesActivity.class);
            startActivity(intent);
        });
        
        termsOption.setOnClickListener(v -> {
            Intent intent = new Intent(this, TermsConditionsActivity.class);
            startActivity(intent);
        });
        
        logoutButton.setOnClickListener(v -> {
            performLogout();
        });
    }
    
    private void loadUserProfile() {
        if (currentUser == null) {
            Toast.makeText(this, "Please login to view profile", Toast.LENGTH_SHORT).show();
            redirectToLogin();
            return;
        }
        
        try {
            // Display basic user info
            userNameText.setText(currentUser.getString("full_name"));
            userEmailText.setText(currentUser.getString("email"));
            
            String userType = currentUser.getString("user_type");
            userTypeText.setText(userType.substring(0, 1).toUpperCase() + userType.substring(1));
            
            // Format member since date
            String createdAt = currentUser.optString("created_at", "");
            if (!createdAt.isEmpty()) {
                try {
                    // Extract year from date string
                    String year = createdAt.substring(0, 4);
                    memberSinceText.setText("Member since " + year);
                } catch (Exception e) {
                    memberSinceText.setText("Member since 2024");
                }
            } else {
                memberSinceText.setText("Member since 2024");
            }
            
            // Load additional profile data from API
            int userId = currentUser.getInt("user_id");
            loadDetailedProfile(userId);
            
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error loading user profile", e);
            Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        }
    }
    
    private void loadDetailedProfile(int userId) {
        apiHelper.getUserProfile(userId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        Log.d("ProfileActivity", "Detailed profile API response: " + response);
                        JSONObject jsonResponse = new JSONObject(response);
                        String status = jsonResponse.getString("status");
                        
                        if ("success".equals(status)) {
                            JSONObject profileData = jsonResponse.getJSONObject("data");
                            Log.d("ProfileActivity", "Profile data: " + profileData.toString());
                            
                            // Load profile image from fresh API data
                            String profileImage = profileData.optString("profile_image", "");
                            Log.d("ProfileActivity", "Profile image from API: " + profileImage);
                            
                            if (!profileImage.isEmpty() && profilePhotoImage != null) {
                                    String imageUrl = "http://192.168.48.1/ragamfinal/uploads/" + profileImage + "?t=" + System.currentTimeMillis();
                                Log.d("ProfileActivity", "Loading image from URL: " + imageUrl);
                                
                                Glide.with(ProfileActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .skipMemoryCache(true) // Skip memory cache to get fresh image
                                    .centerCrop() // Maintain aspect ratio and fill the ImageView
                                    .into(profilePhotoImage);
                            } else {
                                Log.d("ProfileActivity", "No profile image or ImageView is null");
                                // Set default image if no profile image
                                if (profilePhotoImage != null) {
                                    profilePhotoImage.setImageResource(R.drawable.ic_profile);
                                }
                            }
                            
                            // Update enrolled courses count for students
                            if ("student".equals(profileData.getString("user_type"))) {
                                int enrolledCount = profileData.optInt("enrolled_courses", 0);
                                enrolledCoursesText.setText("Enrolled in " + enrolledCount + " courses");
                            } else {
                                int createdCount = profileData.optInt("created_courses", 0);
                                enrolledCoursesText.setText("Created " + createdCount + " courses");
                            }
                            
                        }
                    } catch (Exception e) {
                        Log.e("ProfileActivity", "Error parsing profile response", e);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e("ProfileActivity", "Error loading detailed profile: " + error);
                    // Don't show error to user for this optional data
                });
            }
        });
    }
    
    private void performLogout() {
        apiHelper.logout();
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(this, UserOptionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void highlightBottomNavIcon(ImageView selectedIcon) {
        // Reset all icons to default color
        homeIcon.setColorFilter(Color.GRAY);
        if (coursesIcon.getVisibility() == View.VISIBLE) {
            coursesIcon.setColorFilter(Color.GRAY);
        }
        profileIcon.setColorFilter(Color.GRAY);
        
        // Highlight selected icon
        selectedIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
    }
}
