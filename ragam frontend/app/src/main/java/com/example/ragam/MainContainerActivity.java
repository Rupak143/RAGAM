package com.example.ragamfinal;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.ragamfinal.fragments.CoursesFragment;
import com.example.ragamfinal.fragments.ProfileFragment;
import com.example.ragamfinal.fragments.StudentHomeFragment;
import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

public class MainContainerActivity extends AppCompatActivity {
    
    private ImageView homeIcon, coursesIcon, profileIcon;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    private static final String TAG = "MainContainerActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_container);
        
        apiHelper = new ApiHelper(this);
        currentUser = apiHelper.getUserSession();
        
        initBottomNavigation();
        
        // Check which fragment to load from intent
        String fragmentToLoad = getIntent().getStringExtra("fragment");
        if (fragmentToLoad == null) {
            fragmentToLoad = "home";
        }
        
        loadFragment(fragmentToLoad);
    }
    
    private void initBottomNavigation() {
        homeIcon = findViewById(R.id.ic_home);
        coursesIcon = findViewById(R.id.ic_courses);
        profileIcon = findViewById(R.id.ic_profile);
        
        // Check user type and hide courses icon for teachers
        try {
            if (currentUser != null) {
                String userType = currentUser.getString("user_type");
                if ("teacher".equals(userType)) {
                    coursesIcon.setVisibility(ImageView.GONE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking user type", e);
        }
        
        homeIcon.setOnClickListener(v -> {
            loadFragment("home");
        });
        
        coursesIcon.setOnClickListener(v -> {
            loadFragment("courses");
        });
        
        profileIcon.setOnClickListener(v -> {
            loadFragment("profile");
        });
    }
    
    private void loadFragment(String fragmentName) {
        Fragment fragment = null;
        
        switch (fragmentName) {
            case "home":
                fragment = new StudentHomeFragment();
                highlightBottomNavIcon(homeIcon);
                break;
            case "courses":
                fragment = new CoursesFragment();
                highlightBottomNavIcon(coursesIcon);
                break;
            case "profile":
                fragment = new ProfileFragment();
                highlightBottomNavIcon(profileIcon);
                break;
        }
        
        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        }
    }
    
    private void highlightBottomNavIcon(ImageView selectedIcon) {
        // Reset all icons to default color
        homeIcon.setColorFilter(Color.GRAY);
        coursesIcon.setColorFilter(Color.GRAY);
        profileIcon.setColorFilter(Color.GRAY);
        
        // Highlight selected icon
        selectedIcon.setColorFilter(ContextCompat.getColor(this, R.color.rounded_button_orange));
    }
    
    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
