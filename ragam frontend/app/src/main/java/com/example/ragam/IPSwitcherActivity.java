package com.example.ragamfinal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ragamfinal.config.AppConfig;
import com.example.ragamfinal.utils.ApiHelper;

public class IPSwitcherActivity extends AppCompatActivity {
    
    private static final String TAG = "IPSwitcher";
    
    private RadioGroup radioGroupIP;
    private RadioButton radioIP1, radioIP2, radioLocalhost;
    private Button btnSave, btnTest, btnBack;
    private TextView textCurrentIP, textStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip_switcher);
        
        // Initialize views
        radioGroupIP = findViewById(R.id.radioGroupIP);
        radioIP1 = findViewById(R.id.radioIP1);
        radioIP2 = findViewById(R.id.radioIP2);
        radioLocalhost = findViewById(R.id.radioLocalhost);
        btnSave = findViewById(R.id.btnSaveIP);
        btnTest = findViewById(R.id.btnTestConnection);
        btnBack = findViewById(R.id.btnBackFromIPSwitcher);
        textCurrentIP = findViewById(R.id.textCurrentIP);
        textStatus = findViewById(R.id.textConnectionStatus);
        
        // Set up IP labels
        radioIP1.setText("IP 1: " + AppConfig.IP_ADDRESS_1 + " (Mobile Hotspot)");
        radioIP2.setText("IP 2: " + AppConfig.IP_ADDRESS_2 + " (WiFi Network)");
        radioLocalhost.setText("Localhost: " + AppConfig.IP_LOCALHOST + " (Emulator)");
        
        // Load current IP
        loadCurrentIP();
        
        // Set up listeners
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSelectedIP();
            }
        });
        
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testConnection();
            }
        });
        
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    private void loadCurrentIP() {
        String currentIP = AppConfig.getCurrentIP(this);
        textCurrentIP.setText("Current IP: " + currentIP);
        
        // Select the appropriate radio button
        if (currentIP.equals(AppConfig.IP_ADDRESS_1)) {
            radioIP1.setChecked(true);
        } else if (currentIP.equals(AppConfig.IP_ADDRESS_2)) {
            radioIP2.setChecked(true);
        } else if (currentIP.equals(AppConfig.IP_LOCALHOST)) {
            radioLocalhost.setChecked(true);
        }
    }
    
    private void saveSelectedIP() {
        int selectedId = radioGroupIP.getCheckedRadioButtonId();
        String selectedIP = null;
        
        if (selectedId == R.id.radioIP1) {
            selectedIP = AppConfig.IP_ADDRESS_1;
        } else if (selectedId == R.id.radioIP2) {
            selectedIP = AppConfig.IP_ADDRESS_2;
        } else if (selectedId == R.id.radioLocalhost) {
            selectedIP = AppConfig.IP_LOCALHOST;
        }
        
        if (selectedIP != null) {
            AppConfig.setCurrentIP(this, selectedIP);
            textCurrentIP.setText("Current IP: " + selectedIP);
            textStatus.setText("IP address saved! Restart app to use new IP.");
            textStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            Toast.makeText(this, "IP changed to: " + selectedIP, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "IP changed to: " + selectedIP);
        } else {
            Toast.makeText(this, "Please select an IP address", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void testConnection() {
        textStatus.setText("Testing connection...");
        textStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
        
        // Get currently selected IP (not saved yet)
        int selectedId = radioGroupIP.getCheckedRadioButtonId();
        String testIP = null;
        
        if (selectedId == R.id.radioIP1) {
            testIP = AppConfig.IP_ADDRESS_1;
        } else if (selectedId == R.id.radioIP2) {
            testIP = AppConfig.IP_ADDRESS_2;
        } else if (selectedId == R.id.radioLocalhost) {
            testIP = AppConfig.IP_LOCALHOST;
        }
        
        if (testIP == null) {
            Toast.makeText(this, "Please select an IP to test", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Temporarily save the IP for testing
        final String originalIP = AppConfig.getCurrentIP(this);
        AppConfig.setCurrentIP(this, testIP);
        
        ApiHelper apiHelper = new ApiHelper(this);
        final String finalTestIP = testIP;
        
        apiHelper.testConnection(new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> {
                    textStatus.setText("✓ Connection successful to " + finalTestIP);
                    textStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    Toast.makeText(IPSwitcherActivity.this, "Connection successful!", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    textStatus.setText("✗ Connection failed: " + error);
                    textStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    Toast.makeText(IPSwitcherActivity.this, "Connection failed!", Toast.LENGTH_SHORT).show();
                    
                    // Restore original IP
                    AppConfig.setCurrentIP(IPSwitcherActivity.this, originalIP);
                });
            }
        });
    }
}
