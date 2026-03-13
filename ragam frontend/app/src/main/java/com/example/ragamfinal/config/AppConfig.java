package com.example.ragamfinal.config;

import android.content.Context;
import android.content.SharedPreferences;

public class AppConfig {
    // ============================================================
    // DUAL IP CONFIGURATION - Easy Switching System
    // ============================================================

    // Primary IP addresses - You can switch between these easily
    public static final String IP_ADDRESS_1 = "172.20.10.4";   // Current IP (New Network)
    public static final String IP_ADDRESS_2 = "172.20.10.4";   // Alternative IP (Mobile Hotspot)
    public static final String IP_LOCALHOST = "10.0.2.2";      // Android Emulator localhost

    // MySQL Database Configuration (Backend reference)
    public static final String MYSQL_PORT = "3305";             // MySQL port

    // Default IP (change this to switch default)
    private static final String DEFAULT_IP = IP_ADDRESS_1;

    // Backend folder path under htdocs
    private static final String API_BASE_PATH = "ragamfinal";

    // Preference key for storing selected IP
    private static final String PREF_SELECTED_IP = "selected_ip_address";

    /**
     * Get current IP address (from SharedPreferences or default)
     */
    public static String getCurrentIP(Context context) {
        if (context == null) {
            return DEFAULT_IP;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_SELECTED_IP, DEFAULT_IP);
    }

    /**
     * Set current IP address
     */
    public static void setCurrentIP(Context context, String ipAddress) {
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(PREF_SELECTED_IP, ipAddress).apply();
        }
    }

    /**
     * Switch to next IP address (IP1 -> IP2 -> IP1)
     */
    public static String switchToNextIP(Context context) {
        String currentIP = getCurrentIP(context);
        String nextIP;

        if (currentIP.equals(IP_ADDRESS_1)) {
            nextIP = IP_ADDRESS_2;
        } else {
            nextIP = IP_ADDRESS_1;
        }

        setCurrentIP(context, nextIP);
        return nextIP;
    }

    /**
     * Get base URL dynamically based on current IP
     */
    public static String getBaseUrl(Context context) {
        String ip = getCurrentIP(context);
        return "http://" + ip + "/" + API_BASE_PATH + "/";
    }

    // Static BASE_URL for backwards compatibility (uses default IP)
    // WARNING: Use getBaseUrl(context) instead for dynamic IP switching
    public static final String BASE_URL = "http://" + DEFAULT_IP + "/" + API_BASE_PATH + "/";

    // API Endpoints (static - for backwards compatibility)
    public static final String LOGIN_ENDPOINT = BASE_URL + "login.php";
    public static final String REGISTER_ENDPOINT = BASE_URL + "register.php";
    public static final String COURSES_ENDPOINT = BASE_URL + "courses.php";
    public static final String MENTORS_ENDPOINT = BASE_URL + "mentors.php";
    public static final String MESSAGES_ENDPOINT = BASE_URL + "messages.php";
    public static final String USER_PROFILE_ENDPOINT = BASE_URL + "user_profile.php";
    public static final String CREATE_COURSE_ENDPOINT = BASE_URL + "create_course.php";
    public static final String MY_COURSES_ENDPOINT = BASE_URL + "my_courses.php";
    public static final String STUDENTS_ENDPOINT = BASE_URL + "students.php";

    // Request timeout settings
    public static final int DEFAULT_TIMEOUT_MS = 60000; // 60 seconds (increased for debugging)
    public static final int MAX_RETRIES = 3;

    // Shared Preferences keys
    public static final String PREF_NAME = "RagamFinalPrefs";
    public static final String USER_SESSION_KEY = "user_session";

    /**
     * Get IP display name for UI
     */
    public static String getIPDisplayName(String ip) {
        if (ip.equals(IP_ADDRESS_1)) {
            return "IP 1 (Mobile Hotspot)";
        } else if (ip.equals(IP_ADDRESS_2)) {
            return "IP 2 (WiFi Network)";
        } else if (ip.equals(IP_LOCALHOST)) {
            return "Localhost (Emulator)";
        } else {
            return "Custom IP";
        }
    }

    /**
     * Check if IP is available/reachable
     * Note: This is a placeholder - actual network check should be done asynchronously
     */
    public static boolean isIPConfigured(String ip) {
        return ip != null && !ip.isEmpty() &&
               (ip.equals(IP_ADDRESS_1) || ip.equals(IP_ADDRESS_2) || ip.equals(IP_LOCALHOST));
    }
}
