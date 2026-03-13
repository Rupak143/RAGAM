package com.example.ragamfinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

public class StudentSignUpActivity extends AppCompatActivity {
    
    private EditText fullNameInput, emailInput, phoneInput, passwordInput, confirmPasswordInput;
    private Button signUpButton;
    private TextView signInLink;
    private ApiHelper apiHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_signup);
        
        initViews();
        setupClickListeners();
        
        apiHelper = new ApiHelper(this);
    }
    
    private void initViews() {
        fullNameInput = findViewById(R.id.fullname_input);
        emailInput = findViewById(R.id.email_input);
        phoneInput = findViewById(R.id.phone_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        signUpButton = findViewById(R.id.signup_button);
        signInLink = findViewById(R.id.signin_here);
    }
    
    private void setupClickListeners() {
        signUpButton.setOnClickListener(v -> performRegistration());
        signInLink.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentSignInActivity.class);
            startActivity(intent);
            finish();
        });
    }
    
    private void performRegistration() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        
        if (fullName.isEmpty()) {
            fullNameInput.setError("Full name is required");
            return;
        }

        if (!fullName.matches("[a-zA-Z ]+")) {
            fullNameInput.setError("Name must contain letters only");
            return;
        }
        
        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            return;
        }
        
        if (!email.toLowerCase().endsWith("@gmail.com")) {
            emailInput.setError("Enter the valid mail");
            return;
        }
        
        if (!phone.isEmpty()) {
            if (!phone.matches("[6-9][0-9]{9}")) {
                phoneInput.setError("Enter the valid phone number");
                return;
            }
        }
        
        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            return;
        }
        
        if (password.length() < 8) {
            passwordInput.setError("Password must be at least 8 characters");
            return;
        }
        
        if (!password.matches(".*[a-z].*")) {
            passwordInput.setError("Password must contain at least one lowercase letter");
            return;
        }
        
        if (!password.matches(".*[A-Z].*")) {
            passwordInput.setError("Password must contain at least one uppercase letter");
            return;
        }
        
        if (!password.matches(".*[0-9].*")) {
            passwordInput.setError("Password must contain at least one number");
            return;
        }
        
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            passwordInput.setError("Password must contain at least one special character");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            return;
        }
        
        // Show loading
        signUpButton.setEnabled(false);
        signUpButton.setText("Creating account...");
        
        apiHelper.register(email, password, fullName, "student", phone, "", 0, "", new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        // Check for both "success" (boolean) and "status" (string) for compatibility
                        boolean isSuccess = false;
                        if (jsonResponse.has("success")) {
                            isSuccess = jsonResponse.getBoolean("success");
                        } else if (jsonResponse.has("status")) {
                            String status = jsonResponse.getString("status");
                            isSuccess = "success".equals(status);
                        }
                        
                        if (isSuccess) {
                            // Don't save session immediately, let user login manually
                            Toast.makeText(StudentSignUpActivity.this, "Account created successfully! Please sign in with your credentials.", Toast.LENGTH_LONG).show();
                            
                            // Redirect to login page
                            Intent intent = new Intent(StudentSignUpActivity.this, StudentSignInActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            String message = jsonResponse.optString("message", "Registration failed");
                            Toast.makeText(StudentSignUpActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("StudentSignUp", "Error parsing response", e);
                        Log.e("StudentSignUp", "Response was: " + response);
                        Toast.makeText(StudentSignUpActivity.this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                    
                    signUpButton.setEnabled(true);
                    signUpButton.setText("SIGN UP");
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(StudentSignUpActivity.this, error, Toast.LENGTH_SHORT).show();
                    signUpButton.setEnabled(true);
                    signUpButton.setText("SIGN UP");
                });
            }
        });
    }
}
