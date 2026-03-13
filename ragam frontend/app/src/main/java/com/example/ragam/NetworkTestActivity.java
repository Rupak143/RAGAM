package com.example.ragamfinal;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ragamfinal.config.AppConfig;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class NetworkTestActivity extends AppCompatActivity {
    
    private TextView resultText;
    private Button testButton;
    private RequestQueue requestQueue;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create simple layout programmatically
        resultText = new TextView(this);
        resultText.setText("Press button to test network connection");
        resultText.setPadding(20, 20, 20, 20);
        
        testButton = new Button(this);
        testButton.setText("Test Login Connection");
        testButton.setOnClickListener(v -> testLoginConnection());
        
        // Simple vertical layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(testButton);
        layout.addView(resultText);
        
        setContentView(layout);
        
        requestQueue = Volley.newRequestQueue(this);
    }
    
    private void testLoginConnection() {
        resultText.setText("Testing connection...\n");
        appendResult("Backend URL: " + AppConfig.BASE_URL + "auth.php?action=login");
        
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("email", "student@test.com");
            jsonBody.put("password", "password");
            jsonBody.put("user_type", "student");
            
            StringRequest request = new StringRequest(Request.Method.POST,
                    AppConfig.BASE_URL + "auth.php?action=login",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d("NetworkTest", "SUCCESS: " + response);
                            appendResult("\n✅ SUCCESS!");
                            appendResult("Response: " + response);
                            
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                if (jsonResponse.getBoolean("success")) {
                                    appendResult("\n🎉 LOGIN WORKING! Backend is accessible!");
                                    Toast.makeText(NetworkTestActivity.this, "Login is working!", Toast.LENGTH_LONG).show();
                                } else {
                                    appendResult("\n❌ Login failed: " + jsonResponse.getString("message"));
                                }
                            } catch (Exception e) {
                                appendResult("\n❌ JSON Parse Error: " + e.getMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e("NetworkTest", "ERROR", error);
                            appendResult("\n❌ CONNECTION FAILED!");
                            
                            if (error.networkResponse != null) {
                                appendResult("HTTP Code: " + error.networkResponse.statusCode);
                                if (error.networkResponse.data != null) {
                                    String errorBody = new String(error.networkResponse.data);
                                    appendResult("Error Body: " + errorBody);
                                }
                            }
                            
                            appendResult("Error Message: " + error.getMessage());
                            appendResult("\nPossible issues:");
                            appendResult("1. XAMPP/Apache not running");
                            appendResult("2. Wrong IP address (try localhost instead of 10.0.2.2)");
                            appendResult("3. Firewall blocking connection");
                            appendResult("4. auth.php file missing");
                            
                            Toast.makeText(NetworkTestActivity.this, "Connection failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
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
            
            requestQueue.add(request);
            
        } catch (Exception e) {
            appendResult("\n❌ Exception: " + e.getMessage());
            Log.e("NetworkTest", "Exception", e);
        }
    }
    
    private void appendResult(String text) {
        runOnUiThread(() -> {
            resultText.setText(resultText.getText() + "\n" + text);
        });
    }
}