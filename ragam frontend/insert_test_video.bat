@echo off
echo === Inserting Test Video Data ===
mysql -u root -P 3306 ragamfinal -e "SELECT user_id, email FROM users WHERE email = 'mohan@gmail.com';"
mysql -u root -P 3306 ragamfinal -e "SELECT enrollment_id, student_id, course_id FROM enrollments WHERE student_id = (SELECT user_id FROM users WHERE email = 'mohan@gmail.com');"
mysql -u root -P 3306 ragamfinal -e "SELECT lesson_id, course_id, lesson_title FROM course_lessons WHERE course_id IN (SELECT course_id FROM enrollments WHERE student_id = (SELECT user_id FROM users WHERE email = 'mohan@gmail.com'));"
echo.
echo Inserting test video...
mysql -u root -P 3306 ragamfinal -e "INSERT INTO course_lessons (course_id, lesson_title, lesson_description, video_url, lesson_order, duration_minutes, is_free) SELECT course_id, 'Test Video 1', 'Test video description', 'uploads/videos/test1.mp4', 1, 10, 1 FROM enrollments WHERE student_id = (SELECT user_id FROM users WHERE email = 'mohan@gmail.com') AND course_id NOT IN (SELECT DISTINCT course_id FROM course_lessons) LIMIT 1;"
echo.
echo Checking videos again...
mysql -u root -P 3306 ragamfinal -e "SELECT lesson_id, course_id, lesson_title, video_url FROM course_lessons WHERE course_id IN (SELECT course_id FROM enrollments WHERE student_id = (SELECT user_id FROM users WHERE email = 'mohan@gmail.com'));"
pause
