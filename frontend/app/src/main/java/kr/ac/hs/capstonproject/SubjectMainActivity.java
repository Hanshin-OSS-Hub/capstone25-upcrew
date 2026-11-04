package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

public class SubjectMainActivity extends AppCompatActivity {

    private Button btnNotice, btnMaterial, btnAssignment, btnClass;
    private LinearLayout layoutNotice, layoutMaterial, layoutAssignment, layoutClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_subject_main);
        setupBottomNavigation();

        View header = findViewById(R.id.include_header);
        header.findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        String courseId = getIntent().getStringExtra("courseId");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("Courses").document(courseId).get()
                .addOnSuccessListener(courseDoc -> {
                    String courseName = courseDoc.getString("courseName");
                    String professor = courseDoc.getString("professor");
                    ((TextView) header.findViewById(R.id.tv_subject_name)).setText(courseName);

                    loadNotices(db, courseId, professor);
                    loadMaterials(db, courseId, professor);
                    loadAssignments(db, courseId, uid);
                    loadClasses(db, courseId, uid);
                });

        btnNotice     = findViewById(R.id.btn_notice);
        btnMaterial   = findViewById(R.id.btn_material);
        btnAssignment = findViewById(R.id.btn_assignment);
        btnClass      = findViewById(R.id.btn_class);

        layoutNotice     = findViewById(R.id.layout_notice);
        layoutMaterial   = findViewById(R.id.layout_material);
        layoutAssignment = findViewById(R.id.layout_assignment);
        layoutClass      = findViewById(R.id.layout_class);

        setActiveTab(btnMaterial);

        btnNotice.setOnClickListener(v -> {
            showLayout(layoutNotice);
            setActiveTab(btnNotice);
        });
        btnMaterial.setOnClickListener(v -> {
            showLayout(layoutMaterial);
            setActiveTab(btnMaterial);
        });
        btnAssignment.setOnClickListener(v -> {
            showLayout(layoutAssignment);
            setActiveTab(btnAssignment);
        });
        btnClass.setOnClickListener(v -> {
            showLayout(layoutClass);
            setActiveTab(btnClass);
        });
    }

    private void loadMaterials(FirebaseFirestore db, String courseId, String professor) {
        db.collection("Courses").document(courseId).collection("materials")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    layoutMaterial.removeAllViews();
                    for (var doc : querySnapshot.getDocuments()) {
                        String title = doc.getString("title");
                        com.google.firebase.Timestamp ts = doc.getTimestamp("createdAt");
                        String date = ts != null ? new java.text.SimpleDateFormat("yy.MM.dd").format(ts.toDate()) : "";

                        View itemView = getLayoutInflater().inflate(R.layout.item_subject_material, layoutMaterial, false);
                        ((TextView) itemView.findViewById(R.id.tv_material_title)).setText(title);
                        ((TextView) itemView.findViewById(R.id.tv_material_info)).setText("작성자: " + professor + " | " + date);

                        itemView.setOnClickListener(v -> {
                            android.content.Intent intent = new android.content.Intent(this, SubjectMaterialDetailActivity.class);
                            intent.putExtra("courseId", courseId);
                            intent.putExtra("materialId", doc.getId());
                            intent.putExtra("professor", professor);
                            startActivity(intent);
                        });
                        layoutMaterial.addView(itemView);
                    }
                });

    }

    private void loadAssignments(FirebaseFirestore db, String courseId, String uid) {
        db.collection("Users").document(uid).collection("myTasks")
                .whereEqualTo("type", "assignment")
                .get()
                .addOnSuccessListener(taskSnapshot -> {

                    java.util.Map<String, Boolean> doneMap = new java.util.HashMap<>();

                    for (var taskDoc : taskSnapshot.getDocuments()) {
                        String refPath = taskDoc.getString("refPath"); // 여기 중요!
                        Boolean isDone = taskDoc.getBoolean("isDone");

                        if (refPath != null) {
                            doneMap.put(refPath, isDone != null && isDone);
                        }
                    }

                    db.collection("Courses").document(courseId).collection("assignments")
                            .orderBy("startAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(assignmentSnapshot -> {
                                layoutAssignment.removeAllViews();

                                for (var doc : assignmentSnapshot.getDocuments()) {
                                    String title = doc.getString("title");

                                    String fullPath = "/Courses/" + courseId + "/assignments/" + doc.getId();

                                    boolean isDone = doneMap.getOrDefault(fullPath, false);

                                    java.text.SimpleDateFormat sdf =
                                            new java.text.SimpleDateFormat("yy.MM.dd", java.util.Locale.KOREA);

                                    String startStr = doc.getTimestamp("startAt") != null
                                            ? sdf.format(doc.getTimestamp("startAt").toDate())
                                            : "";

                                    String endStr = doc.getTimestamp("endAt") != null
                                            ? sdf.format(doc.getTimestamp("endAt").toDate())
                                            : "";

                                    String dateStr = startStr + " ~ " + endStr;

                                    View itemView = getLayoutInflater().inflate(
                                            isDone
                                                    ? R.layout.item_subject_assignment_done
                                                    : R.layout.item_subject_assignment_undone,
                                            layoutAssignment,
                                            false
                                    );

                                    ((TextView) itemView.findViewById(R.id.tv_assignment_title))
                                            .setText(title != null ? title : "");

                                    ((TextView) itemView.findViewById(R.id.tv_assignment_date))
                                            .setText(dateStr);

                                    itemView.setOnClickListener(v -> {

                                        Intent intent = new Intent(this, SubjectAssignmentDetailActivity.class);

                                        intent.putExtra("courseId", courseId);

                                        intent.putExtra("assignmentId", doc.getId());

                                        startActivity(intent);

                                    });

                                    layoutAssignment.addView(itemView);
                                }
                            });
                });
    }

    private void loadNotices(FirebaseFirestore db, String courseId, String professor) {
        db.collection("Courses").document(courseId).collection("notices")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    layoutNotice.removeAllViews();
                    for (var doc : querySnapshot.getDocuments()) {
                        String title = doc.getString("title");
                        com.google.firebase.Timestamp ts = doc.getTimestamp("createdAt");
                        String date = ts != null ? new java.text.SimpleDateFormat("yy.MM.dd").format(ts.toDate()) : "";

                        View itemView = getLayoutInflater().inflate(R.layout.item_subject_notice, layoutNotice, false);
                        ((TextView) itemView.findViewById(R.id.tv_notice_title)).setText(title);
                        ((TextView) itemView.findViewById(R.id.tv_notice_info)).setText("작성자: " + professor + " | " + date);

                        // 클릭 시 상세 (추후 구현)
                        itemView.setOnClickListener(v -> {
                            android.content.Intent intent = new android.content.Intent(this, SubjectNoticeDetailActivity.class);
                            intent.putExtra("courseId", courseId);
                            intent.putExtra("noticeId", doc.getId());
                            intent.putExtra("professor", professor);
                            startActivity(intent);
                        });
                        layoutNotice.addView(itemView);
                    }
                });
    }

    private void loadClasses(FirebaseFirestore db, String courseId, String uid) {
        db.collection("Users").document(uid).collection("myTasks")
                .whereEqualTo("type", "class")
                .get()
                .addOnSuccessListener(taskSnapshot -> {

                    // refPath → status 맵
                    java.util.Map<String, String> statusMap = new java.util.HashMap<>();
                    for (var taskDoc : taskSnapshot.getDocuments()) {
                        android.util.Log.d("DEBUG", "문서ID: " + taskDoc.getId());
                        android.util.Log.d("DEBUG", "전체데이터: " + taskDoc.getData());
                        String refPath = taskDoc.getString("classRefPath");
                        String status = taskDoc.getString("status");
                        android.util.Log.d("DEBUG", "refPath: " + refPath + " status: " + status); // 여기
                        if (refPath != null) statusMap.put(refPath, status);
                    }

                    db.collection("Courses").document(courseId).collection("classes")
                            .orderBy("startAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(classSnapshot -> {
                                layoutClass.removeAllViews();

                                for (var doc : classSnapshot.getDocuments()) {
                                    String title = doc.getString("title");
                                    Long duration = doc.getLong("durationSeconds");
                                    String durationStr = duration != null ? duration / 60 + "분" : "";
                                    String fullPath = "/Courses/" + courseId + "/classes/" + doc.getId();
                                    android.util.Log.d("DEBUG", "fullPath: " + fullPath); // 로그
                                    String status = statusMap.getOrDefault(fullPath, "not_started");

                                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yy.MM.dd");
                                    String startStr = doc.getTimestamp("startAt") != null ? sdf.format(doc.getTimestamp("startAt").toDate()) : "";
                                    String endStr   = doc.getTimestamp("endAt")   != null ? sdf.format(doc.getTimestamp("endAt").toDate())   : "";

                                    View itemView = getLayoutInflater().inflate(R.layout.item_subject_class, layoutClass, false);

                                    ((TextView) itemView.findViewById(R.id.tv_class_title)).setText(title);
                                    ((TextView) itemView.findViewById(R.id.tv_class_info)).setText("온라인 | " + durationStr);
                                    ((TextView) itemView.findViewById(R.id.tv_class_date)).setText(startStr + " ~ " + endStr);

                                    TextView tvState = itemView.findViewById(R.id.tv_class_state);
                                    switch (status) {
                                        case "done":
                                            tvState.setText("출석완료");
                                            tvState.setBackgroundResource(R.drawable.bg_assignment_done);
                                            break;
                                        case "in_progress":
                                            tvState.setText("진행중");
                                            tvState.setBackgroundResource(R.drawable.bg_assignment_undone);
                                            break;
                                        case "not_started":
                                            tvState.setText("미완료");
                                            tvState.setBackgroundResource(R.drawable.bg_assignment_undone);
                                            break;

                                        default:
                                            tvState.setText("???");
                                            tvState.setBackgroundResource(R.drawable.bg_assignment_undone);
                                            break;
                                    }

                                    // 수정 ...
                                    itemView.setOnClickListener(v -> {
                                        com.google.firebase.Timestamp startTimestamp = doc.getTimestamp("startAt");
                                        com.google.firebase.Timestamp endTimestamp = doc.getTimestamp("endAt");

                                        java.util.Date nowDate = new java.util.Date();

                                        if (startTimestamp != null && endTimestamp != null) {
                                            if (startTimestamp.toDate().after(endTimestamp.toDate())) {
                                                Toast.makeText(this, "강의 기간 설정이 잘못되었습니다.", Toast.LENGTH_SHORT).show();
                                                return;
                                            }
                                        }

                                        if (startTimestamp != null && nowDate.before(startTimestamp.toDate())) {
                                            Toast.makeText(this, "아직 수강 기간이 시작되지 않았습니다.", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        if (endTimestamp != null && nowDate.after(endTimestamp.toDate())) {
                                            Toast.makeText(this, "수강 기간이 종료되어 입장할 수 없습니다.", Toast.LENGTH_SHORT).show();
                                            return;
                                        }

                                        Intent intent = new Intent(this, SubjectClassDetailActivity.class);
                                        intent.putExtra("courseId", courseId);
                                        intent.putExtra("classId", doc.getId());
                                        startActivity(intent);
                                    });
                                    layoutClass.addView(itemView);
                                }
                            });
                });
    }

    private void showLayout(LinearLayout target) {
        layoutNotice.setVisibility(View.GONE);
        layoutMaterial.setVisibility(View.GONE);
        layoutAssignment.setVisibility(View.GONE);
        layoutClass.setVisibility(View.GONE);
        target.setVisibility(View.VISIBLE);
    }
    private void setActiveTab(Button active) {
        Button[] allButtons = {btnNotice, btnMaterial, btnAssignment, btnClass};
        for (Button btn : allButtons) {
            if (btn == active) {
                // 선택됨: 보라 바탕 + 흰 글씨
                btn.setBackgroundTintList(getColorStateList(R.color.light_purple));
                btn.setTextColor(getColor(android.R.color.white));
            } else {
                // 미선택: 흰 바탕 + 검정 글씨
                btn.setBackgroundTintList(getColorStateList(android.R.color.white));
                btn.setTextColor(getColor(android.R.color.black));
            }
        }
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