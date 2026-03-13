package com.example.ragamfinal;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.ragamfinal.config.AppConfig;

import org.json.JSONObject;

/**
 * Quiz Result Activity
 * - Shows final score and percentage
 * - Displays pass/fail status
 * - Shows certificate if score >= 50%
 * - Allows retaking quiz if failed
 * - Saves certificate to backend
 */
public class QuizResultActivity extends AppCompatActivity {
    
    private TextView resultTitle, scoreText, percentageText, statusText, feedbackText;
    private Button certificateButton, retakeButton, doneButton;
    
    private int studentId, courseId, totalQuestions, correctAnswers, percentage;
    private String courseTitle;
    private boolean passed;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_result);
        
        loadIntentData();
        initViews();
        displayResults();
        
        if (passed) {
            saveCertificate();
        }
    }
    
    private void loadIntentData() {
        studentId = getIntent().getIntExtra("student_id", 0);
        courseId = getIntent().getIntExtra("course_id", 0);
        courseTitle = getIntent().getStringExtra("course_title");
        totalQuestions = getIntent().getIntExtra("total_questions", 0);
        correctAnswers = getIntent().getIntExtra("correct_answers", 0);
        percentage = getIntent().getIntExtra("percentage", 0);
        passed = getIntent().getBooleanExtra("passed", false);
    }
    
    private void initViews() {
        resultTitle = findViewById(R.id.result_title);
        scoreText = findViewById(R.id.score_text);
        percentageText = findViewById(R.id.percentage_text);
        statusText = findViewById(R.id.status_text);
        feedbackText = findViewById(R.id.feedback_text);
        certificateButton = findViewById(R.id.btn_view_certificate);
        retakeButton = findViewById(R.id.btn_retake_quiz);
        doneButton = findViewById(R.id.btn_done);
        
        certificateButton.setOnClickListener(v -> showCertificate());
        retakeButton.setOnClickListener(v -> retakeQuiz());
        doneButton.setOnClickListener(v -> finish());
    }
    
    private void displayResults() {
        scoreText.setText(correctAnswers + " / " + totalQuestions);
        percentageText.setText(percentage + "%");
        
        if (passed) {
            resultTitle.setText("🎉 Congratulations!");
            statusText.setText("PASSED");
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            feedbackText.setText("Excellent work! You've passed the quiz and earned a certificate.");
            
            certificateButton.setVisibility(View.VISIBLE);
            retakeButton.setVisibility(View.GONE);
        } else {
            resultTitle.setText("Keep Trying!");
            statusText.setText("NOT PASSED");
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            feedbackText.setText("You need 50% or more to pass and earn a certificate. Review the course materials and try again!");
            
            certificateButton.setVisibility(View.GONE);
            retakeButton.setVisibility(View.VISIBLE);
        }
    }
    
    private void saveCertificate() {
        String url = AppConfig.getBaseUrl(this) + "courses.php?action=save_certificate";
        
        try {
            JSONObject requestData = new JSONObject();
            requestData.put("student_id", studentId);
            requestData.put("course_id", courseId);
            requestData.put("course_title", courseTitle);
            requestData.put("score", percentage);
            requestData.put("total_questions", totalQuestions);
            requestData.put("correct_answers", correctAnswers);
            
            JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.POST, url, requestData,
                response -> {
                    android.util.Log.d("QuizResult", "Certificate saved: " + response.toString());
                },
                error -> {
                    android.util.Log.e("QuizResult", "Error saving certificate: " + error.toString());
                }
            );
            
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(jsonRequest);
            
        } catch (Exception e) {
            android.util.Log.e("QuizResult", "Error creating certificate request", e);
        }
    }
    
    private void showCertificate() {
        LayoutInflater inflater = getLayoutInflater();
        View certificateView = inflater.inflate(R.layout.dialog_certificate, null);
        
        TextView certStudentName = certificateView.findViewById(R.id.cert_student_name);
        TextView certCourseName = certificateView.findViewById(R.id.cert_course_name);
        TextView certScore = certificateView.findViewById(R.id.cert_score);
        TextView certDate = certificateView.findViewById(R.id.cert_date);
        
        try {
            com.example.ragamfinal.utils.ApiHelper apiHelper = new com.example.ragamfinal.utils.ApiHelper(this);
            JSONObject currentUser = apiHelper.getUserSession();
            String studentName = currentUser != null ? 
                currentUser.optString("full_name", currentUser.optString("username", "Student")) : 
                "Student";
            
            certStudentName.setText(studentName);
            certCourseName.setText(courseTitle);
            certScore.setText(percentage + "%");
            
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.US);
            String currentDate = dateFormat.format(new java.util.Date());
            certDate.setText(currentDate);
            
        } catch (Exception e) {
            android.util.Log.e("QuizResult", "Error loading certificate data", e);
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(certificateView);
        builder.setPositiveButton("Close", null);
        builder.show();
    }
    
    private void retakeQuiz() {
        new AlertDialog.Builder(this)
            .setTitle("Retake Quiz?")
            .setMessage("Are you ready to retake the quiz? Make sure to review the course materials first.")
            .setPositiveButton("Start Quiz", (dialog, which) -> {
                Intent quizIntent = new Intent(this, StudentQuizActivity.class);
                quizIntent.putExtra("course_id", courseId);
                quizIntent.putExtra("course_title", courseTitle);
                startActivity(quizIntent);
                finish();
            })
            .setNegativeButton("Not Yet", null)
            .show();
    }
    
    @Override
    public void onBackPressed() {
        finish();
    }
}
