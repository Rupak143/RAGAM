package com.example.ragamfinal;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    
    private TextView chatTitle;
    private ListView chatListView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageView backButton;
    
    private List<ChatMessage> messagesList;
    private ChatAdapter chatAdapter;
    
    private String mentorName;
    private int mentorId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        initViews();
        loadIntentData();
        setupDemoMessages();
        setupClickListeners();
    }
    
    private void initViews() {
        chatTitle = findViewById(R.id.chat_title);
        chatListView = findViewById(R.id.chat_messages_list);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        backButton = findViewById(R.id.back_button);
        
        messagesList = new ArrayList<>();
        chatAdapter = new ChatAdapter();
        chatListView.setAdapter(chatAdapter);
    }
    
    private void loadIntentData() {
        mentorId = getIntent().getIntExtra("mentor_id", 0);
        mentorName = getIntent().getStringExtra("mentor_name");
        
        if (mentorName != null) {
            chatTitle.setText(mentorName);
        } else {
            chatTitle.setText("Chat");
        }
    }
    
    private void setupDemoMessages() {
        // Add some demo messages to show chat functionality
        messagesList.add(new ChatMessage(
            "Hello! Welcome to the mentoring platform. How can I help you today?", 
            false, // from mentor
            System.currentTimeMillis() - 3600000 // 1 hour ago
        ));
        
        messagesList.add(new ChatMessage(
            "Hi! I'm interested in learning more about your courses.", 
            true, // from student
            System.currentTimeMillis() - 3000000 // 50 minutes ago
        ));
        
        messagesList.add(new ChatMessage(
            "That's great! I offer courses in classical music, contemporary styles, and music theory. Which area interests you most?", 
            false, // from mentor
            System.currentTimeMillis() - 2400000 // 40 minutes ago
        ));
        
        messagesList.add(new ChatMessage(
            "I'm particularly interested in classical music. What level would be suitable for a beginner?", 
            true, // from student
            System.currentTimeMillis() - 1800000 // 30 minutes ago
        ));
        
        messagesList.add(new ChatMessage(
            "Perfect! My 'Classical Music Fundamentals' course is designed specifically for beginners. It covers basic theory, scales, and simple pieces to get you started.", 
            false, // from mentor
            System.currentTimeMillis() - 1200000 // 20 minutes ago
        ));
        
        chatAdapter.notifyDataSetChanged();
        scrollToBottom();
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        sendButton.setOnClickListener(v -> sendMessage());
        
        messageInput.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }
    
    private void sendMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (!messageText.isEmpty()) {
            // Add user message
            messagesList.add(new ChatMessage(messageText, true, System.currentTimeMillis()));
            messageInput.setText("");
            chatAdapter.notifyDataSetChanged();
            scrollToBottom();
            
            // Simulate mentor response after a short delay
            chatListView.postDelayed(() -> {
                addDemoResponse(messageText);
            }, 1500);
        }
    }
    
    private void addDemoResponse(String userMessage) {
        String response;
        
        // Simple demo responses based on keywords
        String lowerMessage = userMessage.toLowerCase();
        if (lowerMessage.contains("price") || lowerMessage.contains("cost") || lowerMessage.contains("fee")) {
            response = "The course pricing varies depending on the level and duration. Most of my beginner courses start from ₹2000. Would you like me to send you detailed pricing information?";
        } else if (lowerMessage.contains("schedule") || lowerMessage.contains("time") || lowerMessage.contains("when")) {
            response = "I offer flexible scheduling options. We can arrange sessions based on your availability. What time slots work best for you?";
        } else if (lowerMessage.contains("experience") || lowerMessage.contains("qualification")) {
            response = "I have over 10 years of experience teaching music and hold a Master's degree in Classical Music. I've helped hundreds of students achieve their musical goals.";
        } else if (lowerMessage.contains("demo") || lowerMessage.contains("trial") || lowerMessage.contains("sample")) {
            response = "Absolutely! I offer a free 30-minute trial session for all new students. This helps us understand your current level and learning goals. Would you like to schedule one?";
        } else if (lowerMessage.contains("thank") || lowerMessage.contains("thanks")) {
            response = "You're very welcome! I'm here to help you on your musical journey. Feel free to ask any other questions you might have.";
        } else {
            response = "That's a great question! I'd be happy to discuss this further. Would you like to schedule a call to talk about your specific learning goals and how I can help you achieve them?";
        }
        
        messagesList.add(new ChatMessage(response, false, System.currentTimeMillis()));
        chatAdapter.notifyDataSetChanged();
        scrollToBottom();
    }
    
    private void scrollToBottom() {
        if (messagesList.size() > 0) {
            chatListView.post(() -> chatListView.setSelection(messagesList.size() - 1));
        }
    }
    
    private class ChatAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return messagesList.size();
        }
        
        @Override
        public Object getItem(int position) {
            return messagesList.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ChatMessage message = messagesList.get(position);
            
            if (message.isFromUser) {
                convertView = getLayoutInflater().inflate(R.layout.item_chat_message_sent, parent, false);
            } else {
                convertView = getLayoutInflater().inflate(R.layout.item_chat_message_received, parent, false);
            }
            
            TextView messageText = convertView.findViewById(R.id.message_text);
            TextView messageTime = convertView.findViewById(R.id.message_time);
            
            messageText.setText(message.text);
            
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            messageTime.setText(timeFormat.format(new Date(message.timestamp)));
            
            return convertView;
        }
    }
    
    private static class ChatMessage {
        String text;
        boolean isFromUser;
        long timestamp;
        
        ChatMessage(String text, boolean isFromUser, long timestamp) {
            this.text = text;
            this.isFromUser = isFromUser;
            this.timestamp = timestamp;
        }
    }
}
