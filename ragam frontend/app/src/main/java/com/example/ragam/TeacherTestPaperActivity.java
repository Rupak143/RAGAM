package com.example.ragamfinal;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ragamfinal.config.AppConfig;
import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TeacherTestPaperActivity extends AppCompatActivity {
    private static final String TAG = "TeacherTestPaper";
    private static final int VIDEO_PICK_REQUEST_BASE = 2000;
    
    private ImageView backButton;
    private TextView courseTitle;
    private LinearLayout questionsContainer;
    private Button addQuestionButton, saveTestPaperButton;
    private ApiHelper apiHelper;
    
    private String courseTitleText, courseDescription, courseCategory, instructorId;
    private int videoCount;
    private String[] videoTitles, videoDescriptions;
    private Uri[] videoUris; // ADDED: Store actual video file URIs
    private List<QuestionData> questions;
    private ProgressDialog progressDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_test_paper);
        
        apiHelper = new ApiHelper(this);
        questions = new ArrayList<>();
        
        // Get course data from intent
        courseTitleText = getIntent().getStringExtra("course_title");
        courseDescription = getIntent().getStringExtra("course_description");
        courseCategory = getIntent().getStringExtra("course_category");
        instructorId = getIntent().getStringExtra("instructor_id");
        videoCount = getIntent().getIntExtra("video_count", 0);
        videoTitles = getIntent().getStringArrayExtra("video_titles");
        videoDescriptions = getIntent().getStringArrayExtra("video_descriptions");

        if (instructorId == null || instructorId.trim().isEmpty()) {
            JSONObject sessionUser = apiHelper.getUserSession();
            if (sessionUser != null) {
                instructorId = sessionUser.optString("user_id", "");
            }
        }

        Log.d(TAG, "Resolved instructor_id: " + instructorId);
        
        // Initialize video URIs array
        videoUris = new Uri[videoCount];
        
        initViews();
        setupClickListeners();
        loadCourseInfo();
    }
    
    private void initViews() {
        backButton = findViewById(R.id.back_button);
        courseTitle = findViewById(R.id.course_title);
        questionsContainer = findViewById(R.id.questions_container);
        addQuestionButton = findViewById(R.id.btn_add_question);
        saveTestPaperButton = findViewById(R.id.btn_save_test_paper);
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        addQuestionButton.setOnClickListener(v -> addQuestionCard());
        saveTestPaperButton.setOnClickListener(v -> saveTestPaper());
    }
    
    private void loadCourseInfo() {
        courseTitle.setText("Test Paper for: " + courseTitleText);
        
        // Show video selection requirement
        Toast.makeText(this, "⚠️ Please select video files for all " + videoCount + " videos before saving", Toast.LENGTH_LONG).show();
        
        // Add video selection UI before questions
        addVideoSelectionUI();
        
        // Add first question by default
        addQuestionCard();
    }

    private void addVideoSelectionUI() {
        if (videoTitles == null || videoTitles.length == 0) return;

        // Add a header for video selection
        TextView videoHeader = new TextView(this);
        videoHeader.setText("📹 Select Video Files");
        videoHeader.setTextSize(18);
        videoHeader.setPadding(20, 20, 20, 10);
        videoHeader.setTextColor(0xFF2196F3);
        questionsContainer.addView(videoHeader);

        // Add selection button for each video
        for (int i = 0; i < videoTitles.length; i++) {
            final int videoIndex = i;

            Button btnSelectVideo = new Button(this);
            btnSelectVideo.setId(View.generateViewId());
            btnSelectVideo.setText("📁 Select Video " + (i + 1) + ": " + videoTitles[i]);
            btnSelectVideo.setPadding(20, 20, 20, 20);
            btnSelectVideo.setOnClickListener(v -> openVideoFilePicker(videoIndex, btnSelectVideo));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(20, 10, 20, 10);
            btnSelectVideo.setLayoutParams(params);

            questionsContainer.addView(btnSelectVideo);
        }

        // Add divider before questions
        View divider = new View(this);
        divider.setBackgroundColor(0xFFCCCCCC);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            3
        );
        dividerParams.setMargins(20, 30, 20, 30);
        divider.setLayoutParams(dividerParams);
        questionsContainer.addView(divider);

        // Add questions header
        TextView questionsHeader = new TextView(this);
        questionsHeader.setText("❓ Test Questions");
        questionsHeader.setTextSize(18);
        questionsHeader.setPadding(20, 20, 20, 10);
        questionsHeader.setTextColor(0xFF2196F3);
        questionsContainer.addView(questionsHeader);
    }
    
    private void openVideoFilePicker(final int videoIndex, final Button btnSelectVideo) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(
                Intent.createChooser(intent, "Select Video " + (videoIndex + 1)), 
                VIDEO_PICK_REQUEST_BASE + videoIndex
            );
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "❌ No file manager found. Please install one.", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode >= VIDEO_PICK_REQUEST_BASE && 
            requestCode < VIDEO_PICK_REQUEST_BASE + videoCount &&
            resultCode == RESULT_OK && 
            data != null) {
            
            int videoIndex = requestCode - VIDEO_PICK_REQUEST_BASE;
            Uri videoUri = data.getData();
            
            if (videoUri != null) {
                videoUris[videoIndex] = videoUri;
                
                String fileName = getFileName(videoUri);
                Toast.makeText(this, "✅ Video " + (videoIndex + 1) + " selected: " + fileName, Toast.LENGTH_SHORT).show();
                
                // Update button text to show file selected
                // Button is at index videoIndex + 1 (after header at index 0)
                try {
                    View childView = questionsContainer.getChildAt(videoIndex + 1);
                    if (childView instanceof Button) {
                        Button btnSelectVideo = (Button) childView;
                        btnSelectVideo.setText("✅ " + fileName);
                        btnSelectVideo.setBackgroundColor(0xFF4CAF50);
                        btnSelectVideo.setTextColor(0xFFFFFFFF);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating button UI", e);
                }
            }
        }
    }
    
    private String getFileName(Uri uri) {
        String fileName = uri.getLastPathSegment();
        if (fileName != null && fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }
        return fileName != null ? fileName : "video.mp4";
    }
    
    private void addQuestionCard() {
        View questionCard = getLayoutInflater().inflate(R.layout.item_question_card, questionsContainer, false);
        
        TextView questionNumber = questionCard.findViewById(R.id.question_number);
        EditText questionText = questionCard.findViewById(R.id.question_text);
        EditText option1 = questionCard.findViewById(R.id.option_1);
        EditText option2 = questionCard.findViewById(R.id.option_2);
        EditText option3 = questionCard.findViewById(R.id.option_3);
        EditText option4 = questionCard.findViewById(R.id.option_4);
        EditText correctAnswer = questionCard.findViewById(R.id.correct_answer);
        Button removeButton = questionCard.findViewById(R.id.btn_remove_question);
        
        int questionNum = questions.size() + 1;
        questionNumber.setText("Question " + questionNum);
        
        QuestionData questionData = new QuestionData();
        questionData.questionCard = questionCard;
        questionData.questionText = questionText;
        questionData.option1 = option1;
        questionData.option2 = option2;
        questionData.option3 = option3;
        questionData.option4 = option4;
        questionData.correctAnswer = correctAnswer;
        
        questions.add(questionData);
        
        removeButton.setOnClickListener(v -> {
            questions.remove(questionData);
            questionsContainer.removeView(questionCard);
            updateQuestionNumbers();
        });
        
        questionsContainer.addView(questionCard);
        updateSaveButtonVisibility();
    }
    
    private void updateQuestionNumbers() {
        for (int i = 0; i < questions.size(); i++) {
            TextView questionNumber = questions.get(i).questionCard.findViewById(R.id.question_number);
            questionNumber.setText("Question " + (i + 1));
        }
    }
    
    private void updateSaveButtonVisibility() {
        saveTestPaperButton.setVisibility(questions.size() > 0 ? View.VISIBLE : View.GONE);
    }
    
    private void saveTestPaper() {
        if (!validateQuestions()) {
            return;
        }
        
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving test paper...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        try {
            JSONObject testPaperData = new JSONObject();
            testPaperData.put("course_title", courseTitleText);
            testPaperData.put("course_description", courseDescription);
            testPaperData.put("course_category", courseCategory);
            
            JSONArray questionsArray = new JSONArray();
            for (int i = 0; i < questions.size(); i++) {
                QuestionData q = questions.get(i);
                JSONObject questionObj = new JSONObject();
                questionObj.put("question_number", i + 1);
                questionObj.put("question_text", q.questionText.getText().toString().trim());
                questionObj.put("option_1", q.option1.getText().toString().trim());
                questionObj.put("option_2", q.option2.getText().toString().trim());
                questionObj.put("option_3", q.option3.getText().toString().trim());
                questionObj.put("option_4", q.option4.getText().toString().trim());
                questionObj.put("correct_answer", q.correctAnswer.getText().toString().trim());
                questionsArray.put(questionObj);
            }
            testPaperData.put("questions", questionsArray);
            
            // For now, simulate saving and return to course creation
            progressDialog.dismiss();
            showSuccessDialog();
            
        } catch (JSONException e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error creating test paper data", e);
            Toast.makeText(this, "Error saving test paper", Toast.LENGTH_SHORT).show();
        }
    }
    
    private boolean validateQuestions() {
        if (questions.isEmpty()) {
            Toast.makeText(this, "Please add at least one question", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        for (int i = 0; i < questions.size(); i++) {
            QuestionData q = questions.get(i);
            
            if (q.questionText.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "Question " + (i + 1) + " text is required", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            if (q.option1.getText().toString().trim().isEmpty() ||
                q.option2.getText().toString().trim().isEmpty() ||
                q.option3.getText().toString().trim().isEmpty() ||
                q.option4.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "All options for Question " + (i + 1) + " are required", Toast.LENGTH_SHORT).show();
                return false;
            }

            String opt1 = q.option1.getText().toString().trim();
            String opt2 = q.option2.getText().toString().trim();
            String opt3 = q.option3.getText().toString().trim();
            String opt4 = q.option4.getText().toString().trim();

            EditText[][] optionPairs = {
                {q.option1, q.option2}, {q.option1, q.option3}, {q.option1, q.option4},
                {q.option2, q.option3}, {q.option2, q.option4}, {q.option3, q.option4}
            };
            String[][] valuePairs = {
                {opt1, opt2}, {opt1, opt3}, {opt1, opt4},
                {opt2, opt3}, {opt2, opt4}, {opt3, opt4}
            };

            for (int p = 0; p < valuePairs.length; p++) {
                if (valuePairs[p][0].equalsIgnoreCase(valuePairs[p][1])) {
                    optionPairs[p][0].setError("Duplicate option");
                    optionPairs[p][1].setError("Duplicate option");
                    Toast.makeText(this, "Question " + (i + 1) + ": Options must not be identical", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            String correctAns = q.correctAnswer.getText().toString().trim();
            if (correctAns.isEmpty()) {
                Toast.makeText(this, "Correct answer for Question " + (i + 1) + " is required", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            // Validate correct answer is one of the options
            if (!correctAns.equals("1") && !correctAns.equals("2") && !correctAns.equals("3") && !correctAns.equals("4")) {
                Toast.makeText(this, "Correct answer for Question " + (i + 1) + " must be 1, 2, 3, or 4", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        
        return true;
    }
    
    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
            .setTitle("🎉 Test Paper Created!")
            .setMessage("Your test paper has been prepared with " + questions.size() + " questions. Ready to create the course?")
            .setPositiveButton("Create Course", (dialog, which) -> {
                createCourseWithTestPaper();
            })
            .setNegativeButton("Add More Questions", null)
            .setCancelable(false)
            .show();
    }
    
    private void createCourseWithTestPaper() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating course with test paper...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        try {
            // Step 1: Create course first
            apiHelper.createCourse(Integer.parseInt(instructorId), courseTitleText, courseDescription, courseCategory, videoCount, new ApiHelper.JSONApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    boolean success = response.optBoolean("success", false);
                    if (success) {
                        // Get course ID from response
                        JSONObject courseData = response.optJSONObject("data");
                        int courseId = courseData.optInt("course_id", 0);
                        
                        if (courseId > 0) {
                            // Step 2: Save videos
                            saveVideosForCourse(courseId, progressDialog);
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(TeacherTestPaperActivity.this, "Course created but no ID returned", Toast.LENGTH_LONG).show();
                            showFinalSuccessDialog();
                        }
                    } else {
                        progressDialog.dismiss();
                        String message = response.optString("message", "Failed to create course");
                        Toast.makeText(TeacherTestPaperActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
                    }
                }
                
                @Override
                public void onError(String error) {
                    progressDialog.dismiss();
                    Toast.makeText(TeacherTestPaperActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
            
        } catch (Exception e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error preparing course data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveVideosForCourse(int courseId, ProgressDialog progressDialog) {
        // Validate all videos selected
        for (int i = 0; i < videoCount; i++) {
            if (videoUris[i] == null) {
                progressDialog.dismiss();
                Toast.makeText(this, "⚠️ Please select video file for Video " + (i + 1) + ": " + videoTitles[i], Toast.LENGTH_LONG).show();
                return;
            }
        }
        
        progressDialog.setMessage("Uploading videos...");
        Log.d(TAG, "Starting video upload for course " + courseId);
        
        // Upload videos one by one recursively
        uploadVideoRecursive(courseId, 0, progressDialog);
    }
    
    private void uploadVideoRecursive(final int courseId, final int videoIndex, final ProgressDialog progressDialog) {
        // Base case: all videos uploaded
        if (videoIndex >= videoCount) {
            Log.d(TAG, "All videos uploaded successfully. Now saving test paper...");
            saveTestPaperForCourse(courseId, progressDialog);
            return;
        }
        
        progressDialog.setMessage("Uploading video " + (videoIndex + 1) + " of " + videoCount + "...");
        
        final Uri videoUri = videoUris[videoIndex];
        final String videoTitle = videoTitles[videoIndex];
        final String videoDescription = videoDescriptions[videoIndex];
        
        Log.d(TAG, "Uploading video " + (videoIndex + 1) + ": " + videoTitle);
        
        // Step 1: Upload the actual file to server
        apiHelper.uploadVideoFile(videoUri.toString(), new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(String uploadResponse) {
                try {
                    Log.d(TAG, "Video " + (videoIndex + 1) + " upload response: " + uploadResponse);
                    
                    JSONObject uploadResult = new JSONObject(uploadResponse);
                    boolean success = uploadResult.optString("status", "").equals("success") || 
                                     uploadResult.optBoolean("success", false);
                    
                    if (success) {
                        // Get the relative path returned from server
                        JSONObject data = uploadResult.optJSONObject("data");
                        String relativePath = data != null ? 
                            data.getString("video_url") : 
                            uploadResult.optString("video_url", "");
                        
                        // FIXED: Remove "ragamfinal/" prefix if present to avoid double path
                        if (relativePath.startsWith("ragamfinal/")) {
                            relativePath = relativePath.substring("ragamfinal/".length());
                            Log.d(TAG, "Removed 'ragamfinal/' prefix from path");
                        }
                        
                        Log.d(TAG, "Video " + (videoIndex + 1) + " uploaded to: " + relativePath);
                        
                        // Step 2: Save video metadata to database
                        JSONObject videoData = new JSONObject();
                        videoData.put("course_id", courseId);
                        videoData.put("video_title", videoTitle);
                        videoData.put("video_description", videoDescription);
                        videoData.put("video_url", relativePath);
                        
                        apiHelper.makeJsonPostRequest(
                            AppConfig.BASE_URL + "courses.php?action=add_lesson", 
                            videoData, 
                            new ApiHelper.JSONApiCallback() {
                                @Override
                                public void onSuccess(JSONObject response) {
                                    Log.d(TAG, "Video " + (videoIndex + 1) + " metadata saved to database");
                                    // Upload next video
                                    uploadVideoRecursive(courseId, videoIndex + 1, progressDialog);
                                }
                                
                                @Override
                                public void onError(String error) {
                                    Log.e(TAG, "Error saving video " + (videoIndex + 1) + " metadata: " + error);
                                    progressDialog.dismiss();
                                    showErrorDialog("Failed to save video " + (videoIndex + 1) + " details: " + error);
                                }
                            }
                        );
                        
                    } else {
                        throw new Exception("Upload response indicated failure");
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error processing upload response for video " + (videoIndex + 1), e);
                    progressDialog.dismiss();
                    showErrorDialog("Failed to process video " + (videoIndex + 1) + " upload: " + e.getMessage());
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error uploading video " + (videoIndex + 1) + " file: " + error);
                progressDialog.dismiss();
                showErrorDialog("Failed to upload video " + (videoIndex + 1) + " file:\n\n" + error + 
                              "\n\nPlease check:\n" +
                              "• Video file is valid MP4/AVI/MOV\n" +
                              "• File size is under 100MB\n" +
                              "• Internet connection is active\n" +
                              "• Server is running");
            }
        });
    }
    
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
            .setTitle("❌ Upload Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }
    
    private void saveTestPaperForCourse(int courseId, ProgressDialog progressDialog) {
        progressDialog.setMessage("Saving test paper...");
        
        try {
            JSONObject testPaperData = new JSONObject();
            testPaperData.put("course_id", courseId);
            testPaperData.put("course_title", courseTitleText);
            testPaperData.put("course_category", courseCategory);
            
            JSONArray questionsArray = new JSONArray();
            for (int i = 0; i < questions.size(); i++) {
                QuestionData q = questions.get(i);
                JSONObject questionObj = new JSONObject();
                questionObj.put("question_number", i + 1);
                questionObj.put("question_text", q.questionText.getText().toString().trim());
                questionObj.put("option_a", q.option1.getText().toString().trim());
                questionObj.put("option_b", q.option2.getText().toString().trim());
                questionObj.put("option_c", q.option3.getText().toString().trim());
                questionObj.put("option_d", q.option4.getText().toString().trim());
                questionObj.put("correct_answer", q.correctAnswer.getText().toString().trim());
                questionsArray.put(questionObj);
            }
            testPaperData.put("questions", questionsArray);
            
            apiHelper.makeJsonPostRequest(AppConfig.BASE_URL + "courses.php?action=save_test_paper", testPaperData, new ApiHelper.JSONApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    progressDialog.dismiss();
                    Log.d(TAG, "Test paper saved successfully");
                    showFinalSuccessDialog();
                }
                
                @Override
                public void onError(String error) {
                    progressDialog.dismiss();
                    Log.e(TAG, "Error saving test paper: " + error);
                    Toast.makeText(TeacherTestPaperActivity.this, "Course created but test paper save failed", Toast.LENGTH_LONG).show();
                    showFinalSuccessDialog();
                }
            });
            
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error preparing test paper data", e);
            Toast.makeText(this, "Error saving test paper: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void showFinalSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle("🎉 Success!")
                .setMessage("Course created successfully with test paper!")
                .setPositiveButton("View My Courses", (dialog, which) -> {
                    Intent intent = new Intent(TeacherTestPaperActivity.this, TeacherMyCoursesActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Create Another", (dialog, which) -> {
                    Intent intent = new Intent(TeacherTestPaperActivity.this, TeacherCourseSetupActivity.class);
                    intent.putExtra("instructor_id", instructorId);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }
    
    private static class QuestionData {
        View questionCard;
        EditText questionText;
        EditText option1, option2, option3, option4;
        EditText correctAnswer;
    }
}