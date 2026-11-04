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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SubjectAssignmentSubmittedActivity extends AppCompatActivity {

    private TextView tvAssignmentTitle;
    private TextView tvAssignmentDescription;
    private LinearLayout layoutFiles;
    private MaterialCardView cardFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_subject_assignment_submitted);

        setupHeader();
        setupBottomNavigation();

        tvAssignmentTitle = findViewById(R.id.tvAssignmentTitle);
        tvAssignmentDescription = findViewById(R.id.tvAssignmentDescription);
        layoutFiles = findViewById(R.id.layout_files);
        cardFiles = findViewById(R.id.card_files);

        String courseId = getIntent().getStringExtra("courseId");
        String assignmentId = getIntent().getStringExtra("assignmentId");

        loadAssignment(courseId, assignmentId);
    }

    private void loadAssignment(String courseId, String assignmentId) {
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


                    Timestamp startAt = doc.getTimestamp("startAt");
                    Timestamp endAt = doc.getTimestamp("endAt");

                    SimpleDateFormat sdf =
                            new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);

                    tvAssignmentTitle.setText(title != null ? title : "");
                    tvAssignmentDescription.setText(description != null ? description : "");


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
                            tvFile.setTextColor(getColor(R.color.black));
                            tvFile.setTextSize(15);
                            tvFile.setPadding(0, 0, 0, 16);

                            if (fileUrl != null && !fileUrl.isEmpty()) {
                                tvFile.setOnClickListener(v -> downloadFile(fileUrl));
                            }

                            layoutFiles.addView(tvFile);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    tvAssignmentTitle.setText("불러오기 실패");
                    tvAssignmentDescription.setText(e.getMessage());
                });
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