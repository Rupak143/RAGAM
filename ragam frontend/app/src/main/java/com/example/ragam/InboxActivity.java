package com.example.ragamfinal;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InboxActivity extends AppCompatActivity {
    
    private ListView messagesListView;
    private ImageView homeIcon, coursesIcon, inboxIcon, profileIcon;
    private TextView titleText;
    private ApiHelper apiHelper;
    private MessageAdapter messageAdapter;
    private List<JSONObject> messagesList;
    private JSONObject currentUser;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);
        
        // Initialize ApiHelper and get user session FIRST
        apiHelper = new ApiHelper(this);
        currentUser = apiHelper.getUserSession();
        
        initViews();
        setupBottomNavigation();
        loadMessages();
    }
    
    private void initViews() {
        messagesListView = findViewById(R.id.messages_list);
        homeIcon = findViewById(R.id.ic_home);
        coursesIcon = findViewById(R.id.ic_courses);
        inboxIcon = findViewById(R.id.ic_inbox);
        profileIcon = findViewById(R.id.ic_profile);
        titleText = findViewById(R.id.page_title);
        
        messagesList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messagesList);
        messagesListView.setAdapter(messageAdapter);
        
        // Handle message item clicks
        messagesListView.setOnItemClickListener((parent, view, position, id) -> {
            try {
                JSONObject message = messagesList.get(position);
                String senderName = message.getString("sender_name");
                int senderId = message.getInt("sender_id");
                
                // Open chat with the message sender
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("mentor_id", senderId);
                intent.putExtra("mentor_name", senderName);
                startActivity(intent);
                
            } catch (Exception e) {
                Log.e("InboxActivity", "Error opening message", e);
                Toast.makeText(this, "Error opening chat", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void setupBottomNavigation() {
        // Highlight inbox icon (current page)
        highlightBottomNavIcon(inboxIcon);
        
        // Hide courses icon for instructors
        if (currentUser != null) {
            try {
                String userType = currentUser.getString("user_type");
                if ("teacher".equals(userType)) {
                    coursesIcon.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e("InboxActivity", "Error checking user type", e);
            }
        }
        
        homeIcon.setOnClickListener(v -> {
            navigateToHome();
        });

        coursesIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, CoursesActivity.class);
            startActivity(intent);
        });

        inboxIcon.setOnClickListener(v -> {
            // Already on inbox
            highlightBottomNavIcon(inboxIcon);
        });

        profileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });
    }
    
    private void navigateToHome() {
        if (currentUser != null) {
            try {
                String userType = currentUser.getString("user_type");
                Intent intent;
                if ("teacher".equals(userType)) {
                    intent = new Intent(this, TeacherHomeActivity.class);
                } else {
                    intent = new Intent(this, MainContainerActivity.class);
                    intent.putExtra("fragment", "home");
                }
                startActivity(intent);
            } catch (Exception e) {
                // Default to student home if error
                Intent intent = new Intent(this, MainContainerActivity.class);
                intent.putExtra("fragment", "home");
                startActivity(intent);
            }
        } else {
            // No user session, redirect to login
            Intent intent = new Intent(this, UserOptionActivity.class);
            startActivity(intent);
            finish();
        }
    }
    
    private void loadMessages() {
        if (currentUser == null) {
            Toast.makeText(this, "Please login to view messages", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Load demo messages for demonstration
        loadDemoMessages();
        
        // Also try to load actual messages from server
        try {
            int userId = currentUser.getInt("user_id");
            
            apiHelper.getInboxMessages(userId, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    runOnUiThread(() -> {
                        try {
                            JSONObject jsonResponse = new JSONObject(response);
                            String status = jsonResponse.getString("status");
                            
                            if ("success".equals(status)) {
                                JSONArray messagesArray = jsonResponse.getJSONArray("data");
                                
                                for (int i = 0; i < messagesArray.length(); i++) {
                                    messagesList.add(messagesArray.getJSONObject(i));
                                }
                                
                                messageAdapter.notifyDataSetChanged();
                                titleText.setText("Inbox (" + messagesList.size() + ")");
                            }
                        } catch (Exception e) {
                            Log.e("InboxActivity", "Error parsing messages", e);
                        }
                    });
                }
                
                @Override
                public void onError(String error) {
                    Log.d("InboxActivity", "Server messages not available, using demo messages");
                }
            });
            
        } catch (Exception e) {
            Log.e("InboxActivity", "Error loading messages", e);
        }
    }
    
    private void loadDemoMessages() {
        try {
            messagesList.clear();
            
            // Demo message from mentor
            JSONObject message1 = new JSONObject();
            message1.put("sender_id", 101);
            message1.put("sender_name", "Priya Sharma (Mentor)");
            message1.put("subject", "Welcome to Classical Music Course");
            message1.put("message_text", "Welcome to my classical music course! I'm excited to be your mentor. Feel free to ask any questions you might have about the course content or schedule.");
            message1.put("sent_at", "2024-01-15 10:30:00");
            message1.put("is_read", false);
            messagesList.add(message1);
            
            // Demo message from another mentor
            JSONObject message2 = new JSONObject();
            message2.put("sender_id", 102);
            message2.put("sender_name", "Ravi Kumar (Mentor)");
            message2.put("subject", "Guitar Learning Progress");
            message2.put("message_text", "Hi! I noticed you've completed the first module of the guitar course. Great progress! Let's schedule a practice session to review your technique.");
            message2.put("sent_at", "2024-01-14 15:45:00");
            message2.put("is_read", true);
            messagesList.add(message2);
            
            // Demo message from support
            JSONObject message3 = new JSONObject();
            message3.put("sender_id", 103);
            message3.put("sender_name", "Support Team");
            message3.put("subject", "Course Enrollment Confirmation");
            message3.put("message_text", "Your enrollment in 'Advanced Music Theory' has been confirmed. You can now access all course materials and start learning!");
            message3.put("sent_at", "2024-01-13 09:15:00");
            message3.put("is_read", true);
            messagesList.add(message3);
            
            messageAdapter.notifyDataSetChanged();
            titleText.setText("Inbox (" + messagesList.size() + ")");
            
        } catch (Exception e) {
            Log.e("InboxActivity", "Error creating demo messages", e);
        }
    }
    
    private void highlightBottomNavIcon(ImageView selectedIcon) {
        // Reset all icons to default color
        homeIcon.setColorFilter(Color.GRAY);
        inboxIcon.setColorFilter(Color.GRAY);
        profileIcon.setColorFilter(Color.GRAY);
        
        // Highlight selected icon
        selectedIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
    }
    
    private static class MessageAdapter extends BaseAdapter {
        private Context context;
        private List<JSONObject> messages;
        private LayoutInflater inflater;
        private SimpleDateFormat dateFormat;
        
        public MessageAdapter(Context context, List<JSONObject> messages) {
            this.context = context;
            this.messages = messages;
            this.inflater = LayoutInflater.from(context);
            this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        }
        
        @Override
        public int getCount() {
            return messages.size();
        }
        
        @Override
        public Object getItem(int position) {
            return messages.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_message, parent, false);
                holder = new ViewHolder();
                holder.senderName = convertView.findViewById(R.id.sender_name);
                holder.messageSubject = convertView.findViewById(R.id.message_subject);
                holder.messagePreview = convertView.findViewById(R.id.message_preview);
                holder.messageDate = convertView.findViewById(R.id.message_date);
                holder.unreadIndicator = convertView.findViewById(R.id.unread_indicator);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            
            try {
                JSONObject message = messages.get(position);
                holder.senderName.setText(message.getString("sender_name"));
                
                String subject = message.optString("subject", "No Subject");
                holder.messageSubject.setText(subject);
                
                String messageText = message.getString("message_text");
                if (messageText.length() > 50) {
                    messageText = messageText.substring(0, 50) + "...";
                }
                holder.messagePreview.setText(messageText);
                
                // Format date
                try {
                    String sentAt = message.getString("sent_at");
                    // You might need to parse the date string properly based on your backend format
                    holder.messageDate.setText(sentAt.substring(0, 10)); // Simple date extraction
                } catch (Exception e) {
                    holder.messageDate.setText("");
                }
                
                // Show unread indicator
                boolean isRead = message.getBoolean("is_read");
                holder.unreadIndicator.setVisibility(isRead ? View.GONE : View.VISIBLE);
                
            } catch (Exception e) {
                Log.e("MessageAdapter", "Error binding message data", e);
            }
            
            return convertView;
        }
        
        private static class ViewHolder {
            TextView senderName;
            TextView messageSubject;
            TextView messagePreview;
            TextView messageDate;
            View unreadIndicator;
        }
    }
}
