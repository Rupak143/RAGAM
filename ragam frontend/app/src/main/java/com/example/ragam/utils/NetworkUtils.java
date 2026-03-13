package com.example.ragamfinal.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetworkUtils {
    private static final String TAG = "NetworkUtils";
    
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    public static String getNetworkInfo(Context context) {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
                return "Network: " + activeNetworkInfo.getTypeName() + 
                       " | Connected: " + activeNetworkInfo.isConnected() +
                       " | Available: " + activeNetworkInfo.isAvailable();
            }
        }
        return "No network information available";
    }
    
    public static void logNetworkStatus(Context context) {
        Log.d(TAG, "Network Status: " + getNetworkInfo(context));
        Log.d(TAG, "Network Available: " + isNetworkAvailable(context));
    }
}
