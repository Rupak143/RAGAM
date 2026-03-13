package com.example.ragamfinal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class UserOptionActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_option);
        
        LinearLayout studentLoginButton = findViewById(R.id.student_login_button);
        LinearLayout teacherLoginButton = findViewById(R.id.teacher_login_button);
        
        studentLoginButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentSignInActivity.class);
            startActivity(intent);
        });
        
        teacherLoginButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, TeacherSignInActivity.class);
            startActivity(intent);
        });
    }
}
