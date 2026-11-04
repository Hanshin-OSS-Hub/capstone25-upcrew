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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SubjectAssignmentDetailActivity extends AppCompatActivity {

    private TextView tvAssignmentTitle, tvStartAt, tvEndAt, tvScoreTotal, tvDescription;
    private MaterialCardView cardFiles;
    private LinearLayout layoutFiles;

    private String courseId;
    private String assignmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_subject_assignment_detail);

        courseId = getIntent().getStringExtra("courseId");
        assignmentId = getIntent().getStringExtra("assignmentId");

        setupHeader();
        setupBottomNavigation();

        tvAssignmentTitle = findViewById(R.id.tvAssignmentTitle);
        tvStartAt = findViewById(R.id.tvStartAt);
        tvEndAt = findViewById(R.id.tvEndAt);
        tvScoreTotal = findViewById(R.id.tvScoreTotal);
        tvDescription = findViewById(R.id.tvDescription);

        cardFiles = findViewById(R.id.card_files);
        layoutFiles = findViewById(R.id.layout_files);

        MaterialButton btnAssignmentInfo = findViewById(R.id.btnAssignmentInfo);
        btnAssignmentInfo.setOnClickListener(v -> {
            Intent intent = new Intent(this, SubjectAssignmentSubmitActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("assignmentId", assignmentId);
            startActivity(intent);
        });

        loadAssignment();
    }

    private void loadAssignment() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Courses")
                .document(courseId)
                .collection("assignments")
                .document(assignmentId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String title = doc.getString("title");
                    String description = doc.getString("description");

                    Object scoreObj = doc.get("scoreTotal");
                    String scoreTotal = scoreObj != null ? String.valueOf(scoreObj) : "";

                    Timestamp startAt = doc.getTimestamp("startAt");
                    Timestamp endAt = doc.getTimestamp("endAt");

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd", Locale.KOREA);

                    tvAssignmentTitle.setText(title != null ? title : "");
                    tvDescription.setText(description != null ? description : "");
                    tvScoreTotal.setText(scoreTotal);

                    tvStartAt.setText(startAt != null ? sdf.format(startAt.toDate()) : "");
                    tvEndAt.setText(endAt != null ? sdf.format(endAt.toDate()) : "");

                    List<Map<String, Object>> files =
                            (List<Map<String, Object>>) doc.get("files");

                    if (files != null && !files.isEmpty()) {
                        cardFiles.setVisibility(View.VISIBLE);
                        layoutFiles.removeAllViews();

                        for (int i = 0; i < files.size(); i++) {
                            Map<String, Object> file = files.get(i);

                            String fileName = (String) file.get("fileName");
                            String fileUrl = (String) file.get("fileUrl");

                            TextView tvFile = new TextView(this);
                            tvFile.setText("📎 " + (fileName != null ? fileName : "파일 " + (i + 1)));
                            tvFile.setTextColor(getColor(R.color.gray));
                            tvFile.setTextSize(16);
                            tvFile.setPadding(0, 0, 0, 12);

                            if (fileUrl != null && !fileUrl.isEmpty()) {
                                tvFile.setOnClickListener(v -> openFile(fileUrl));
                            }

                            layoutFiles.addView(tvFile);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "불러오기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openFile(String fileUrl) {
        if (!fileUrl.startsWith("gs://")) {
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(fileUrl)));
            return;
        }

        StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl);

        Toast.makeText(this, "파일 준비 중...", Toast.LENGTH_SHORT).show();

        ref.getDownloadUrl()
                .addOnSuccessListener(uri ->
                        startActivity(new Intent(Intent.ACTION_VIEW, uri)))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "파일 열기 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void setupHeader() {
        View header = findViewById(R.id.include_header);
        ((TextView) header.findViewById(R.id.tv_subject_name)).setText("과제제출");
        header.findViewById(R.id.btn_back).setOnClickListener(v -> finish());
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