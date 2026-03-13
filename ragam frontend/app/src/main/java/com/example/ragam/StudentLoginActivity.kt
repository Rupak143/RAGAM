package com.example.ragam

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.ragamfinal.utils.ApiHelper
import com.example.ragamfinal.StudentDashboardActivity
import org.json.JSONObject

class StudentLoginActivity : AppCompatActivity() {
    
    private lateinit var apiHelper: ApiHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        // Initialize API Helper
        apiHelper = ApiHelper(this)

        val titleText = findViewById<TextView>(R.id.signin_title)
        titleText.text = "Student Login"

        val email = findViewById<EditText>(R.id.email_input)
        val password = findViewById<EditText>(R.id.password_input)
        val signInBtn = findViewById<Button>(R.id.signin_button)
        val signupLink = findViewById<TextView>(R.id.signup_here)

        // Pre-fill email if coming from registration
        val registeredEmail = intent.getStringExtra("registered_email")
        if (!registeredEmail.isNullOrEmpty()) {
            email.setText(registeredEmail)
        }

        signInBtn.setOnClickListener {
            val userEmail = email.text.toString().trim()
            val userPassword = password.text.toString().trim()

            // Validate inputs
            if (TextUtils.isEmpty(userEmail) || TextUtils.isEmpty(userPassword)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent double submission
            signInBtn.isEnabled = false

            // Authenticate user with database
            authenticateStudent(userEmail, userPassword, signInBtn)
        }

        signupLink.setOnClickListener {
            startActivity(Intent(this, StudentSignupActivity::class.java))
        }
    }

    private fun authenticateStudent(email: String, password: String, signInBtn: Button) {
        Log.d("StudentLoginActivity", "Attempting login for: $email")

        apiHelper.login(email, password, "student", object : ApiHelper.ApiCallback {
            override fun onSuccess(response: String) {
                runOnUiThread {
                    try {
                        val jsonResponse = JSONObject(response)
                        if (jsonResponse.getBoolean("success")) {
                            val userData = jsonResponse.getJSONObject("data")
                            
                            // Save user session
                            apiHelper.saveUserSession(userData)
                            
                            Log.d("StudentLoginActivity", "Login successful for: ${userData.getString("email")}")
                            
                            Toast.makeText(this@StudentLoginActivity, 
                                "Welcome ${userData.getString("full_name")}!", 
                                Toast.LENGTH_SHORT).show()
                            
                            // Navigate to Student Dashboard
                            val intent = Intent(this@StudentLoginActivity, StudentDashboardActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            val errorMsg = jsonResponse.optString("message", "Login failed")
                            Toast.makeText(this@StudentLoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                            signInBtn.isEnabled = true
                        }
                    } catch (e: Exception) {
                        Log.e("StudentLoginActivity", "Error parsing response: ${e.message}")
                        Toast.makeText(this@StudentLoginActivity, 
                            "Login error: ${e.message}", 
                            Toast.LENGTH_LONG).show()
                        signInBtn.isEnabled = true
                    }
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    Log.e("StudentLoginActivity", "Login error: $error")
                    Toast.makeText(this@StudentLoginActivity, 
                        "Login failed: $error", 
                        Toast.LENGTH_LONG).show()
                    signInBtn.isEnabled = true
                }
            }
        })
    }
}
