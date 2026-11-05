package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class SubjectAssignmentSubmittedActivity extends AppCompatActivity {

    private TextView tvAssignmentTitle;
    private TextView tvSubmittedAt;
    private TextView tvAssignmentDescription;
    private LinearLayout layoutFiles;
    private MaterialCardView cardFiles;

    private String courseId;
    private String assignmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_subject_assignment_submitted);

        setupHeader();
        setupBottomNavigation();

        tvAssignmentTitle = findViewById(R.id.tvAssignmentTitle);
        tvSubmittedAt = findViewById(R.id.tvSubmittedAt);
        tvAssignmentDescription = findViewById(R.id.tvAssignmentDescription);
        layoutFiles = findViewById(R.id.layout_files);
        cardFiles = findViewById(R.id.card_files);

        courseId = getIntent().getStringExtra("courseId");
        assignmentId = getIntent().getStringExtra("assignmentId");

        findViewById(R.id.btnResubmit).setOnClickListener(v -> {
            Intent intent = new Intent(this, SubjectAssignmentSubmitActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("assignmentId", assignmentId);
            startActivity(intent);
            finish();
        });

        loadAssignmentTitle();
        loadSubmission();
    }

    private void loadAssignmentTitle() {
        FirebaseFirestore.getInstance()
                .collection("Courses").document(courseId)
                .collection("assignments").document(assignmentId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        tvAssignmentTitle.setText(doc.getString("title"));
                    }
                });
    }

    private void loadSubmission() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("Courses").document(courseId)
                .collection("assignments").document(assignmentId)
                .collection("submissions").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        tvAssignmentDescription.setText("제출 내역이 없습니다.");
                        return;
                    }

                    String content = doc.getString("content");
                    tvAssignmentDescription.setText(content != null && !content.isEmpty() ? content : "(제출 내용 없음)");

                    Timestamp submittedAt = doc.getTimestamp("submittedAt");
                    if (submittedAt != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);
                        tvSubmittedAt.setText("제출일시: " + sdf.format(submittedAt.toDate()));
                    }

                    String fileName = doc.getString("fileName");
                    String fileUrl = doc.getString("fileUrl");

                    if (fileUrl != null && !fileUrl.isEmpty()) {
                        cardFiles.setVisibility(View.VISIBLE);
                        layoutFiles.removeAllViews();

                        TextView tvFile = new TextView(this);
                        tvFile.setText("📎 " + (fileName != null ? fileName : "첨부파일"));
                        tvFile.setTextColor(getColor(R.color.black));
                        tvFile.setTextSize(15);
                        tvFile.setOnClickListener(v -> downloadFile(fileUrl));
                        layoutFiles.addView(tvFile);
                    }
                })
                .addOnFailureListener(e ->
                        tvAssignmentDescription.setText("불러오기 실패: " + e.getMessage()));
    }

    private void downloadFile(String gsUrl) {
        try {
            StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(gsUrl);

            Toast.makeText(this, "파일 준비 중...", Toast.LENGTH_SHORT).show();

            ref.getDownloadUrl()
                    .addOnSuccessListener(uri -> openUrl(uri.toString()))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());

        } catch (Exception e) {
            Toast.makeText(this, "경로 오류: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url));
        startActivity(intent);
    }

    private void setupHeader() {
        View header = findViewById(R.id.include_header);

        TextView title = header.findViewById(R.id.tv_subject_name);
        if (title != null) title.setText("과제 제출 정보");

        View back = header.findViewById(R.id.btn_back);
        if (back != null) back.setOnClickListener(v -> finish());
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
