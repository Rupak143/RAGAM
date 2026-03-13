package com.example.ragamfinal.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.ragamfinal.config.AppConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ApiHelper {
    private static final String TAG = "ApiHelper";
    // Use centralized URL configuration with dynamic IP support
    private String BASE_URL;
    
    private RequestQueue requestQueue;
    private Context context;
    
    public ApiHelper(Context context) {
        this.context = context;
        this.requestQueue = Volley.newRequestQueue(context);
        // Get dynamic base URL based on selected IP
        this.BASE_URL = AppConfig.getBaseUrl(context);
        Log.d(TAG, "ApiHelper initialized with BASE_URL: " + BASE_URL);
    }
    
    /**
     * Refresh BASE_URL - call this after changing IP address
     */
    public void refreshBaseUrl() {
        this.BASE_URL = AppConfig.getBaseUrl(context);
        Log.d(TAG, "BASE_URL refreshed to: " + BASE_URL);
    }
    
    /**
     * Get current base URL
     */
    public String getBaseUrl() {
        return BASE_URL;
    }
    
    // Helper method to set timeout for requests
    private void setRequestTimeout(Request<?> request) {
        request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
            AppConfig.DEFAULT_TIMEOUT_MS, // Use centralized timeout config
            AppConfig.MAX_RETRIES, // Use centralized retry config
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
    }
    
    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    // Authentication endpoints
    public void login(String email, String password, String userType, ApiCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", email);
            jsonBody.put("password", password);
            jsonBody.put("user_type", userType);
            
            StringRequest request = new StringRequest(Request.Method.POST,
                    BASE_URL + "auth.php?action=login",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "Login response: " + response);
                            callback.onSuccess(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Login error", error);
                            String errorMessage = "Login failed";
                            if (error.networkResponse != null && error.networkResponse.data != null) {
                                try {
                                    String errorResponse = new String(error.networkResponse.data, "UTF-8");
                                    Log.e(TAG, "Error response body: " + errorResponse);
                                    errorMessage = "Login failed: " + errorResponse;
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing error response", e);
                                }
                            }
                            callback.onError(errorMessage);
                        }
                    }) {
                @Override
                public byte[] getBody() {
                    return jsonBody.toString().getBytes();
                }
                
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            
            setRequestTimeout(request);
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Login JSON error", e);
            callback.onError("Login failed: " + e.getMessage());
        }
    }
    
    public void register(String email, String password, String fullName, String userType, 
                        String phone, String bio, int experienceYears, String specialization, 
                        ApiCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", email);
            jsonBody.put("password", password);
            jsonBody.put("full_name", fullName);
            jsonBody.put("user_type", userType);
            jsonBody.put("phone", phone);
            jsonBody.put("bio", bio);
            jsonBody.put("experience_years", experienceYears);
            jsonBody.put("specialization", specialization);
            
            StringRequest request = new StringRequest(Request.Method.POST,
                    BASE_URL + "auth.php?action=register",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "Registration response: " + response);
                            callback.onSuccess(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Registration error", error);
                            String errorMessage = "Registration failed";
                            if (error.networkResponse != null && error.networkResponse.data != null) {
                                try {
                                    String errorResponse = new String(error.networkResponse.data, "UTF-8");
                                    Log.e(TAG, "Error response body: " + errorResponse);
                                    errorMessage = "Registration failed: " + errorResponse;
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing error response", e);
                                }
                            }
                            callback.onError(errorMessage);
                        }
                    }) {
                @Override
                public byte[] getBody() {
                    return jsonBody.toString().getBytes();
                }
                
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            
            setRequestTimeout(request);
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Registration JSON error", e);
            callback.onError("Registration failed: " + e.getMessage());
        }
    }
    
    // Course endpoints
    public void getAllCourses(ApiCallback callback) {
        getAllCourses(0, callback); // Get all courses without category filter
    }
    
    // Get courses filtered by category
    public void getAllCourses(int categoryId, ApiCallback callback) {
        String url = BASE_URL + "courses.php?action=all_courses";
        if (categoryId > 0) {
            url += "&category_id=" + categoryId;
        }
        
        Log.d(TAG, "getAllCourses() calling URL: " + url);
        
        StringRequest request = new StringRequest(Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "getAllCourses() response received, length: " + response.length());
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "getAllCourses() error: " + error.getMessage());
                        String errorMsg = "Network error loading courses";
                        if (error.networkResponse != null) {
                            errorMsg += " (Code: " + error.networkResponse.statusCode + ")";
                        }
                        callback.onError(errorMsg);
                    }
                });
        
        requestQueue.add(request);
    }
    
    public void getCourseDetails(int courseId, ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.GET,
                BASE_URL + "courses.php?action=course_details&course_id=" + courseId,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Get course details error", error);
                        callback.onError("Failed to get course details: " + error.getMessage());
                    }
                });
        
        requestQueue.add(request);
    }
    
    public void enrollCourse(int studentId, int courseId, ApiCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("student_id", studentId);
            jsonBody.put("course_id", courseId);
            
            Log.d(TAG, "Enrolling with data: " + jsonBody.toString());
            
            StringRequest request = new StringRequest(Request.Method.POST,
                    BASE_URL + "courses.php?action=enroll",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "Enrollment response: " + response);
                            callback.onSuccess(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Enroll course error", error);
                            String errorMessage = "Enrollment failed";
                            if (error.networkResponse != null && error.networkResponse.data != null) {
                                try {
                                    String errorResponse = new String(error.networkResponse.data, "UTF-8");
                                    Log.e(TAG, "Error response body: " + errorResponse);
                                    errorMessage = "Enrollment failed: " + errorResponse;
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing error response", e);
                                }
                            }
                            callback.onError(errorMessage);
                        }
                    }) {
                @Override
                public byte[] getBody() {
                    return jsonBody.toString().getBytes();
                }
                
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            
            setRequestTimeout(request);
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Enroll course JSON error", e);
            callback.onError("Enrollment failed: " + e.getMessage());
        }
    }
    
    public void getEnrolledCourses(int studentId, ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.GET,
                BASE_URL + "courses.php?action=enrolled_courses&student_id=" + studentId,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Get enrolled courses error", error);
                        callback.onError("Failed to get enrolled courses: " + error.getMessage());
                    }
                });
        
        requestQueue.add(request);
    }
    
    public void getCompletedCourses(int studentId, ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.GET,
                BASE_URL + "courses.php?action=completed_courses&student_id=" + studentId,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Get completed courses error", error);
                        callback.onError("Failed to get completed courses: " + error.getMessage());
                    }
                });
        
        requestQueue.add(request);
    }
    
    // Profile endpoints
    public void getMentors(ApiCallback callback) {
        getMentors(0, callback); // Get all mentors without category filter
    }
    
    // Get mentors filtered by category
    public void getMentors(int categoryId, ApiCallback callback) {
        String url = BASE_URL + "courses.php?action=get_mentors";
        if (categoryId > 0) {
            url += "&category_id=" + categoryId;
        }
        
        Log.d(TAG, "getMentors() calling URL: " + url);
        
        StringRequest request = new StringRequest(Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "getMentors() response: " + response);
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "getMentors() error: " + error.getMessage());
                        String errorMsg = "Network error";
                        if (error.networkResponse != null) {
                            errorMsg += " (Code: " + error.networkResponse.statusCode + ")";
                        }
                        callback.onError(errorMsg);
                    }
                });
        
        requestQueue.add(request);
    }
    
    public void getUserProfile(int userId, ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.GET,
                BASE_URL + "profile.php?action=profile&user_id=" + userId,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Get profile error", error);
                        callback.onError("Failed to get profile: " + error.getMessage());
                    }
                });
        
        requestQueue.add(request);
    }
    
    // Course creation endpoint
    public void createCourse(int teacherId, String title, String description, String videoPath, ApiCallback callback) {
        try {
            // First upload the video file, then create the course
            uploadVideoFile(videoPath, new ApiCallback() {
                @Override
                public void onSuccess(String uploadResponse) {
                    try {
                        Log.d(TAG, "Video upload response: " + uploadResponse);
                        JSONObject uploadResult = new JSONObject(uploadResponse);
                        
                        String videoUrl;
                        if (uploadResult.getString("status").equals("success")) {
                            videoUrl = uploadResult.getString("video_url");
                        } else {
                            // Fallback to demo video if upload fails
                            Log.w(TAG, "Video upload failed, using demo video");
                            videoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
                        }
                        
                        // Now create the course with the video URL
                        createCourseWithVideoUrl(teacherId, title, description, videoUrl, callback);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing upload response", e);
                        // Fallback to demo video
                        String demoVideoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
                        createCourseWithVideoUrl(teacherId, title, description, demoVideoUrl, callback);
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Video upload failed: " + error);
                    // Fallback to demo video
                    String demoVideoUrl = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
                    createCourseWithVideoUrl(teacherId, title, description, demoVideoUrl, callback);
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error in createCourse", e);
            callback.onError("Course creation failed: " + e.getMessage());
        }
    }
    
    private void createCourseWithVideoUrl(int teacherId, String title, String description, String videoUrl, ApiCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("teacher_id", teacherId);
            jsonBody.put("course_title", title);
            jsonBody.put("course_description", description);
            jsonBody.put("video_path", videoUrl);
            jsonBody.put("category_id", 1); // Default to first category
            jsonBody.put("course_price", 0); // Default to free
            jsonBody.put("difficulty_level", "beginner");
            
            Log.d(TAG, "Creating course with data: " + jsonBody.toString());
            
            StringRequest request = new StringRequest(Request.Method.POST,
                    BASE_URL + "courses.php?action=create_course",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "Create course response: " + response);
                            callback.onSuccess(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Create course error", error);
                            String errorMessage = "Course creation failed";
                            
                            if (error.networkResponse != null) {
                                Log.e(TAG, "Error status code: " + error.networkResponse.statusCode);
                                if (error.networkResponse.data != null) {
                                    try {
                                        String errorResponse = new String(error.networkResponse.data, "UTF-8");
                                        Log.e(TAG, "Error response body: " + errorResponse);
                                        errorMessage = "Course creation failed: " + errorResponse;
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error parsing error response", e);
                                    }
                                }
                            } else if (error.getMessage() != null) {
                                errorMessage = "Network error: " + error.getMessage();
                            }
                            
                            callback.onError(errorMessage);
                        }
                    }) {
                @Override
                public byte[] getBody() {
                    return jsonBody.toString().getBytes();
                }
                
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            
            setRequestTimeout(request);
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Create course JSON error", e);
            callback.onError("Course creation failed: " + e.getMessage());
        }
    }

    // Debug method to test course creation endpoint
    public void debugCourseCreation(ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.GET,
                BASE_URL + "courses.php?action=debug_create",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Debug response: " + response);
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Debug error", error);
                        String errorMessage = "Debug failed";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String errorResponse = new String(error.networkResponse.data, "UTF-8");
                                Log.e(TAG, "Debug error response: " + errorResponse);
                                errorMessage = errorResponse;
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing debug error response", e);
                            }
                        }
                        callback.onError(errorMessage);
                    }
                });
        
        setRequestTimeout(request);
        requestQueue.add(request);
    }

    // Simple connectivity test
    public void testConnection(ApiCallback callback) {
        Log.d(TAG, "Testing connection to: " + BASE_URL + "test.php");
        
        StringRequest request = new StringRequest(Request.Method.GET,
                BASE_URL + "test.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Connection test successful: " + response);
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Connection test failed", error);
                        String errorMessage = "Connection failed to " + BASE_URL;
                        
                        if (error.networkResponse != null) {
                            errorMessage += " (HTTP " + error.networkResponse.statusCode + ")";
                        } else if (error.getCause() != null) {
                            errorMessage += " (" + error.getCause().getMessage() + ")";
                        } else {
                            errorMessage += " (Network unreachable)";
                        }
                        
                        callback.onError(errorMessage);
                    }
                });
        
        setRequestTimeout(request);
        requestQueue.add(request);
    }

    // Get teacher's courses
    public void getTeacherCourses(int teacherId, ApiCallback callback) {
        try {
            StringRequest request = new StringRequest(Request.Method.GET,
                    BASE_URL + "courses.php?action=teacher_courses&teacher_id=" + teacherId,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "Teacher courses response: " + response);
                            callback.onSuccess(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Get teacher courses error", error);
                            callback.onError("Failed to get courses: " + error.getMessage());
                        }
                    });

            setRequestTimeout(request);
            requestQueue.add(request);
        } catch (Exception e) {
            Log.e(TAG, "Error creating teacher courses request", e);
            callback.onError("Error creating request: " + e.getMessage());
        }
    }

    // Get enrolled students for teacher's courses
    public void getCourseStudents(int courseId, ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.GET,
                BASE_URL + "courses.php?action=course_students&course_id=" + courseId,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Get course students error", error);
                        callback.onError("Failed to get students: " + error.getMessage());
                    }
                });
        
        requestQueue.add(request);
    }

    // Get all students enrolled in teacher's courses
    public void getTeacherStudents(int teacherId, ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.GET,
                BASE_URL + "courses.php?action=teacher_students&teacher_id=" + teacherId,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Get teacher students error", error);
                        callback.onError("Failed to get students: " + error.getMessage());
                    }
                });
        
        requestQueue.add(request);
    }
    
    // Messages endpoints
    public void getInboxMessages(int userId, ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.GET,
                BASE_URL + "messages.php?action=inbox&user_id=" + userId,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Get inbox messages error", error);
                        callback.onError("Failed to get messages: " + error.getMessage());
                    }
                });
        
        requestQueue.add(request);
    }
    
    // User session management
    public void saveUserSession(JSONObject user) {
        SharedPreferences prefs = context.getSharedPreferences("RagamSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_data", user.toString());
        editor.putBoolean("is_logged_in", true);
        editor.apply();
    }
    
    public JSONObject getUserSession() {
        SharedPreferences prefs = context.getSharedPreferences("RagamSession", Context.MODE_PRIVATE);
        String userData = prefs.getString("user_data", null);
        if (userData != null) {
            try {
                return new JSONObject(userData);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing user data", e);
            }
        }
        return null;
    }
    
    public boolean isLoggedIn() {
        SharedPreferences prefs = context.getSharedPreferences("RagamSession", Context.MODE_PRIVATE);
        return prefs.getBoolean("is_logged_in", false);
    }
    
    public void logout() {
        SharedPreferences prefs = context.getSharedPreferences("RagamSession", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
        Log.d(TAG, "User logged out successfully");
    }
    
    // Delete course method
    public void deleteCourse(int courseId, ApiCallback callback) {
        String url = BASE_URL + "courses.php?action=delete_course";
        
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("course_id", courseId);
            
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    Log.d(TAG, "Delete course response: " + response.toString());
                    callback.onSuccess(response.toString());
                },
                error -> {
                    Log.e(TAG, "Delete course error", error);
                    String errorMessage = "Failed to delete course";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMessage = new String(error.networkResponse.data);
                    }
                    callback.onError(errorMessage);
                });
            
            setRequestTimeout(request);
            requestQueue.add(request);
            
        } catch (Exception e) {
            Log.e(TAG, "Error deleting course", e);
            callback.onError("Error deleting course: " + e.getMessage());
        }
    }
    
    // Get course lessons with enrollment check
    public void getCourseLessons(int courseId, int studentId, ApiCallback callback) {
        StringRequest request = new StringRequest(Request.Method.GET,
                BASE_URL + "courses.php?action=course_lessons&course_id=" + courseId + "&student_id=" + studentId,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Course lessons response: " + response);
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Get course lessons error", error);
                        String errorMessage = "Failed to get lessons";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String errorResponse = new String(error.networkResponse.data, "UTF-8");
                                Log.e(TAG, "Error response body: " + errorResponse);
                                errorMessage = "Failed to get lessons: " + errorResponse;
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error response", e);
                            }
                        }
                        callback.onError(errorMessage);
                    }
                });
        
        setRequestTimeout(request);
        requestQueue.add(request);
    }
    
    public void uploadVideoFile(String videoPath, ApiCallback callback) {
        Log.d(TAG, "Attempting to upload video: " + videoPath);
        
        // Check if it's already a web URL
        if (videoPath.startsWith("http://") || videoPath.startsWith("https://")) {
            // Already a web URL, return as is
            try {
                JSONObject result = new JSONObject();
                result.put("status", "success");
                result.put("video_url", videoPath);
                callback.onSuccess(result.toString());
                return;
            } catch (Exception e) {
                callback.onError("Error creating response: " + e.getMessage());
                return;
            }
        }
        
        // Handle content:// URIs by actually uploading the file
        if (videoPath.startsWith("content://")) {
            uploadVideoFromUri(videoPath, callback);
        } else {
            // Fallback to demo video for other cases
            Log.w(TAG, "Unknown video path format, using demo video: " + videoPath);
            try {
                JSONObject result = new JSONObject();
                result.put("status", "success");
                result.put("video_url", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
                callback.onSuccess(result.toString());
            } catch (Exception e) {
                callback.onError("Error creating demo response: " + e.getMessage());
            }
        }
    }
    
    private void uploadVideoFromUri(String uriString, ApiCallback callback) {
        try {
            android.net.Uri uri = android.net.Uri.parse(uriString);
            
            Log.d(TAG, "Starting video upload...");
            Log.d(TAG, "Upload URI: " + uriString);
            Log.d(TAG, "Upload URL: " + BASE_URL + "upload_video.php");
            
            // Create multipart request
            MultipartRequest multipartRequest = new MultipartRequest(
                BASE_URL + "upload_video.php",
                context,
                error -> {
                    Log.e(TAG, "Video upload failed", error);
                    String errorMessage = "Video upload failed";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            errorMessage = new String(error.networkResponse.data, "UTF-8");
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing upload error", e);
                        }
                    }
                    
                    // Fallback to demo video on upload failure
                    Log.w(TAG, "Upload failed, falling back to demo video: " + errorMessage);
                    try {
                        JSONObject result = new JSONObject();
                        result.put("status", "success");
                        result.put("video_url", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
                        callback.onSuccess(result.toString());
                    } catch (Exception ex) {
                        callback.onError("Upload failed and demo fallback failed: " + ex.getMessage());
                    }
                },
                response -> {
                    try {
                        String responseString = new String(response.data, "UTF-8");
                        Log.d(TAG, "Video upload success: " + responseString);
                        
                        // Parse response and convert relative path to full URL
                        JSONObject responseJson = new JSONObject(responseString);
                        if (responseJson.getBoolean("success")) {
                            JSONObject data = responseJson.getJSONObject("data");
                            String relativePath = data.getString("video_url");
                            // relativePath already contains "uploads/videos/filename.mp4" from backend
                            // Just prepend BASE_URL - FIXED: removed duplicate "uploads/"
                            String fullUrl = BASE_URL + relativePath;
                            
                            Log.d(TAG, "Video upload successful!");
                            Log.d(TAG, "Relative path: " + relativePath);
                            Log.d(TAG, "Full URL: " + fullUrl);
                            
                            JSONObject result = new JSONObject();
                            result.put("status", "success");
                            result.put("video_url", fullUrl);
                            callback.onSuccess(result.toString());
                        } else {
                            throw new Exception("Upload response indicated failure");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing upload response", e);
                        // Fallback to demo video
                        try {
                            JSONObject result = new JSONObject();
                            result.put("status", "success");
                            result.put("video_url", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
                            callback.onSuccess(result.toString());
                        } catch (Exception ex) {
                            callback.onError("Response parsing failed: " + ex.getMessage());
                        }
                    }
                }
            );
            
            // Add form parameters
            multipartRequest.addStringParam("course_id", "temp");
            multipartRequest.addStringParam("lesson_title", "Introduction Video");
            
            // Add the video file
            String fileName = MultipartRequest.getFileName(context, uri);
            String mimeType = MultipartRequest.getMimeType(context, uri);
            multipartRequest.addFileParam("video", fileName, mimeType, uri);
            
            // Add to request queue
            requestQueue.add(multipartRequest);
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up multipart upload", e);
            // Fallback to demo video
            try {
                JSONObject result = new JSONObject();
                result.put("status", "success");
                result.put("video_url", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
                callback.onSuccess(result.toString());
            } catch (Exception ex) {
                callback.onError("Upload setup failed: " + ex.getMessage());
            }
        }
    }
    
    // Profile management methods
    public void updateProfile(int userId, String fullName, String email, String phone, String password, 
                             String bio, String specialization, int experienceYears, ApiCallback callback) {
        try {
            StringRequest request = new StringRequest(Request.Method.POST,
                    BASE_URL + "profile_api.php",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "Update profile response: " + response);
                            callback.onSuccess(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Update profile error", error);
                            String errorMessage = "Profile update failed";
                            if (error.networkResponse != null && error.networkResponse.data != null) {
                                try {
                                    errorMessage = new String(error.networkResponse.data, "UTF-8");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing error response", e);
                                }
                            }
                            callback.onError(errorMessage);
                        }
                    }) {
                @Override
                protected Map<String, String> getParams() {
                    Map<String, String> params = new HashMap<>();
                    params.put("action", "update_profile");
                    params.put("user_id", String.valueOf(userId));
                    params.put("full_name", fullName);
                    params.put("email", email);
                    params.put("phone", phone);
                    params.put("bio", bio);
                    params.put("specialization", specialization);
                    params.put("experience_years", String.valueOf(experienceYears));
                    if (!password.isEmpty()) {
                        params.put("password", password);
                    }
                    return params;
                }
            };
            
            setRequestTimeout(request);
            requestQueue.add(request);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in updateProfile", e);
            callback.onError("Profile update failed: " + e.getMessage());
        }
    }
    
    public void uploadProfilePhoto(int userId, String photoUriString, ApiCallback callback) {
        try {
            android.net.Uri uri = android.net.Uri.parse(photoUriString);
            
            Log.d(TAG, "Starting profile photo upload...");
            Log.d(TAG, "Photo URI: " + photoUriString);
            Log.d(TAG, "Upload URL: " + BASE_URL + "profile_api.php");
            
            // Create multipart request
            MultipartRequest multipartRequest = new MultipartRequest(
                BASE_URL + "profile_api.php",
                context,
                error -> {
                    Log.e(TAG, "Profile photo upload failed", error);
                    String errorMessage = "Photo upload failed";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            errorMessage = new String(error.networkResponse.data, "UTF-8");
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing upload error", e);
                        }
                    }
                    callback.onError(errorMessage);
                },
                response -> {
                    try {
                        String responseString = new String(response.data, "UTF-8");
                        Log.d(TAG, "Profile photo upload success: " + responseString);
                        callback.onSuccess(responseString);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing upload response", e);
                        callback.onError("Response parsing failed: " + e.getMessage());
                    }
                }
            );
            
            // Add form parameters
            multipartRequest.addStringParam("action", "upload_profile_photo");
            multipartRequest.addStringParam("user_id", String.valueOf(userId));
            
            // Add the photo file
            String fileName = MultipartRequest.getFileName(context, uri);
            String mimeType = MultipartRequest.getMimeType(context, uri);
            multipartRequest.addFileParam("profile_photo", fileName, mimeType, uri);
            
            // Add to request queue
            requestQueue.add(multipartRequest);
            
        } catch (Exception e) {
            Log.e(TAG, "Error setting up profile photo upload", e);
            callback.onError("Upload setup failed: " + e.getMessage());
        }
    }
    
    // Multi-video course creation
    public void createMultiVideoCourse(int instructorId, String title, String description, String category, int videoCount, ApiCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("instructor_id", instructorId);
            jsonBody.put("title", title);
            jsonBody.put("description", description);
            jsonBody.put("category", category);
            jsonBody.put("video_count", videoCount);
            
            StringRequest request = new StringRequest(Request.Method.POST,
                    BASE_URL + "courses.php?action=create_multi_video_course",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "Create multi-video course response: " + response);
                            callback.onSuccess(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Create multi-video course error", error);
                            String errorMessage = "Course creation failed";
                            if (error.networkResponse != null && error.networkResponse.data != null) {
                                try {
                                    String errorResponse = new String(error.networkResponse.data, "UTF-8");
                                    errorMessage = "Course creation failed: " + errorResponse;
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing error response", e);
                                }
                            }
                            callback.onError(errorMessage);
                        }
                    }) {
                @Override
                public byte[] getBody() {
                    return jsonBody.toString().getBytes();
                }
                
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            
            setRequestTimeout(request);
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Create multi-video course JSON error", e);
            callback.onError("Course creation failed: " + e.getMessage());
        }
    }
    
    // Get course videos for student
    public void getCourseVideos(int courseId, int userId, ApiCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("course_id", courseId);
            jsonBody.put("user_id", userId);
            
            StringRequest request = new StringRequest(Request.Method.POST,
                    BASE_URL + "courses.php?action=get_course_videos",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "Get course videos response: " + response);
                            callback.onSuccess(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Get course videos error", error);
                            String errorMessage = "Failed to load course videos";
                            if (error.networkResponse != null && error.networkResponse.data != null) {
                                try {
                                    String errorResponse = new String(error.networkResponse.data, "UTF-8");
                                    errorMessage = "Failed to load course videos: " + errorResponse;
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing error response", e);
                                }
                            }
                            callback.onError(errorMessage);
                        }
                    }) {
                @Override
                public byte[] getBody() {
                    return jsonBody.toString().getBytes();
                }
                
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            
            setRequestTimeout(request);
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Get course videos JSON error", e);
            callback.onError("Failed to load course videos: " + e.getMessage());
        }
    }
    
    // Mark video as completed
    public void markVideoAsCompleted(int userId, int videoId, ApiCallback callback) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("user_id", userId);
            jsonBody.put("video_id", videoId);
            
            StringRequest request = new StringRequest(Request.Method.POST,
                    BASE_URL + "courses.php?action=mark_video_completed",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "Mark video completed response: " + response);
                            callback.onSuccess(response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, "Mark video completed error", error);
                            String errorMessage = "Failed to mark video as completed";
                            if (error.networkResponse != null && error.networkResponse.data != null) {
                                try {
                                    String errorResponse = new String(error.networkResponse.data, "UTF-8");
                                    errorMessage = "Failed to mark video as completed: " + errorResponse;
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing error response", e);
                                }
                            }
                            callback.onError(errorMessage);
                        }
                    }) {
                @Override
                public byte[] getBody() {
                    return jsonBody.toString().getBytes();
                }
                
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }
            };
            
            setRequestTimeout(request);
            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Mark video completed JSON error", e);
            callback.onError("Failed to mark video as completed: " + e.getMessage());
        }
    }
    
    // New simplified course creation method
    public void createCourse(int instructorId, String title, String description, String category, int videoCount, JSONApiCallback callback) {
        String url = BASE_URL + "courses.php?action=create_course";
        
        try {
            JSONObject params = new JSONObject();
            params.put("teacher_id", instructorId);
            params.put("course_title", title);
            params.put("course_description", description);
            params.put("category_id", mapCategoryNameToId(category));
            params.put("course_price", 0);
            params.put("difficulty_level", "beginner");
            
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, params,
                    callback::onSuccess,
                    error -> {
                        Log.e("ApiHelper", "Create course error: " + error.getMessage());
                        callback.onError("Network error: " + error.getMessage());
                    });
            
            requestQueue.add(request);
        } catch (JSONException e) {
            callback.onError("JSON error: " + e.getMessage());
        }
    }

    // Create course with multi-video support
    public void createCourse(JSONObject courseData, final JSONApiCallback callback) {
        try {
            String url = BASE_URL + "courses.php?action=create_course";

            JSONObject payload = new JSONObject();
            payload.put("teacher_id", courseData.optInt("teacher_id", courseData.optInt("instructor_id", 0)));
            payload.put("course_title", courseData.optString("course_title", courseData.optString("title", "")));
            payload.put("course_description", courseData.optString("course_description", courseData.optString("description", "")));

            if (courseData.has("category_id")) {
                payload.put("category_id", courseData.optInt("category_id", 1));
            } else {
                payload.put("category_id", mapCategoryNameToId(courseData.optString("category", "")));
            }

            payload.put("course_price", courseData.optInt("course_price", 0));
            payload.put("difficulty_level", courseData.optString("difficulty_level", "beginner"));

            if (courseData.has("video_path")) {
                payload.put("video_path", courseData.optString("video_path", ""));
            }
            
            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                payload,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Create course response: " + response);
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Create course error", error);
                        String errorMessage = "Failed to create course";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String errorResponse = new String(error.networkResponse.data, "UTF-8");
                                Log.e(TAG, "Error response: " + errorResponse);
                                errorMessage = errorResponse;
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error response", e);
                            }
                        } else if (error.getMessage() != null) {
                            errorMessage = error.getMessage();
                        }
                        callback.onError(errorMessage);
                    }
                });
            
            setRequestTimeout(request);
            requestQueue.add(request);
            
        } catch (Exception e) {
            Log.e(TAG, "Create course error", e);
            callback.onError("Failed to create course: " + e.getMessage());
        }
    }

    private int mapCategoryNameToId(String category) {
        if (category == null) {
            return 1;
        }

        switch (category.trim()) {
            case "Vocal Training":
                return 1;
            case "Instrumental Music":
                return 2;
            case "Devotional Music":
                return 6;
            case "Kids Special":
                return 8;
            default:
                return 1;
        }
    }
    
    // Upload course video
    public void uploadCourseVideo(JSONObject videoData, final JSONApiCallback callback) {
        try {
            String url = BASE_URL + "courses.php?action=upload_video"; // Add action to URL
            
            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                videoData,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Upload video response: " + response);
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Upload video error", error);
                        String errorMessage = "Failed to upload video";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String errorResponse = new String(error.networkResponse.data, "UTF-8");
                                Log.e(TAG, "Error response: " + errorResponse);
                                errorMessage = errorResponse;
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error response", e);
                            }
                        } else if (error.getMessage() != null) {
                            errorMessage = error.getMessage();
                        }
                        callback.onError(errorMessage);
                    }
                });
            
            setRequestTimeout(request);
            requestQueue.add(request);
            
        } catch (Exception e) {
            Log.e(TAG, "Upload video error", e);
            callback.onError("Failed to upload video: " + e.getMessage());
        }
    }
    
    // Generic JSON POST request method
    public void makeJsonPostRequest(String url, JSONObject params, final JSONApiCallback callback) {
        try {
            JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "POST response from " + url + ": " + response);
                        callback.onSuccess(response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "POST error to " + url, error);
                        String errorMessage = "Request failed";
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            try {
                                String errorResponse = new String(error.networkResponse.data, "UTF-8");
                                Log.e(TAG, "Error response: " + errorResponse);
                                errorMessage = errorResponse;
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing error response", e);
                            }
                        } else if (error.getMessage() != null) {
                            errorMessage = error.getMessage();
                        }
                        callback.onError(errorMessage);
                    }
                });
            
            setRequestTimeout(request);
            requestQueue.add(request);
            
        } catch (Exception e) {
            Log.e(TAG, "Request error", e);
            callback.onError("Failed to make request: " + e.getMessage());
        }
    }
    
    // JSON callback interface for methods that return JSON objects
    public interface JSONApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }
}
