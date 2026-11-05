package kr.ac.hs.capstonproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        setupBottomNavigation();

        UpCrewFirebaseMessagingService.createChannelIfNeeded(this);
        requestNotificationPermissionIfNeeded();

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


    // 할 일 후보: [endAt(Date), courseName, label, courseId]
    private final List<Object[]> upcomingCandidates = new ArrayList<>();

    private void loadUpcomingTasks() {
        if (mAuth.getCurrentUser() == null) {
            return;
        }
        String uid = mAuth.getCurrentUser().getUid();
        upcomingCandidates.clear();

        db.collection("Users").document(uid).collection("myCourses")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(myCoursesSnap -> {
                    List<String> courseIds = new ArrayList<>();
                    for (DocumentSnapshot d : myCoursesSnap.getDocuments()) {
                        String cid = d.getString("courseId");
                        if (cid != null) courseIds.add(cid);
                    }

                    if (courseIds.isEmpty()) return;

                    // 강의 하나당 "과제 조회"와 "강의 조회" 두 갈래가 끝나야 하므로 2배로 센다.
                    int[] remainingBranches = {courseIds.size() * 2};

                    for (String courseId : courseIds) {
                        db.collection("Courses").document(courseId).get()
                                .addOnSuccessListener(courseDoc -> {
                                    String courseName = courseDoc.getString("courseName");
                                    loadUpcomingAssignments(courseId, courseName, uid, remainingBranches);
                                    loadUpcomingClasses(courseId, courseName, uid, remainingBranches);
                                })
                                .addOnFailureListener(e -> {
                                    onBranchDone(remainingBranches);
                                    onBranchDone(remainingBranches);
                                });
                    }
                });
    }

    private void loadUpcomingAssignments(String courseId, String courseName, String uid, int[] remainingBranches) {
        db.collection("Courses").document(courseId).collection("assignments")
                .get()
                .addOnSuccessListener(assignSnap -> {
                    Date now = new Date();

                    List<DocumentSnapshot> pending = new ArrayList<>();
                    for (DocumentSnapshot aDoc : assignSnap.getDocuments()) {
                        com.google.firebase.Timestamp endAtTs = aDoc.getTimestamp("endAt");
                        if (endAtTs != null && endAtTs.toDate().after(now)) {
                            pending.add(aDoc);
                        }
                    }

                    if (pending.isEmpty()) {
                        onBranchDone(remainingBranches);
                        return;
                    }

                    int[] remainingAssignments = {pending.size()};
                    for (DocumentSnapshot aDoc : pending) {
                        db.collection("Courses").document(courseId)
                                .collection("assignments").document(aDoc.getId())
                                .collection("submissions").document(uid)
                                .get()
                                .addOnSuccessListener(subDoc -> {
                                    boolean submitted = subDoc.exists() && Boolean.TRUE.equals(subDoc.getBoolean("isSubmitted"));
                                    if (!submitted) {
                                        Date endAt = aDoc.getTimestamp("endAt").toDate();
                                        upcomingCandidates.add(new Object[]{endAt, courseName, "[과제] " + aDoc.getString("title"), courseId});
                                    }
                                    if (--remainingAssignments[0] == 0) onBranchDone(remainingBranches);
                                })
                                .addOnFailureListener(e -> {
                                    if (--remainingAssignments[0] == 0) onBranchDone(remainingBranches);
                                });
                    }
                })
                .addOnFailureListener(e -> onBranchDone(remainingBranches));
    }

    private void loadUpcomingClasses(String courseId, String courseName, String uid, int[] remainingBranches) {
        db.collection("Courses").document(courseId).collection("classes")
                .get()
                .addOnSuccessListener(classSnap -> {
                    Date now = new Date();

                    List<DocumentSnapshot> pending = new ArrayList<>();
                    for (DocumentSnapshot cDoc : classSnap.getDocuments()) {
                        com.google.firebase.Timestamp endAtTs = cDoc.getTimestamp("endAt");
                        if (endAtTs != null && endAtTs.toDate().after(now)) {
                            pending.add(cDoc);
                        }
                    }

                    if (pending.isEmpty()) {
                        onBranchDone(remainingBranches);
                        return;
                    }

                    int[] remainingClasses = {pending.size()};
                    for (DocumentSnapshot cDoc : pending) {
                        db.collection("Courses").document(courseId)
                                .collection("classes").document(cDoc.getId())
                                .collection("progress").document(uid)
                                .get()
                                .addOnSuccessListener(progressDoc -> {
                                    boolean completed = progressDoc.exists() && Boolean.TRUE.equals(progressDoc.getBoolean("completed"));
                                    if (!completed) {
                                        Date endAt = cDoc.getTimestamp("endAt").toDate();
                                        upcomingCandidates.add(new Object[]{endAt, courseName, "[온라인강의] " + cDoc.getString("title"), courseId});
                                    }
                                    if (--remainingClasses[0] == 0) onBranchDone(remainingBranches);
                                })
                                .addOnFailureListener(e -> {
                                    if (--remainingClasses[0] == 0) onBranchDone(remainingBranches);
                                });
                    }
                })
                .addOnFailureListener(e -> onBranchDone(remainingBranches));
    }

    private void onBranchDone(int[] remainingBranches) {
        remainingBranches[0]--;
        if (remainingBranches[0] == 0) {
            upcomingCandidates.sort((a, b) -> ((Date) a[0]).compareTo((Date) b[0]));
            for (int i = 0; i < Math.min(2, upcomingCandidates.size()); i++) {
                Object[] info = upcomingCandidates.get(i);
                String courseName = (String) info[1];
                String label = (String) info[2];
                String courseId = (String) info[3];
                setUpcomingSubjectCard(i, courseName, label, courseId);
            }
        }
    }

    private void setUpcomingSubjectCard(int index, String title, String desc, String courseId) {
        LinearLayout layout = (index == 0) ? layoutUpcomingSubject1 : layoutUpcomingSubject2;
        layout.setVisibility(View.VISIBLE);

        TextView tvTitle = (TextView) layout.getChildAt(0);
        TextView tvDesc = (TextView) layout.getChildAt(1);

        tvTitle.setText(title);
        tvDesc.setText(desc);

        layout.setOnClickListener(v -> {
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

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}