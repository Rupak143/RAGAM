package com.example.ragamfinal.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ragamfinal.CourseDetailActivity;
import com.example.ragamfinal.R;
import com.example.ragamfinal.utils.ApiHelper;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CoursesFragment extends Fragment {

    private TextView pageTitle;
    private EditText searchInput;
    private ListView coursesList;
    private ApiHelper apiHelper;
    private String categoryName;
    private int categoryId;
    private List<JSONObject> coursesDataList = new ArrayList<>();
    private List<Course> courseItems = new ArrayList<>();
    private Set<Integer> completedCourseIds = new HashSet<>();
    private CourseAdapter currentAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_courses, container, false);

        Bundle args = getArguments();
        if (args != null) {
            categoryId = args.getInt("category_id", 0);
            categoryName = args.getString("category_name");
        }

        apiHelper = new ApiHelper(requireContext());

        initViews(view);
        setupToggleButtons(view);
        loadCourses();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (apiHelper != null && !courseItems.isEmpty()) {
            refreshCompletionStatus();
        }
    }

    private void refreshCompletionStatus() {
        JSONObject user = apiHelper.getUserSession();
        if (user == null || !"student".equals(user.optString("user_type", ""))) return;
        int studentId = user.optInt("user_id", user.optInt("id", 0));
        if (studentId <= 0) return;

        apiHelper.getCompletedCourses(studentId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    Set<Integer> newCompletedIds = new HashSet<>();
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.has("data")) {
                        JSONArray completedArray = jsonResponse.getJSONArray("data");
                        for (int i = 0; i < completedArray.length(); i++) {
                            JSONObject completed = completedArray.getJSONObject(i);
                            int cId = completed.optInt("course_id", 0);
                            if (cId > 0) newCompletedIds.add(cId);
                        }
                    }

                    if (!newCompletedIds.equals(completedCourseIds)) {
                        completedCourseIds = newCompletedIds;
                        for (Course c : courseItems) {
                            c.isCompleted = completedCourseIds.contains(c.courseId);
                        }
                        if (getActivity() != null && currentAdapter != null) {
                            getActivity().runOnUiThread(() -> currentAdapter.notifyDataSetChanged());
                        }
                    }
                } catch (Exception e) {
                    Log.e("CoursesFragment", "Error refreshing completion status", e);
                }
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void initViews(View view) {
        pageTitle = view.findViewById(R.id.page_title);
        searchInput = view.findViewById(R.id.search_input);
        coursesList = view.findViewById(R.id.courses_list);

        if (categoryName != null && !categoryName.isEmpty()) {
            pageTitle.setText(categoryName + " Courses");
        } else {
            pageTitle.setText("All Courses");
        }
    }

    private void setupToggleButtons(View view) {
        view.findViewById(R.id.toggle_buttons).setVisibility(View.GONE);
    }

    private void loadCourses() {
        List<Course> loadingCourses = new ArrayList<>();
        loadingCourses.add(new Course(0, "Loading...", "Connecting to server", "", "SYSTEM", "⏳", false));
        currentAdapter = new CourseAdapter(loadingCourses);
        coursesList.setAdapter(currentAdapter);
        pageTitle.setText("Loading Courses...");

        loadActualCourses();
    }

    private void loadActualCourses() {
        JSONObject user = apiHelper.getUserSession();
        if (user != null && "student".equals(user.optString("user_type", ""))) {
            int studentId = user.optInt("user_id", user.optInt("id", 0));
            if (studentId > 0) {
                loadEnrolledThenCourses(studentId);
                return;
            }
        }
        loadAllCourses();
    }

    private void loadEnrolledThenCourses(int studentId) {
        apiHelper.getCompletedCourses(studentId, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    if (jsonResponse.has("data")) {
                        JSONArray completedArray = jsonResponse.getJSONArray("data");
                        for (int i = 0; i < completedArray.length(); i++) {
                            JSONObject completed = completedArray.getJSONObject(i);
                            int courseId = completed.optInt("course_id", 0);
                            if (courseId > 0) {
                                completedCourseIds.add(courseId);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("CoursesFragment", "Error parsing completed courses", e);
                }
                loadAllCourses();
            }

            @Override
            public void onError(String error) {
                loadAllCourses();
            }
        });
    }

    private void loadAllCourses() {
        Log.d("CoursesFragment", "Starting to load courses... Category ID: " + categoryId);

        if (categoryId > 0) {
            apiHelper.getAllCourses(categoryId, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    handleCoursesResponse(response);
                }

                @Override
                public void onError(String error) {
                    handleCoursesError(error);
                }
            });
        } else {
            apiHelper.getAllCourses(new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(String response) {
                    handleCoursesResponse(response);
                }

                @Override
                public void onError(String error) {
                    handleCoursesError(error);
                }
            });
        }
    }

    private void handleCoursesResponse(String response) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            try {
                Log.d("CoursesFragment", "Courses response: " + response);
                JSONObject jsonResponse = new JSONObject(response);

                boolean isSuccess = false;
                if (jsonResponse.has("status")) {
                    String status = jsonResponse.getString("status");
                    isSuccess = "success".equals(status);
                } else if (jsonResponse.has("success")) {
                    isSuccess = jsonResponse.getBoolean("success");
                }

                Log.d("CoursesFragment", "Is success: " + isSuccess);

                if (isSuccess && jsonResponse.has("data")) {
                    JSONArray coursesArray = jsonResponse.getJSONArray("data");
                    Log.d("CoursesFragment", "Found " + coursesArray.length() + " courses");
                    courseItems.clear();
                    coursesDataList.clear();

                    for (int i = 0; i < coursesArray.length(); i++) {
                        JSONObject courseJson = coursesArray.getJSONObject(i);
                        coursesDataList.add(courseJson);

                        int courseId = courseJson.optInt("course_id", 0);
                        String courseTitle = courseJson.optString("course_title",
                                            courseJson.optString("title", "Untitled Course"));
                        String teacherName = courseJson.optString("teacher_name", "Unknown Teacher");
                        int coursePrice = courseJson.optInt("course_price",
                                        courseJson.optInt("price", 0));
                        String priceStr = coursePrice == 0 || courseJson.optInt("is_free", 0) == 1
                                        ? "Free" : "₹" + coursePrice;
                        String difficultyLevel = courseJson.optString("difficulty_level", "beginner");
                        String rating = courseJson.optString("rating_formatted",
                                      String.format("%.1f★", courseJson.optDouble("rating", 4.5)));

                        boolean isCompleted = completedCourseIds.contains(courseId);

                        courseItems.add(new Course(courseId, courseTitle, teacherName, priceStr, difficultyLevel, rating, isCompleted));
                    }

                    if (courseItems.isEmpty()) {
                        showErrorMessage("No courses found in this category");
                    } else {
                        currentAdapter = new CourseAdapter(courseItems);
                        coursesList.setAdapter(currentAdapter);

                        coursesList.setOnItemClickListener((parent, view, position, id) -> {
                            if (courseItems.get(position).isCompleted) return;
                            JSONObject courseData = coursesDataList.get(position);
                            Intent intent = new Intent(requireContext(), CourseDetailActivity.class);
                            intent.putExtra("course_data", courseData.toString());
                            startActivity(intent);
                        });

                        if (categoryName != null && !categoryName.isEmpty()) {
                            pageTitle.setText(categoryName + " (" + courseItems.size() + ")");
                        } else {
                            pageTitle.setText("Courses (" + courseItems.size() + ")");
                        }
                        Log.d("CoursesFragment", "Loaded " + courseItems.size() + " courses");
                    }
                } else {
                    showErrorMessage("API returned error status");
                }
            } catch (Exception e) {
                Log.e("CoursesFragment", "Error parsing courses", e);
                showErrorMessage("Error parsing response: " + e.getMessage());
            }
        });
    }

    private void handleCoursesError(String error) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            Log.e("CoursesFragment", "Error loading courses: " + error);
            showErrorMessage("Network error: " + error);
        });
    }

    private void showErrorMessage(String message) {
        List<Course> courses = new ArrayList<>();
        courses.add(new Course(0, "Unable to Load Courses", message, "", "SYSTEM", "⚠ Error", false));
        currentAdapter = new CourseAdapter(courses);
        coursesList.setAdapter(currentAdapter);
        pageTitle.setText("Courses - Error");
    }

    private static class Course {
        int courseId;
        String title, teacher, price, level, rating;
        boolean isCompleted;

        Course(int courseId, String title, String teacher, String price, String level, String rating, boolean isCompleted) {
            this.courseId = courseId;
            this.title = title;
            this.teacher = teacher;
            this.price = price;
            this.level = level;
            this.rating = rating;
            this.isCompleted = isCompleted;
        }
    }

    private class CourseAdapter extends BaseAdapter {
        private List<Course> courses;

        CourseAdapter(List<Course> courses) {
            this.courses = courses;
        }

        @Override
        public int getCount() {
            return courses.size();
        }

        @Override
        public Object getItem(int position) {
            return courses.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_course, parent, false);
            }

            Course course = courses.get(position);

            TextView titleView = convertView.findViewById(R.id.course_title);
            TextView teacherView = convertView.findViewById(R.id.teacher_name);
            TextView priceView = convertView.findViewById(R.id.course_price);
            TextView levelView = convertView.findViewById(R.id.difficulty_level);
            TextView completedBadge = convertView.findViewById(R.id.completed_badge);

            titleView.setText(course.title);
            teacherView.setText(course.teacher);
            priceView.setText(course.price);
            levelView.setText(course.level);

            if (course.isCompleted) {
                completedBadge.setVisibility(View.VISIBLE);
                completedBadge.getBackground().setColorFilter(0xFF388E3C, android.graphics.PorterDuff.Mode.SRC_IN);
            } else {
                completedBadge.setVisibility(View.GONE);
                completedBadge.getBackground().clearColorFilter();
            }

            return convertView;
        }
    }
}
