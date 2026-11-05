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

import java.util.List;

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
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
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
        db.collection("Courses").document(courseId).collection("assignments")
                .orderBy("startAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(assignmentSnapshot -> {
                    List<com.google.firebase.firestore.DocumentSnapshot> docs = assignmentSnapshot.getDocuments();
                    if (docs.isEmpty()) {
                        layoutAssignment.removeAllViews();
                        return;
                    }

                    boolean[] doneFlags = new boolean[docs.size()];
                    int[] remaining = {docs.size()};

                    for (int i = 0; i < docs.size(); i++) {
                        int index = i;
                        var doc = docs.get(i);
                        db.collection("Courses").document(courseId)
                                .collection("assignments").document(doc.getId())
                                .collection("submissions").document(uid)
                                .get()
                                .addOnSuccessListener(subDoc -> {
                                    doneFlags[index] = subDoc.exists()
                                            && Boolean.TRUE.equals(subDoc.getBoolean("isSubmitted"));
                                    if (--remaining[0] == 0) renderAssignmentList(docs, doneFlags, courseId);
                                })
                                .addOnFailureListener(e -> {
                                    if (--remaining[0] == 0) renderAssignmentList(docs, doneFlags, courseId);
                                });
                    }
                });
    }

    // 쿼리 결과의 순서(최신순)를 그대로 유지하기 위해, 전부 조회가 끝난 뒤 한 번에 순서대로 그린다.
    private void renderAssignmentList(List<com.google.firebase.firestore.DocumentSnapshot> docs, boolean[] doneFlags, String courseId) {
        layoutAssignment.removeAllViews();
        for (int i = 0; i < docs.size(); i++) {
            renderAssignmentCard(docs.get(i), doneFlags[i], courseId);
        }
    }

    private void renderAssignmentCard(com.google.firebase.firestore.DocumentSnapshot doc, boolean isDone, String courseId) {
        String title = doc.getString("title");

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

        if (!isDone) {
            com.google.firebase.Timestamp endAtTs = doc.getTimestamp("endAt");
            boolean deadlinePassed = endAtTs != null && endAtTs.toDate().before(new java.util.Date());
            if (deadlinePassed) {
                ((TextView) itemView.findViewById(R.id.tv_assignment_state)).setText("미완료");
            }
        }

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(this, SubjectAssignmentDetailActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("assignmentId", doc.getId());
            startActivity(intent);
        });

        layoutAssignment.addView(itemView);
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
        db.collection("Courses").document(courseId).collection("classes")
                .orderBy("startAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(classSnapshot -> {
                    List<com.google.firebase.firestore.DocumentSnapshot> docs = classSnapshot.getDocuments();
                    if (docs.isEmpty()) {
                        layoutClass.removeAllViews();
                        return;
                    }

                    String[] statuses = new String[docs.size()];
                    int[] remaining = {docs.size()};

                    for (int i = 0; i < docs.size(); i++) {
                        int index = i;
                        var doc = docs.get(i);
                        db.collection("Courses").document(courseId)
                                .collection("classes").document(doc.getId())
                                .collection("progress").document(uid)
                                .get()
                                .addOnSuccessListener(progressDoc -> {
                                    boolean completed = progressDoc.exists() && Boolean.TRUE.equals(progressDoc.getBoolean("completed"));
                                    statuses[index] = resolveClassStatus(doc, completed);
                                    if (--remaining[0] == 0) renderClassList(docs, statuses, courseId);
                                })
                                .addOnFailureListener(e -> {
                                    statuses[index] = resolveClassStatus(doc, false);
                                    if (--remaining[0] == 0) renderClassList(docs, statuses, courseId);
                                });
                    }
                });
    }

    // 쿼리 결과의 순서(최신순)를 그대로 유지하기 위해, 전부 조회가 끝난 뒤 한 번에 순서대로 그린다.
    private void renderClassList(List<com.google.firebase.firestore.DocumentSnapshot> docs, String[] statuses, String courseId) {
        layoutClass.removeAllViews();
        for (int i = 0; i < docs.size(); i++) {
            renderClassCard(docs.get(i), courseId, statuses[i]);
        }
    }

    // 완료 전까지는 수강 기간 내내 "진행중", 기간이 지나야 "미완료"로 표시한다.
    private String resolveClassStatus(com.google.firebase.firestore.DocumentSnapshot doc, boolean completed) {
        if (completed) return "done";

        com.google.firebase.Timestamp endAtTs = doc.getTimestamp("endAt");
        boolean periodEnded = endAtTs != null && endAtTs.toDate().before(new java.util.Date());

        return periodEnded ? "not_started" : "in_progress";
    }

    private void renderClassCard(com.google.firebase.firestore.DocumentSnapshot doc, String courseId, String status) {
        String title = doc.getString("title");
        Long duration = doc.getLong("durationSeconds");
        String durationStr = "";
        if (duration != null) {
            durationStr = duration < 60 ? duration + "초" : (duration / 60) + "분";
        }

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
            default:
                tvState.setText("미완료");
                tvState.setBackgroundResource(R.drawable.bg_assignment_undone);
                break;
        }

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