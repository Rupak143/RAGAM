package com.example.ragamfinal;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.ragamfinal.config.AppConfig;
import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CertificatesActivity extends AppCompatActivity {
    
    private static final String TAG = "CertificatesActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private LinearLayout certificatesContainer;
    private TextView noCertificatesText;
    private ApiHelper apiHelper;
    private ImageView backButton;
    private View pendingCertificateView = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificates);
        
        apiHelper = new ApiHelper(this);
        certificatesContainer = findViewById(R.id.certificates_container);
        noCertificatesText = findViewById(R.id.no_certificates_text);
        backButton = findViewById(R.id.back_button);
        
        backButton.setOnClickListener(v -> finish());
        
        loadCertificates();
    }
    
    private void loadCertificates() {
        try {
            JSONObject user = apiHelper.getUserSession();
            if (user == null) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            
            int studentId = user.getInt("user_id");
            String url = AppConfig.getBaseUrl(this) + "courses.php?action=get_certificates&student_id=" + studentId;
            
            Log.d(TAG, "Loading certificates from: " + url);
            
            StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        Log.d(TAG, "Certificates response: " + response);
                        JSONObject jsonResponse = new JSONObject(response);
                        
                        if (jsonResponse.getBoolean("success")) {
                            JSONArray certificates = jsonResponse.getJSONArray("data");
                            
                            if (certificates.length() == 0) {
                                noCertificatesText.setVisibility(View.VISIBLE);
                                certificatesContainer.setVisibility(View.GONE);
                            } else {
                                noCertificatesText.setVisibility(View.GONE);
                                certificatesContainer.setVisibility(View.VISIBLE);
                                displayCertificates(certificates);
                            }
                        } else {
                            Toast.makeText(this, "Error loading certificates", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing certificates", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e(TAG, "Error loading certificates", error);
                    Toast.makeText(this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                }
            );
            
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in loadCertificates", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void displayCertificates(JSONArray certificates) {
        certificatesContainer.removeAllViews();
        
        try {
            JSONObject user = apiHelper.getUserSession();
            String studentName = user.optString("full_name", user.optString("username", "Student"));
            
            for (int i = 0; i < certificates.length(); i++) {
                JSONObject cert = certificates.getJSONObject(i);
                addCertificateItem(cert, studentName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error displaying certificates", e);
        }
    }
    
    private void addCertificateItem(JSONObject cert, String studentName) {
        try {
            // Create certificate card
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(Color.WHITE);
            card.setPadding(24, 24, 24, 24);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 16);
            card.setLayoutParams(cardParams);
            card.setElevation(4f);
            
            // Trophy icon
            TextView trophy = new TextView(this);
            trophy.setText("🏆");
            trophy.setTextSize(32);
            trophy.setGravity(android.view.Gravity.CENTER);
            card.addView(trophy);
            
            // Course title
            TextView courseTitle = new TextView(this);
            courseTitle.setText(cert.getString("course_title"));
            courseTitle.setTextSize(18);
            courseTitle.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
            courseTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            courseTitle.setPadding(0, 8, 0, 8);
            card.addView(courseTitle);
            
            // Score
            TextView score = new TextView(this);
            score.setText("Score: " + cert.getInt("score") + "%");
            score.setTextSize(16);
            score.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            score.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            score.setPadding(0, 4, 0, 4);
            card.addView(score);
            
            // Date
            TextView date = new TextView(this);
            String issueDate = cert.getString("issue_date");
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
                Date parsedDate = inputFormat.parse(issueDate);
                String formattedDate = outputFormat.format(parsedDate);
                date.setText("Earned on " + formattedDate);
            } catch (Exception e) {
                date.setText("Earned on " + issueDate);
            }
            date.setTextSize(14);
            date.setTextColor(Color.GRAY);
            date.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            date.setPadding(0, 4, 0, 16);
            card.addView(date);
            
            // View Certificate button
            TextView viewButton = new TextView(this);
            viewButton.setText("View Certificate");
            viewButton.setTextSize(16);
            viewButton.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
            viewButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            viewButton.setPadding(0, 12, 0, 0);
            viewButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            card.addView(viewButton);
            
            // Click to view full certificate
            card.setOnClickListener(v -> showCertificateDialog(cert, studentName));
            
            certificatesContainer.addView(card);
            
        } catch (Exception e) {
            Log.e(TAG, "Error adding certificate item", e);
        }
    }
    
    private void showCertificateDialog(JSONObject cert, String studentName) {
        try {
            LayoutInflater inflater = getLayoutInflater();
            View certificateView = inflater.inflate(R.layout.dialog_certificate, null);
            
            // Set certificate details
            TextView certStudentName = certificateView.findViewById(R.id.cert_student_name);
            TextView certCourseName = certificateView.findViewById(R.id.cert_course_name);
            TextView certScore = certificateView.findViewById(R.id.cert_score);
            TextView certDate = certificateView.findViewById(R.id.cert_date);
            
            certStudentName.setText(studentName);
            certCourseName.setText(cert.getString("course_title"));
            certScore.setText(cert.getInt("score") + "%");
            
            String issueDate = cert.getString("issue_date");
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.US);
                Date parsedDate = inputFormat.parse(issueDate);
                String formattedDate = outputFormat.format(parsedDate);
                certDate.setText(formattedDate);
            } catch (Exception e) {
                certDate.setText(issueDate);
            }
            
            // Show certificate dialog with download button
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setView(certificateView);
            builder.setPositiveButton("Download", (dialog, which) -> {
                downloadCertificate(certificateView, cert);
            });
            builder.setNegativeButton("Close", (dialog, which) -> dialog.dismiss());
            builder.show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing certificate dialog", e);
            Toast.makeText(this, "Error displaying certificate", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void downloadCertificate(View certificateView, JSONObject cert) {
        // Check for write permission on Android versions below 10
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                pendingCertificateView = certificateView;
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
                return;
            }
        }
        
        // Permission granted or not needed, proceed with download
        saveCertificateAsImage(certificateView, cert);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingCertificateView != null) {
                    try {
                        // Get the certificate data from the view
                        TextView courseName = pendingCertificateView.findViewById(R.id.cert_course_name);
                        JSONObject cert = new JSONObject();
                        cert.put("course_title", courseName.getText().toString());
                        saveCertificateAsImage(pendingCertificateView, cert);
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving certificate after permission grant", e);
                        Toast.makeText(this, "Error saving certificate", Toast.LENGTH_SHORT).show();
                    }
                    pendingCertificateView = null;
                }
            } else {
                Toast.makeText(this, "Permission denied. Cannot save certificate.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void saveCertificateAsImage(View certificateView, JSONObject cert) {
        try {
            // Create bitmap from view
            certificateView.setDrawingCacheEnabled(true);
            certificateView.buildDrawingCache();
            Bitmap bitmap = Bitmap.createBitmap(certificateView.getWidth(),
                    certificateView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            certificateView.draw(canvas);
            
            // Generate filename
            String courseName = cert.getString("course_title").replaceAll("[^a-zA-Z0-9]", "_");
            String fileName = "Certificate_" + courseName + "_" + System.currentTimeMillis() + ".png";
            
            // Save the image
            OutputStream outputStream;
            Uri imageUri;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Ragam Certificates");
                
                imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (imageUri != null) {
                    outputStream = getContentResolver().openOutputStream(imageUri);
                } else {
                    throw new Exception("Failed to create image file");
                }
            } else {
                // Use File for older Android versions
                File directory = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "Ragam Certificates");
                if (!directory.exists()) {
                    directory.mkdirs();
                }
                File file = new File(directory, fileName);
                outputStream = new FileOutputStream(file);
            }
            
            // Write bitmap to output stream
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.flush();
                outputStream.close();
                
                Toast.makeText(this, "Certificate saved to Pictures/Ragam Certificates", Toast.LENGTH_LONG).show();
                Log.d(TAG, "Certificate saved successfully: " + fileName);
            }
            
            // Clean up
            certificateView.setDrawingCacheEnabled(false);
            certificateView.destroyDrawingCache();
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving certificate as image", e);
            Toast.makeText(this, "Error saving certificate: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
