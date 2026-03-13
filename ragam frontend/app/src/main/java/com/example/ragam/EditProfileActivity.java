package com.example.ragamfinal;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;

import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.io.IOException;

public class EditProfileActivity extends AppCompatActivity {
    
    private static final String TAG = "EditProfileActivity";
    
    private ImageView backButton, profilePhotoPreview;
    private EditText editName, editEmail, editPhone, editPassword;
    private Button choosePhotoButton, saveButton;
    private ApiHelper apiHelper;
    private JSONObject currentUser;
    private Uri selectedPhotoUri;
    
    private ActivityResultLauncher<Intent> photoPickerLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editprofile);
        
        // Initialize ApiHelper
        apiHelper = new ApiHelper(this);
        currentUser = apiHelper.getUserSession();
        
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        initViews();
        setupPhotoPickerLauncher();
        loadCurrentUserData();
        setupClickListeners();
    }
    
    private void initViews() {
        backButton = findViewById(R.id.back_button);
        profilePhotoPreview = findViewById(R.id.profile_photo_preview);
        choosePhotoButton = findViewById(R.id.choose_photo_button);
        editName = findViewById(R.id.edit_name);
        editEmail = findViewById(R.id.edit_email);
        editPhone = findViewById(R.id.edit_phone);
        editPassword = findViewById(R.id.edit_password);
        saveButton = findViewById(R.id.save_button);
    }
    
    private void setupPhotoPickerLauncher() {
        photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedPhotoUri = result.getData().getData();
                    if (selectedPhotoUri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedPhotoUri);
                            profilePhotoPreview.setImageBitmap(bitmap);
                            Toast.makeText(this, "Photo selected successfully", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Log.e(TAG, "Error loading selected image", e);
                            Toast.makeText(this, "Error loading selected image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );
    }
    
    private void loadCurrentUserData() {
        try {
            editName.setText(currentUser.optString("full_name", ""));
            editEmail.setText(currentUser.optString("email", ""));
            editPhone.setText(currentUser.optString("phone", ""));
            
            // Load profile image if exists
            String profileImage = currentUser.optString("profile_image", "");
            if (!profileImage.isEmpty()) {
                // Here you could load the image using an image loading library like Glide
                // For now, we'll just keep the default placeholder
                Log.d(TAG, "Profile image URL: " + profileImage);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error loading user data", e);
            Toast.makeText(this, "Error loading profile data", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        choosePhotoButton.setOnClickListener(v -> selectPhoto());
        
        saveButton.setOnClickListener(v -> saveProfile());
    }
    
    private void selectPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        photoPickerLauncher.launch(Intent.createChooser(intent, "Select Profile Photo"));
    }
    
    private void saveProfile() {
        String name = editName.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        
        if (name.isEmpty()) {
            editName.setError("Name is required");
            return;
        }
        
        if (email.isEmpty()) {
            editEmail.setError("Email is required");
            return;
        }

        if (!password.isEmpty()) {
            if (password.length() < 8) {
                editPassword.setError("Password must be at least 8 characters");
                return;
            }
            if (!password.matches(".*[a-z].*")) {
                editPassword.setError("Password must contain at least one lowercase letter");
                return;
            }
            if (!password.matches(".*[A-Z].*")) {
                editPassword.setError("Password must contain at least one uppercase letter");
                return;
            }
            if (!password.matches(".*[0-9].*")) {
                editPassword.setError("Password must contain at least one number");
                return;
            }
            if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
                editPassword.setError("Password must contain at least one special character");
                return;
            }
        }

        saveButton.setEnabled(false);
        saveButton.setText("Saving...");
        
        try {
            int userId = currentUser.getInt("user_id");
            
            // First upload photo if selected
            if (selectedPhotoUri != null) {
                uploadProfilePhoto(userId, name, email, phone, password);
            } else {
                updateProfile(userId, name, email, phone, password);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving profile", e);
            Toast.makeText(this, "Error saving profile", Toast.LENGTH_SHORT).show();
            resetSaveButton();
        }
    }
    
    private void uploadProfilePhoto(int userId, String name, String email, String phone, String password) {
        // Compress image before upload
        Uri compressedUri = compressImage(selectedPhotoUri);
        
        // Log debug information
        Log.d(TAG, "Original URI: " + selectedPhotoUri);
        Log.d(TAG, "Compressed URI: " + compressedUri);
        
        apiHelper.uploadProfilePhoto(userId, compressedUri.toString(), new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Photo upload response: " + response);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.getBoolean("success")) {
                        // Update user session with new profile image
                        JSONObject currentUser = apiHelper.getUserSession();
                        if (currentUser != null) {
                            JSONObject data = jsonResponse.getJSONObject("data");
                            String profileImage = data.getString("profile_image");
                            currentUser.put("profile_image", profileImage);
                            apiHelper.saveUserSession(currentUser);
                            Log.d(TAG, "Updated user session with new profile image: " + profileImage);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing photo upload response", e);
                }
                // After photo upload, update other profile data
                updateProfile(userId, name, email, phone, password);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Photo upload failed: " + error);
                runOnUiThread(() -> {
                    if (error.contains("File size too large")) {
                        Toast.makeText(EditProfileActivity.this, 
                            "Image file is too large. Please select a smaller image.", Toast.LENGTH_LONG).show();
                    } else if (error.contains("Invalid file type")) {
                        Toast.makeText(EditProfileActivity.this, 
                            "Invalid file type. Please select a JPEG, PNG, or GIF image.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(EditProfileActivity.this, 
                            "Photo upload failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
                // Continue with profile update even if photo upload fails
                updateProfile(userId, name, email, phone, password);
            }
        });
    }
    
    private Uri compressImage(Uri originalUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(originalUri);
            Bitmap originalBitmap = BitmapFactory.decodeStream(imageStream);
            
            if (originalBitmap == null) {
                return originalUri;
            }
            
            // Calculate new dimensions (max 1024x1024)
            int maxDimension = 1024;
            int width = originalBitmap.getWidth();
            int height = originalBitmap.getHeight();
            
            float ratio = Math.min((float) maxDimension / width, (float) maxDimension / height);
            int newWidth = Math.round(width * ratio);
            int newHeight = Math.round(height * ratio);
            
            // Resize bitmap
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
            
            // Compress to JPEG with 80% quality
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            
            // Save compressed image to temporary file with proper JPEG extension
            File tempFile = new File(getCacheDir(), "compressed_profile_" + System.currentTimeMillis() + ".jpeg");
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            fileOutputStream.write(byteArrayOutputStream.toByteArray());
            fileOutputStream.close();
            
            // Clean up
            originalBitmap.recycle();
            resizedBitmap.recycle();
            byteArrayOutputStream.close();
            
            Log.d(TAG, "Image compressed from " + originalUri + " to " + tempFile.getAbsolutePath());
            Log.d(TAG, "Compressed file size: " + tempFile.length() + " bytes");
            return Uri.fromFile(tempFile);
            
        } catch (Exception e) {
            Log.e(TAG, "Error compressing image", e);
            return originalUri; // Return original if compression fails
        }
    }
    
    private void updateProfile(int userId, String name, String email, String phone, String password) {
        apiHelper.updateProfile(userId, name, email, phone, password, "", "", 0, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonResponse = new JSONObject(response);
                        if (jsonResponse.getBoolean("success")) {
                            // Update stored user session
                            JSONObject userData = jsonResponse.getJSONObject("data").getJSONObject("user");
                            apiHelper.saveUserSession(userData);
                            
                            Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_LONG).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            String message = jsonResponse.optString("message", "Profile update failed");
                            Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing update response", e);
                        Toast.makeText(EditProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                    }
                    resetSaveButton();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Profile update failed: " + error);
                    Toast.makeText(EditProfileActivity.this, "Profile update failed: " + error, Toast.LENGTH_SHORT).show();
                    resetSaveButton();
                });
            }
        });
    }
    
    private void resetSaveButton() {
        saveButton.setEnabled(true);
        saveButton.setText("Save Changes");
    }
}