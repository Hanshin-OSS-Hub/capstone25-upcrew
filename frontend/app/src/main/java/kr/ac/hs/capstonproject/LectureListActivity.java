package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LectureListActivity extends AppCompatActivity {

    private LinearLayout cardContainer;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lecture_list);
        setupBottomNavigation();

        cardContainer = findViewById(R.id.cardContainer);

        View header = findViewById(R.id.include_header);
        ((TextView) header.findViewById(R.id.tv_subject_name)).setText("강의리스트");
        header.findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadMyCourses();

    }

    private void loadMyCourses() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("Users").document(uid).collection("myCourses")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot.getDocuments()) {
                        String courseId = doc.getString("courseId");
                        if (courseId == null) continue;

                        db.collection("Courses").document(courseId).get()
                                .addOnSuccessListener(courseDoc -> {
                                    if (!courseDoc.exists()) return;

                                    String courseName = courseDoc.getString("courseName");
                                    String professor = courseDoc.getString("professor");
                                    String credit = courseDoc.getString("credit");
                                    String courseCode = courseDoc.getString("courseCode");
                                    String section = courseDoc.getString("section");


                                    View itemView = LayoutInflater.from(this)
                                            .inflate(R.layout.item_lecture_list, cardContainer, false);

                                    ((TextView) itemView.findViewById(R.id.tvCourseTitle)).setText(courseName + " (" + section + "반)");
                                    ((TextView) itemView.findViewById(R.id.tvCourseProfessor)).setText("교수명: " + professor);
                                    ((TextView) itemView.findViewById(R.id.tvCourseCredit)).setText("구분: " + credit + "학점");
                                    ((TextView) itemView.findViewById(R.id.tvCourseCode)).setText("학수번호: " + courseCode);

                                    itemView.findViewById(R.id.btnNext).setOnClickListener(v -> {
                                        Intent intent = new Intent(this, SubjectMainActivity.class);
                                        intent.putExtra("courseId", courseId);
                                        startActivity(intent);
                                    });
                                    cardContainer.addView(itemView);
                                });
                    }
                });
    }
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.include_bottom_nav);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.tab_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;

            } else if (itemId == R.id.tab_timetable) {
                startActivity(new Intent(this, CalendarActivity.class));
                finish();
                return true;

            } else if (itemId == R.id.tab_board) {
                startActivity(new Intent(this, NotificationActivity.class));
                finish();
                return true;

            } else if (itemId == R.id.tab_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            }

            return false;
        });
    }
}