package com.example.ragamfinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ragamfinal.config.AppConfig;
import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

public class StudentSignInActivity extends AppCompatActivity {
    
    private EditText emailInput, passwordInput;
    private Button signInButton;
    private TextView signUpLink;
    private ApiHelper apiHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_signin);
        
        initViews();
        setupClickListeners();
        
        apiHelper = new ApiHelper(this);
    }
    
    private void initViews() {
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        signInButton = findViewById(R.id.signin_button);
        signUpLink = findViewById(R.id.signup_here);
    }
    
    private void setupClickListeners() {
        signInButton.setOnClickListener(v -> performLogin());
        signUpLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentSignUpActivity.class);
            startActivity(intent);
        });
    }
    
    private void performLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        
        Log.d("StudentSignIn", "Attempting login with email: " + email + ", password: " + password);
        
        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return;
        }
        
        if (!email.toLowerCase().endsWith("@gmail.com")) {
            emailInput.setError("Email must end with @gmail.com");
            return;
        }
        
        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            return;
        }
        
        // Show loading
        signInButton.setEnabled(false);
        signInButton.setText("Signing in...");
        
        Log.d("StudentSignIn", "Calling API with URL: " + AppConfig.BASE_URL + "auth.php?action=login");
        
        apiHelper.login(email, password, "student", new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    Log.d("StudentSignIn", "API Success Response: " + response);
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        boolean success = jsonResponse.getBoolean("success");
                        
                        Log.d("StudentSignIn", "Success flag: " + success);
                        
                        if (success) {
                            JSONObject userData = jsonResponse.getJSONObject("data");
                            Log.d("StudentSignIn", "User data: " + userData.toString());
                            
                            apiHelper.saveUserSession(userData);
                            
                            Toast.makeText(StudentSignInActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                            
                            Intent intent = new Intent(StudentSignInActivity.this, MainContainerActivity.class);
                            intent.putExtra("fragment", "home");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            String message = jsonResponse.getString("message");
                            Log.e("StudentSignIn", "Login failed: " + message);
                            Toast.makeText(StudentSignInActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("StudentSignIn", "Error parsing response", e);
                        Toast.makeText(StudentSignInActivity.this, "Login failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                    
                    signInButton.setEnabled(true);
                    signInButton.setText("SIGN IN");
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e("StudentSignIn", "API Error: " + error);
                    Toast.makeText(StudentSignInActivity.this, "Connection failed: " + error, Toast.LENGTH_LONG).show();
                    signInButton.setEnabled(true);
                    signInButton.setText("SIGN IN");
                });
            }
        });
    }
}
