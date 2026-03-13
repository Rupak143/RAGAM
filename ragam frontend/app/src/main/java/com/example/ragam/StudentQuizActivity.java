package com.example.ragamfinal;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.ragamfinal.config.AppConfig;
import org.json.JSONArray;
import org.json.JSONObject;

public class StudentQuizActivity extends AppCompatActivity {
    private TextView quizTitle, questionCounter, questionText;
    private RadioGroup optionsGroup;
    private RadioButton option1, option2, option3, option4;
    private Button submitButton, nextButton, previousButton;
    private int courseId;
    private String courseTitle;
    private int studentId;
    private JSONArray questions;
    private int currentQuestionIndex = 0;
    private int totalQuestions = 0;
    private int[] userAnswers;
    private boolean isNavigating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_quiz);
        initViews();
        loadIntentData();
        loadTestPaper();
    }

    private void initViews() {
        quizTitle = findViewById(R.id.quiz_title);
        questionCounter = findViewById(R.id.question_counter);
        questionText = findViewById(R.id.question_text);
        optionsGroup = findViewById(R.id.options_group);
        option1 = findViewById(R.id.option_1);
        option2 = findViewById(R.id.option_2);
        option3 = findViewById(R.id.option_3);
        option4 = findViewById(R.id.option_4);
        submitButton = findViewById(R.id.btn_submit);
        nextButton = findViewById(R.id.btn_next);
        previousButton = findViewById(R.id.btn_previous);
        
        submitButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        previousButton.setVisibility(View.GONE);
        
        submitButton.setOnClickListener(v -> submitQuiz());
        nextButton.setOnClickListener(v -> moveToNextQuestion());
        previousButton.setOnClickListener(v -> moveToPreviousQuestion());
        
        findViewById(R.id.back_button).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Exit Quiz?")
                .setMessage("Are you sure you want to exit? Your progress will be lost.")
                .setPositiveButton("Exit", (dialog, which) -> finish())
                .setNegativeButton("Cancel", null)
                .show();
        });
        
        // Set up radio group listener for single selection
        optionsGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1 && !isNavigating) {
                handleOptionSelected(checkedId);
            }
        });
    }

    private void loadIntentData() {
        Intent intent = getIntent();
        courseId = intent.getIntExtra("course_id", 0);
        courseTitle = intent.getStringExtra("course_title");
        studentId = intent.getIntExtra("student_id", 0);
        if (quizTitle != null && courseTitle != null) {
            quizTitle.setText(courseTitle + " - Quiz");
        }
    }

    private void loadTestPaper() {
        String url = AppConfig.getBaseUrl(this) + "courses.php?action=get_test_paper&course_id=" + courseId;
        StringRequest request = new StringRequest(Request.Method.GET, url,
            response -> {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    boolean success = jsonResponse.optBoolean("success", false);
                    if (success) {
                        questions = jsonResponse.getJSONArray("data");
                        totalQuestions = questions.length();
                        if (totalQuestions > 0) {
                            userAnswers = new int[totalQuestions];
                            for (int i = 0; i < totalQuestions; i++) {
                                userAnswers[i] = -1;
                            }
                            displayQuestion(0);
                        } else {
                            String message = jsonResponse.optString("message", "No questions available");
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        String errorMsg = jsonResponse.optString("message", "Failed to load quiz");
                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error loading quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                }
            },
            error -> {
                Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                finish();
            }
        );
        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void displayQuestion(int index) {
        try {
            isNavigating = true;
            currentQuestionIndex = index;
            JSONObject question = questions.getJSONObject(index);
            
            questionCounter.setText("Question " + (index + 1) + " of " + totalQuestions);
            questionText.setText(question.getString("question_text"));
            option1.setText(question.getString("option_a"));
            option2.setText(question.getString("option_b"));
            option3.setText(question.getString("option_c"));
            option4.setText(question.getString("option_d"));
            
            // Clear selection first
            optionsGroup.clearCheck();
            
            // Restore previous selection if exists
            if (userAnswers[index] != -1) {
                int selectedOption = userAnswers[index];
                switch (selectedOption) {
                    case 1: option1.setChecked(true); break;
                    case 2: option2.setChecked(true); break;
                    case 3: option3.setChecked(true); break;
                    case 4: option4.setChecked(true); break;
                }
            }
            
            // Reset all option colors
            resetOptionColors();
            
            // Update navigation buttons visibility
            updateNavigationButtons();
            
            isNavigating = false;
            
        } catch (Exception e) {
            Toast.makeText(this, "Error displaying question: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleOptionSelected(int checkedId) {
        int selectedOption = getSelectedOptionNumber(checkedId);
        if (selectedOption != -1) {
            userAnswers[currentQuestionIndex] = selectedOption;
            highlightSelectedOption(checkedId);
            
            // Auto-advance to next question after 800ms
            new Handler().postDelayed(() -> {
                if (currentQuestionIndex < totalQuestions - 1) {
                    moveToNextQuestion();
                } else {
                    // Last question - show submit button
                    updateNavigationButtons();
                }
            }, 800);
        }
    }

    private void moveToNextQuestion() {
        if (currentQuestionIndex < totalQuestions - 1) {
            displayQuestion(currentQuestionIndex + 1);
        }
    }

    private void moveToPreviousQuestion() {
        if (currentQuestionIndex > 0) {
            displayQuestion(currentQuestionIndex - 1);
        }
    }

    private void updateNavigationButtons() {
        // Previous button - show if not on first question
        if (currentQuestionIndex > 0) {
            previousButton.setVisibility(View.VISIBLE);
        } else {
            previousButton.setVisibility(View.GONE);
        }
        
        // Check if current question is answered
        boolean currentAnswered = userAnswers[currentQuestionIndex] != -1;
        
        // Next button - show if not on last question and current is answered
        if (currentQuestionIndex < totalQuestions - 1) {
            nextButton.setVisibility(currentAnswered ? View.VISIBLE : View.GONE);
            submitButton.setVisibility(View.GONE);
        } else {
            // Last question - show submit if answered
            nextButton.setVisibility(View.GONE);
            submitButton.setVisibility(currentAnswered ? View.VISIBLE : View.GONE);
        }
    }

    private int getSelectedOptionNumber(int checkedId) {
        if (checkedId == R.id.option_1) return 1;
        if (checkedId == R.id.option_2) return 2;
        if (checkedId == R.id.option_3) return 3;
        if (checkedId == R.id.option_4) return 4;
        return -1;
    }

    private void resetOptionColors() {
        int defaultColor = ContextCompat.getColor(this, R.color.textPrimary);
        option1.setTextColor(defaultColor);
        option2.setTextColor(defaultColor);
        option3.setTextColor(defaultColor);
        option4.setTextColor(defaultColor);
    }

    private void highlightSelectedOption(int checkedId) {
        resetOptionColors();
        int highlightColor = ContextCompat.getColor(this, R.color.primary);
        if (checkedId == R.id.option_1) option1.setTextColor(highlightColor);
        else if (checkedId == R.id.option_2) option2.setTextColor(highlightColor);
        else if (checkedId == R.id.option_3) option3.setTextColor(highlightColor);
        else if (checkedId == R.id.option_4) option4.setTextColor(highlightColor);
    }

    private void submitQuiz() {
        // Check if all questions are answered
        for (int i = 0; i < totalQuestions; i++) {
            if (userAnswers[i] == -1) {
                Toast.makeText(this, "Please answer all questions before submitting", Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        // Calculate score
        int correctAnswers = 0;
        try {
            for (int i = 0; i < totalQuestions; i++) {
                JSONObject question = questions.getJSONObject(i);
                String correctAnswerStr = question.getString("correct_answer");
                
                // Convert A,B,C,D to 1,2,3,4
                int correctAnswer = 1;
                switch (correctAnswerStr.toUpperCase()) {
                    case "A": correctAnswer = 1; break;
                    case "B": correctAnswer = 2; break;
                    case "C": correctAnswer = 3; break;
                    case "D": correctAnswer = 4; break;
                }
                
                int userAnswer = userAnswers[i];
                
                if (userAnswer == correctAnswer) {
                    correctAnswers++;
                }
            }
            
            // Calculate percentage
            int percentage = (int) Math.round((correctAnswers * 100.0) / totalQuestions);
            boolean passed = percentage >= 50;
            
            // Launch result activity
            Intent intent = new Intent(this, QuizResultActivity.class);
            intent.putExtra("student_id", studentId);
            intent.putExtra("course_id", courseId);
            intent.putExtra("course_title", courseTitle);
            intent.putExtra("total_questions", totalQuestions);
            intent.putExtra("correct_answers", correctAnswers);
            intent.putExtra("percentage", percentage);
            intent.putExtra("passed", passed);
            startActivity(intent);
            finish();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error submitting quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
            .setTitle("Exit Quiz?")
            .setMessage("Are you sure you want to exit? Your progress will be lost.")
            .setPositiveButton("Exit", (dialog, which) -> super.onBackPressed())
            .setNegativeButton("Cancel", null)
            .show();
    }
}