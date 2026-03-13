package com.example.ragam

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.ragamfinal.utils.ApiHelper
import org.json.JSONObject

class SignUpActivity : AppCompatActivity() {

    private lateinit var apiHelper: ApiHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize API helper
        apiHelper = ApiHelper(this)

        // Get view references
        val firstName = findViewById<EditText>(R.id.firstName)
        val email = findViewById<EditText>(R.id.email)
        val phoneNumber = findViewById<EditText>(R.id.phoneNumber)
        val gender = findViewById<EditText>(R.id.gender)
        val dob = findViewById<EditText>(R.id.dob)
        val password = findViewById<EditText>(R.id.password)
        val confirmPassword = findViewById<EditText>(R.id.confirmPassword)
        val signUpButton = findViewById<Button>(R.id.signupButton)

        signUpButton.setOnClickListener {

            val fName = firstName.text.toString().trim()
            val userEmail = email.text.toString().trim()
            val phone = phoneNumber.text.toString().trim()
            val userGender = gender.text.toString().trim()
            val userDob = dob.text.toString().trim()
            val userPassword = password.text.toString().trim()
            val userConfirmPassword = confirmPassword.text.toString().trim()

            // Input validations
            if (TextUtils.isEmpty(fName) || TextUtils.isEmpty(userEmail) ||
                TextUtils.isEmpty(phone) || TextUtils.isEmpty(userGender) ||
                TextUtils.isEmpty(userDob) || TextUtils.isEmpty(userPassword) ||
                TextUtils.isEmpty(userConfirmPassword)
            ) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userPassword != userConfirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Email validation
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Password length validation
            if (userPassword.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent double submission
            signUpButton.isEnabled = false

            // Register user via API (real database registration)
            registerUser(fName, userEmail, phone, userGender, userDob, userPassword, signUpButton)
        }
    }

    private fun registerUser(fullName: String, email: String, phone: String, 
                            gender: String, dob: String, password: String, 
                            signUpButton: Button) {
        
        // Default to student, but you can add a user type selector in the UI
        val userType = "student"
        val bio = "Gender: $gender, DOB: $dob"
        val specialization = ""
        val experienceYears = 0

        Log.d("SignUpActivity", "Registering user: $email")

        apiHelper.register(email, password, fullName, userType, phone, bio, 
                          experienceYears, specialization, 
            object : ApiHelper.ApiCallback {
                override fun onSuccess(response: String) {
                    runOnUiThread {
                        try {
                            val jsonResponse = JSONObject(response)
                            if (jsonResponse.getBoolean("success")) {
                                Toast.makeText(this@SignUpActivity, 
                                    "Registration Successful! Please login.", 
                                    Toast.LENGTH_LONG).show()
                                
                                Log.d("SignUpActivity", "Registration successful")
                                
                                // Navigate to Login Options Page
                                val intent = Intent(this@SignUpActivity, UserOptionLoginActivity::class.java)
                                intent.putExtra("registered_email", email)
                                startActivity(intent)
                                finish()
                            } else {
                                val errorMsg = jsonResponse.optString("message", "Registration failed")
                                Toast.makeText(this@SignUpActivity, errorMsg, Toast.LENGTH_LONG).show()
                                signUpButton.isEnabled = true
                            }
                        } catch (e: Exception) {
                            Log.e("SignUpActivity", "Error parsing response: ${e.message}")
                            Toast.makeText(this@SignUpActivity, 
                                "Registration error: ${e.message}", 
                                Toast.LENGTH_LONG).show()
                            signUpButton.isEnabled = true
                        }
                    }
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        Log.e("SignUpActivity", "Registration error: $error")
                        Toast.makeText(this@SignUpActivity, 
                            "Registration failed: $error", 
                            Toast.LENGTH_LONG).show()
                        signUpButton.isEnabled = true
                    }
                }
            })
    }
}
