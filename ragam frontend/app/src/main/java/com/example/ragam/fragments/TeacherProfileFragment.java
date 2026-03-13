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
import com.example.ragamfinal.R;
import com.example.ragamfinal.TeacherEditProfileActivity;
import com.example.ragamfinal.TermsConditionsActivity;
import com.example.ragamfinal.UserOptionActivity;
import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

public class TeacherProfileFragment extends Fragment {
    
    private ImageView profilePhotoImage;
    private TextView profileNameText, profileEmailText;
    private LinearLayout editProfileOption, termsOption;
    private Button logoutButton;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    private static final String TAG = "TeacherProfileFragment";
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_profile, container, false);
        
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
        currentUser = apiHelper.getUserSession();
        if (currentUser != null) {
            loadUserProfile();
        }
    }
    
    private void initViews(View view) {
        profilePhotoImage = view.findViewById(R.id.profile_photo_image);
        profileNameText = view.findViewById(R.id.profile_name);
        profileEmailText = view.findViewById(R.id.profile_email);
        
        editProfileOption = view.findViewById(R.id.edit_profile_option);
        termsOption = view.findViewById(R.id.terms_option);
        logoutButton = view.findViewById(R.id.logout_button);
    }
    
    private void setupClickListeners() {
        editProfileOption.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TeacherEditProfileActivity.class);
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
            profileNameText.setText(currentUser.getString("full_name"));
            profileEmailText.setText(currentUser.getString("email"));
            
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
                                String imageUrl = "http://192.168.48.1/ragamfinal/uploads/" + profileImage + "?t=" + System.currentTimeMillis();
                                Log.d(TAG, "Loading image from URL: " + imageUrl);
                                
                                Glide.with(TeacherProfileFragment.this)
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
