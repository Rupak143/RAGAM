package com.example.ragamfinal.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.ragamfinal.CertificatesActivity;
import com.example.ragamfinal.EditProfileActivity;
import com.example.ragamfinal.R;
import com.example.ragamfinal.TeacherEditProfileActivity;
import com.example.ragamfinal.TermsConditionsActivity;
import com.example.ragamfinal.UserOptionActivity;
import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

public class ProfileFragment extends Fragment {
    
    private ImageView profilePhotoImage;
    private TextView userNameText, userEmailText, userTypeText, enrolledCoursesText, memberSinceText;
    private LinearLayout editProfileButton, certificatesOption, termsOption;
    private Button logoutButton;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    private static final String TAG = "ProfileFragment";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        // Initialize ApiHelper first
        apiHelper = new ApiHelper(requireContext());
        currentUser = apiHelper.getUserSession();
        
        initViews(view);
        loadUserProfile();
        setupClickListeners();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called - refreshing profile");
        // Refresh profile data when returning
        currentUser = apiHelper.getUserSession();
        if (currentUser != null) {
            loadUserProfile();
        }
    }
    
    private void initViews(View view) {
        profilePhotoImage = view.findViewById(R.id.profile_photo_image);
        
        Log.d(TAG, "ProfilePhotoImage found: " + (profilePhotoImage != null));
        
        userNameText = view.findViewById(R.id.user_name);
        userEmailText = view.findViewById(R.id.user_email);
        userTypeText = view.findViewById(R.id.user_type);
        enrolledCoursesText = view.findViewById(R.id.enrolled_courses);
        memberSinceText = view.findViewById(R.id.member_since);
        
        editProfileButton = view.findViewById(R.id.edit_profile_button);
        certificatesOption = view.findViewById(R.id.certificates_option);
        termsOption = view.findViewById(R.id.terms_option);
        logoutButton = view.findViewById(R.id.logout_button);
        
        // Check user type and hide certificates for teachers
        try {
            if (currentUser != null) {
                String userType = currentUser.getString("user_type");
                if ("teacher".equals(userType)) {
                    certificatesOption.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking user type", e);
        }
    }
    
    private void setupClickListeners() {
        editProfileButton.setOnClickListener(v -> {
            try {
                String userType = currentUser.getString("user_type");
                Intent intent;
                if ("teacher".equals(userType)) {
                    intent = new Intent(requireContext(), TeacherEditProfileActivity.class);
                } else {
                    intent = new Intent(requireContext(), EditProfileActivity.class);
                }
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error navigating to edit profile", e);
                Toast.makeText(requireContext(), "Error opening edit profile", Toast.LENGTH_SHORT).show();
            }
        });
        
        certificatesOption.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), CertificatesActivity.class);
            startActivity(intent);
        });
        
        termsOption.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TermsConditionsActivity.class);
            startActivity(intent);
        });
        
        logoutButton.setOnClickListener(v -> {
            performLogout();
        });
    }
    
    private void loadUserProfile() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login to view profile", Toast.LENGTH_SHORT).show();
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
            Log.e(TAG, "Error loading user profile", e);
            Toast.makeText(requireContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
            redirectToLogin();
        }
    }
    
    private void loadDetailedProfile(int userId) {
        apiHelper.getUserProfile(userId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    try {
                        Log.d(TAG, "Detailed profile API response: " + response);
                        JSONObject jsonResponse = new JSONObject(response);
                        String status = jsonResponse.getString("status");
                        
                        if ("success".equals(status)) {
                            JSONObject profileData = jsonResponse.getJSONObject("data");
                            Log.d(TAG, "Profile data: " + profileData.toString());
                            
                            // Load profile image
                            String profileImage = profileData.optString("profile_image", "");
                            Log.d(TAG, "Profile image from API: " + profileImage);
                            
                            if (!profileImage.isEmpty() && profilePhotoImage != null) {
                                String imageUrl = "http://192.168.48.1/ragam/uploads/" + profileImage + "?t=" + System.currentTimeMillis();
                                Log.d(TAG, "Loading image from URL: " + imageUrl);
                                
                                Glide.with(ProfileFragment.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_profile)
                                    .error(R.drawable.ic_profile)
                                    .skipMemoryCache(true)
                                    .centerCrop()
                                    .into(profilePhotoImage);
                            } else {
                                Log.d(TAG, "No profile image or ImageView is null");
                                if (profilePhotoImage != null) {
                                    profilePhotoImage.setImageResource(R.drawable.ic_profile);
                                }
                            }
                            
                            // Update enrolled courses count
                            if ("student".equals(profileData.getString("user_type"))) {
                                int enrolledCount = profileData.optInt("enrolled_courses", 0);
                                enrolledCoursesText.setText("Enrolled in " + enrolledCount + " courses");
                            } else {
                                int createdCount = profileData.optInt("created_courses", 0);
                                enrolledCoursesText.setText("Created " + createdCount + " courses");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing profile response", e);
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                
                getActivity().runOnUiThread(() -> {
                    Log.e(TAG, "Error loading detailed profile: " + error);
                });
            }
        });
    }
    
    private void performLogout() {
        apiHelper.logout();
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }
    
    private void redirectToLogin() {
        Intent intent = new Intent(requireContext(), UserOptionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
