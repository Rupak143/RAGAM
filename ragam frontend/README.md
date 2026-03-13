# Ragam Final - Music Learning Android App

## Project Overview

Ragam Final is a comprehensive music learning Android application built with Java and XML, featuring a complete backend API using PHP and MySQL. The app allows students to enroll in music courses and teachers to manage their courses, with features like video streaming, messaging, and profile management.

## Architecture

### Frontend (Android)
- **Language**: Java
- **UI**: XML layouts
- **Architecture**: Activity-based with API integration
- **Features**: 
  - User authentication (Student/Teacher)
  - Course browsing and enrollment
  - Video player for course content
  - Messaging system
  - Profile management
  - Bottom navigation with highlighting

### Backend (PHP/MySQL)
- **Language**: PHP
- **Database**: MySQL
- **Architecture**: RESTful API
- **Features**:
  - User authentication and session management
  - Course and lesson management
  - Enrollment system
  - Messaging system
  - File upload support

## Setup Instructions

### 1. Database Setup

1. **Install XAMPP/WAMP/LAMP** on your development machine
2. **Start Apache and MySQL** services
3. **Create Database**:
   ```sql
   CREATE DATABASE ragamfinal;
   ```
4. **Import Database Schema**:
   - Navigate to `app/src/main/res/Backend_setup/database_schema.sql`
   - Import this file into your MySQL database using phpMyAdmin or command line:
   ```bash
   mysql -u root -p ragamfinal < database_schema.sql
   ```

### 2. Backend API Setup

1. **Copy PHP Files**:
   - Copy the entire `Backend_setup/php_connection/` folder to your web server directory
   - For XAMPP: `C:/xampp/htdocs/ragam_api/`
   - For WAMP: `C:/wamp64/www/ragam_api/`

2. **Configure Database Connection**:
   - Open `config.php` in your web server directory
   - Update database credentials if necessary:
   ```php
   private $host = "localhost";
   private $username = "root";
   private $password = ""; // Your MySQL password
   private $database = "ragamfinal";
   ```

3. **Test API**:
   - Open your browser and go to: `http://localhost/ragam_api/courses.php?action=all_courses`
   - You should see a JSON response with sample courses

### 3. Android App Configuration

1. **Update API Base URL**:
   - Open `app/src/main/java/com/example/ragamfinal/utils/ApiHelper.java`
   - Update the BASE_URL to match your server:
   ```java
   private static final String BASE_URL = "http://YOUR_IP_ADDRESS/ragam_api/";
   ```
   - Replace `YOUR_IP_ADDRESS` with your computer's IP address (find using `ipconfig` on Windows or `ifconfig` on Mac/Linux)

2. **Android Permissions**:
   - The app already includes necessary internet permissions in `AndroidManifest.xml`

3. **Build and Run**:
   - Open the project in Android Studio
   - Sync Gradle files
   - Build and run the application

## Application Flow

### 1. App Launch
- **SplashActivity**: Entry point, checks user session
- **UserOptionActivity**: Allows user to choose Student or Teacher login

### 2. Student Flow
1. **StudentSignInActivity** / **StudentSignUpActivity**: Authentication
2. **StudentHomeActivity**: Main dashboard with course categories
3. **CoursesActivity**: Browse all available courses
4. **CourseDetailActivity**: View course details and enroll
5. **VideoPlayerActivity**: Watch course videos after enrollment
6. **InboxActivity**: View messages from teachers
7. **ProfileActivity**: Manage profile and logout

### 3. Teacher Flow
1. **TeacherSignInActivity** / **TeacherSignUpActivity**: Authentication
2. **TeacherHomeActivity**: Teacher dashboard
3. Additional teacher features can be added (course creation, student management)

### 4. Navigation Features
- **Bottom Navigation**: Home, Courses, Inbox, Profile with highlighting
- **Session Management**: Automatic login/logout with SharedPreferences
- **API Integration**: All data loaded from MySQL database via PHP APIs

## Key Features Implemented

### 🔐 Authentication System
- Separate login/signup for students and teachers
- Password hashing and validation
- Session management with auto-login

### 📚 Course Management
- Course listing with search and filtering
- Course details with teacher information
- Enrollment system with payment status tracking
- Video lessons with progress tracking

### 👥 User Management
- Profile viewing and editing
- Teacher/mentor directory
- User verification system

### 💬 Messaging System
- Inbox for student-teacher communication
- Message reading status
- Course-related messaging

### 🎥 Video Player
- Integrated video player for course content
- Lesson progress tracking
- Media controls and error handling

## Database Schema

### Core Tables
- **users**: Student and teacher profiles
- **categories**: Course categories (Vocal, Instrumental, etc.)
- **courses**: Course information and metadata
- **course_lessons**: Individual video lessons
- **enrollments**: Student course enrollments
- **messages**: Communication system
- **student_progress**: Lesson completion tracking

## API Endpoints

### Authentication
- `POST /auth.php?action=login` - User login
- `POST /auth.php?action=register` - User registration

### Courses
- `GET /courses.php?action=all_courses` - Get all courses
- `GET /courses.php?action=course_details&course_id=X` - Get course details
- `POST /courses.php?action=enroll` - Enroll in course

### Profile
- `GET /profile.php?action=profile&user_id=X` - Get user profile
- `GET /profile.php?action=mentors` - Get all teachers/mentors

### Messages
- `GET /messages.php?action=inbox&user_id=X` - Get user messages
- `POST /messages.php?action=send` - Send message

## Security Features

- Password hashing using PHP `password_hash()`
- SQL injection prevention using prepared statements
- Input validation and sanitization
- Session management for API access
- CORS headers for mobile app access

## Future Enhancements

1. **Payment Integration**: Add payment gateway for course enrollment
2. **Push Notifications**: Real-time notifications for messages and updates
3. **Offline Support**: Download videos for offline viewing
4. **Live Classes**: Video conferencing integration
5. **Assessment System**: Quizzes and assignments
6. **Social Features**: Student forums and discussions

## Troubleshooting

### Common Issues

1. **Connection Error**:
   - Check if XAMPP/WAMP services are running
   - Verify the API base URL in ApiHelper.java
   - Ensure your device/emulator can reach the server IP

2. **Database Error**:
   - Verify MySQL is running
   - Check database credentials in config.php
   - Ensure database schema is properly imported

3. **Login Issues**:
   - Check if sample data is inserted (there's a sample teacher in the schema)
   - Verify API endpoints are accessible

### Testing Credentials
Default teacher account (created in schema):
- Email: `teacher@ragam.com`
- Password: `password123` (you'll need to hash this and update in database)

## Contact

For any issues or questions, please refer to the code comments or create detailed error logs for debugging.

---

**Note**: This is a development setup. For production deployment, additional security measures, proper error handling, and performance optimizations should be implemented.
