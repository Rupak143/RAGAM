package com.example.ragamfinal;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ragamfinal.utils.ApiHelper;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class TeacherUploadVideosActivity extends AppCompatActivity {

    private String courseCategory, courseTitle, courseDescription;
    private int totalVideos, currentVideoIndex = 0;

    private TextView tvProgress, tvCurrentVideo;
    private ProgressBar progressBar;
    private LinearLayout videoFormContainer;
    private Button btnNext, btnFinish, btnSelectVideo;
    private EditText etVideoTitle, etVideoDescription;
    private TextView tvSelectedVideo;

    private List<VideoData> videoDataList = new ArrayList<>();
    private ApiHelper apiHelper;
    private Uri currentVideoUri;

    private static final int PICK_VIDEO_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_upload_videos);

        apiHelper = new ApiHelper(this);
        getCourseDataFromIntent();
        initViews();
        initializeVideoData();
        updateUI();
    }

    private void getCourseDataFromIntent() {
        courseCategory = getIntent().getStringExtra("course_category");
        courseTitle = getIntent().getStringExtra("course_title");
        courseDescription = getIntent().getStringExtra("course_description");
        totalVideos = getIntent().getIntExtra("video_count", 1);
    }

    private void initViews() {
        tvProgress = findViewById(R.id.tv_progress_text);
        progressBar = findViewById(R.id.upload_progress_bar);
        videoFormContainer = findViewById(R.id.videos_container);
        btnNext = findViewById(R.id.btn_next);
        btnFinish = findViewById(R.id.btn_finish_upload);

        progressBar.setMax(totalVideos);

        btnNext.setOnClickListener(v -> nextVideo());
        btnFinish.setOnClickListener(v -> finishCourse());
    }

    private void initializeVideoData() {
        for (int i = 0; i < totalVideos; i++) {
            videoDataList.add(new VideoData());
        }
    }

    private void updateUI() {
        // Update progress
        tvProgress.setText("Video " + (currentVideoIndex + 1) + " of " + totalVideos);
        progressBar.setProgress(currentVideoIndex + 1);

        // Clear container and add current video form
        videoFormContainer.removeAllViews();

        View videoForm = LayoutInflater.from(this).inflate(R.layout.item_video_form, videoFormContainer, false);

        tvCurrentVideo = videoForm.findViewById(R.id.tv_video_number);
        etVideoTitle = videoForm.findViewById(R.id.et_video_title);
        etVideoDescription = videoForm.findViewById(R.id.et_video_description);
        btnSelectVideo = videoForm.findViewById(R.id.btn_select_video);
        tvSelectedVideo = videoForm.findViewById(R.id.tv_selected_video);

        tvCurrentVideo.setText("Video " + (currentVideoIndex + 1));

        // Load existing data if any
        VideoData currentData = videoDataList.get(currentVideoIndex);
        etVideoTitle.setText(currentData.title);
        etVideoDescription.setText(currentData.description);

        if (currentData.videoUri != null) {
            tvSelectedVideo.setText("✓ Video selected");
            tvSelectedVideo.setVisibility(View.VISIBLE);
        } else {
            tvSelectedVideo.setVisibility(View.GONE);
        }

        btnSelectVideo.setOnClickListener(v -> selectVideo());

        videoFormContainer.addView(videoForm);

        // Update button visibility
        if (currentVideoIndex == totalVideos - 1) {
            btnNext.setVisibility(View.GONE);
            btnFinish.setVisibility(View.VISIBLE);
        } else {
            btnNext.setVisibility(View.VISIBLE);
            btnFinish.setVisibility(View.GONE);
        }
    }

    private void selectVideo() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent, PICK_VIDEO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK && data != null) {
            currentVideoUri = data.getData();
            videoDataList.get(currentVideoIndex).videoUri = currentVideoUri;
            tvSelectedVideo.setText("✓ Video selected");
            tvSelectedVideo.setVisibility(View.VISIBLE);
        }
    }

    private void nextVideo() {
        if (!validateCurrentVideo()) return;

        saveCurrentVideoData();
        currentVideoIndex++;
        updateUI();
    }

    private boolean validateCurrentVideo() {
        String title = etVideoTitle.getText().toString().trim();
        String description = etVideoDescription.getText().toString().trim();

        if (title.isEmpty()) {
            etVideoTitle.setError("Video title is required");
            return false;
        }

        if (description.isEmpty()) {
            etVideoDescription.setError("Video description is required");
            return false;
        }

        if (videoDataList.get(currentVideoIndex).videoUri == null) {
            Toast.makeText(this, "Please select a video", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveCurrentVideoData() {
        VideoData currentData = videoDataList.get(currentVideoIndex);
        currentData.title = etVideoTitle.getText().toString().trim();
        currentData.description = etVideoDescription.getText().toString().trim();
    }

    private void finishCourse() {
        if (!validateCurrentVideo()) return;

        saveCurrentVideoData();

        // Check if all videos are complete
        for (int i = 0; i < videoDataList.size(); i++) {
            VideoData data = videoDataList.get(i);
            if (data.title.isEmpty() || data.description.isEmpty() || data.videoUri == null) {
                Toast.makeText(this, "Please complete all videos", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        createMultiVideoCourse();
    }

    private void createMultiVideoCourse() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating course with " + totalVideos + " videos...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        JSONObject currentUser = apiHelper.getUserSession();
        if (currentUser == null) {
            progressDialog.dismiss();
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String teacherId = currentUser.getString("id");
            int instructorId = Integer.parseInt(teacherId);

            apiHelper.createMultiVideoCourse(instructorId, courseTitle, courseDescription, 
                courseCategory, totalVideos, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(TeacherUploadVideosActivity.this, 
                            "Multi-video course created successfully! " + totalVideos + " videos uploaded.", 
                            Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(TeacherUploadVideosActivity.this, TeacherMainContainerActivity.class);
                        intent.putExtra("fragment", "home");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(TeacherUploadVideosActivity.this, 
                            "Failed to create course: " + error, 
                            Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (JSONException e) {
            progressDialog.dismiss();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static class VideoData {
        String title = "";
        String description = "";
        Uri videoUri = null;
    }
}