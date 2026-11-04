package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView tvGreeting;
    private TextView tvStudentInfo;
    private LinearLayout layoutUpcomingSubject1;
    private LinearLayout layoutUpcomingSubject2;
    private RelativeLayout cardLMS;
    private RelativeLayout cardFacility;
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        setupBottomNavigation();

        tvGreeting = findViewById(R.id.tvGreeting);
        tvStudentInfo = findViewById(R.id.tvStudentInfo);
        layoutUpcomingSubject1 = findViewById(R.id.layout_upcoming_subject_1); // 구현전
        layoutUpcomingSubject2 = findViewById(R.id.layout_upcoming_subject_2); // 구현전
        cardLMS = findViewById(R.id.cardLMS);
        cardFacility = findViewById(R.id.cardFacility);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        cardLMS.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LectureListActivity.class);
            startActivity(intent);
        });

        cardFacility.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FacilityActivity.class);
            startActivity(intent);
        });

        UserDataLoader();
    }


    private void loadUpcomingTasks() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("Users").document(uid).collection("myTasks")
                .whereEqualTo("isDone", false)
                .whereEqualTo("type", "assignment")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> tasks = querySnapshot.getDocuments();
                    List<Object[]> taskInfoList = new ArrayList<>(); // [endAt, title, desc, refPath]
                    int total = tasks.size();
                    int[] loadedCount = {0}; // 콜백 완료 카운터

                    if (total == 0) return;

                    Date now = new Date();

                    for (DocumentSnapshot task : tasks) {
                        String assignmentRefPath = task.getString("refPath");
                        if (assignmentRefPath == null) {
                            loadedCount[0]++;
                            continue;
                        }

                        db.document(assignmentRefPath).get()
                                .addOnSuccessListener(assignmentDoc -> {
                                    if (assignmentDoc.exists()) {
                                        Date endAt = assignmentDoc.getDate("endAt");
                                        String courseName = assignmentDoc.getString("courseName");
                                        String title = assignmentDoc.getString("title");

                                        // 현재 시간 이후인 것만 (마감 안 된 것)
                                        if (endAt != null && endAt.after(now)) {
                                            taskInfoList.add(new Object[]{endAt, courseName,title, assignmentRefPath});
                                        }
                                    }


                                    loadedCount[0]++;

                                    // 모든 로드 완료 시 정렬 후 표시
                                    if (loadedCount[0] == total) {
                                        // endAt 기준 오름차순 정렬 (가까운 순)
                                        taskInfoList.sort((a, b) -> ((Date) a[0]).compareTo((Date) b[0]));

                                        for (int i = 0; i < Math.min(2, taskInfoList.size()); i++) {
                                            Object[] info = taskInfoList.get(i);
                                            String courseName = (String) info[1];
                                            String title = (String) info[2];
                                            String refPath = (String) info[3];
                                            setUpcomingSubjectCard(i, courseName, title, refPath);
                                        }
                                    }
                                });
                    }
                });
    }

    private void setUpcomingSubjectCard(int index, String title, String desc, String refPath) {
        LinearLayout layout = (index == 0) ? layoutUpcomingSubject1 : layoutUpcomingSubject2;
        layout.setVisibility(View.VISIBLE);

        TextView tvTitle = (TextView) layout.getChildAt(0);
        TextView tvDesc = (TextView) layout.getChildAt(1);

        tvTitle.setText(title);
        tvDesc.setText(desc);

        layout.setOnClickListener(v -> {
            // "/Courses/2026_1_CSE101_A/assignments/assignment_001" 에서 courseId 파싱
            String[] parts = refPath.split("/");
            String courseId = parts[2];

            Intent intent = new Intent(this, SubjectMainActivity.class);
            intent.putExtra("courseId", courseId);
            startActivity(intent);
        });
    }


    public void UserDataLoader() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 현재 로그인된 사용자의 인스턴스에서 UID를 직접 가져옴
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();

            // 가져온 UID로 Firestore에서 데이터 읽기
            db.collection("Users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String studentId = documentSnapshot.getString("studentID");
                            String major = documentSnapshot.getString("major");
                            String grade = documentSnapshot.getString("grade");
                            tvGreeting.setText(name + "님,\n안녕하세요!");
                            tvStudentInfo.setText(studentId + " / " + major + " / " + grade + "학년");;
                        }
                    });
            loadUpcomingTasks();
        }
    }
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.include_bottom_nav);
        bottomNavigationView.setSelectedItemId(R.id.tab_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.tab_home) {
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