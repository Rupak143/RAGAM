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

import com.example.ragamfinal.fragments.TeacherHomeFragment;
import com.example.ragamfinal.fragments.TeacherProfileFragment;
import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

public class TeacherMainContainerActivity extends AppCompatActivity {
    
    private ImageView homeIcon, profileIcon;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    private static final String TAG = "TeacherMainContainer";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_main_container);
        
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
        profileIcon = findViewById(R.id.ic_profile);
        
        homeIcon.setOnClickListener(v -> {
            loadFragment("home");
        });
        
        profileIcon.setOnClickListener(v -> {
            loadFragment("profile");
        });
    }
    
    private void loadFragment(String fragmentName) {
        Fragment fragment = null;
        
        switch (fragmentName) {
            case "home":
                fragment = new TeacherHomeFragment();
                highlightBottomNavIcon(homeIcon);
                break;
            case "profile":
                fragment = new TeacherProfileFragment();
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
