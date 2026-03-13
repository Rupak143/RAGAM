package com.example.ragamfinal;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 3000; // 3 seconds
    private ApiHelper apiHelper;
    private Handler splashHandler;
    private Runnable sessionCheckRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        apiHelper = new ApiHelper(this);

        splashHandler = new Handler();
        sessionCheckRunnable = this::checkUserSession;

        // Handle get started button click
        LinearLayout getStartedButton = findViewById(R.id.sign_in_button);
        getStartedButton.setOnClickListener(v -> {
            splashHandler.removeCallbacks(sessionCheckRunnable);
            checkUserSession();
        });

        // Add long press to force logout (for testing/debugging)
        getStartedButton.setOnLongClickListener(v -> {
            splashHandler.removeCallbacks(sessionCheckRunnable);
            apiHelper.logout();
            Intent intent = new Intent(this, UserOptionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        });

        // Auto navigate after splash delay
        splashHandler.postDelayed(sessionCheckRunnable, SPLASH_DELAY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (splashHandler != null) {
            splashHandler.removeCallbacks(sessionCheckRunnable);
        }
    }
    
    private void checkUserSession() {
        if (apiHelper.isLoggedIn()) {
            JSONObject user = apiHelper.getUserSession();
            if (user != null) {
                try {
                    String userType = user.getString("user_type");
                    if ("student".equals(userType)) {
                        Intent intent = new Intent(this, MainContainerActivity.class);
                        intent.putExtra("fragment", "home");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(this, TeacherMainContainerActivity.class);
                        intent.putExtra("fragment", "home");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    // If error parsing user data, go to user option
                    Intent intent = new Intent(this, UserOptionActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            } else {
                Intent intent = new Intent(this, UserOptionActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        } else {
            Intent intent = new Intent(this, UserOptionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        finish();
    }
}
