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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Map;

public class SubjectMaterialDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_subject_material_detail);
        setupBottomNavigation();

        // 헤더 뒤로가기
        View header = findViewById(R.id.include_header);
        header.findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 인텐트에서 데이터 수신
        String courseId    = getIntent().getStringExtra("courseId");
        String materialId  = getIntent().getStringExtra("materialId");
        String professor   = getIntent().getStringExtra("professor");

        TextView tvTitle   = findViewById(R.id.tv_material_title);
        TextView tvAuthor  = findViewById(R.id.tv_material_author);
        TextView tvDate    = findViewById(R.id.tv_material_date);
        TextView tvContent = findViewById(R.id.tv_material_content);
        MaterialCardView cardFiles = findViewById(R.id.card_files);
        LinearLayout layoutFiles   = findViewById(R.id.layout_files);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Courses")
                .document(courseId)
                .collection("materials")
                .document(materialId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String title   = doc.getString("title");
                    String content = doc.getString("content");
                    com.google.firebase.Timestamp ts = doc.getTimestamp("createdAt");
                    String date = ts != null
                            ? new java.text.SimpleDateFormat("yy.MM.dd", java.util.Locale.KOREA).format(ts.toDate())
                            : "";

                    // 헤더 과목명
                    TextView tvSubjectName = header.findViewById(R.id.tv_subject_name);
                    if (tvSubjectName != null) tvSubjectName.setText("학습자료실");

                    tvTitle.setText(title != null ? title : "");
                    tvAuthor.setText(professor != null ? professor : "");
                    tvDate.setText(date);
                    tvContent.setText(content != null ? content : "");

                    // 첨부파일 목록 처리
                    List<Map<String, Object>> files =
                            (List<Map<String, Object>>) doc.get("files");

                    if (files != null && !files.isEmpty()) {
                        cardFiles.setVisibility(View.VISIBLE);
                        layoutFiles.removeAllViews();

                        for (int i = 0; i < files.size(); i++) {
                            Map<String, Object> file = files.get(i);
                            String fileName = (String) file.get("fileName");
                            String fileUrl  = (String) file.get("fileUrl");

                            TextView tvFile = new TextView(this);
                            tvFile.setText("📎 " + (fileName != null ? fileName : "파일 " + (i + 1)));
                            tvFile.setTextColor(getColor(R.color.black));
                            tvFile.setTextSize(15);
                            tvFile.setPadding(0, 0, 0, i < files.size() - 1 ? 16 : 0);

                            // 클릭 시 다운로드 URL 열기
                            if (fileUrl != null && !fileUrl.isEmpty()) {
                                tvFile.setOnClickListener(v -> downloadFile(fileUrl, fileName));
                            }

                            layoutFiles.addView(tvFile);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    tvTitle.setText("불러오기 실패");
                    tvContent.setText(e.getMessage());
                });
    }

    /**
     * gs:// 경로를 다운로드 URL로 변환 후 브라우저/뷰어로 열기
     */
    private void downloadFile(String gsUrl, String fileName) {
        android.util.Log.d("STORAGE", "gsUrl 원본: " + gsUrl);

        try {
            StorageReference ref = FirebaseStorage.getInstance().getReferenceFromUrl(gsUrl);
            android.util.Log.d("STORAGE", "ref 경로: " + ref.getPath());

            Toast.makeText(this, "파일 준비 중...", Toast.LENGTH_SHORT).show();
            ref.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        android.util.Log.d("STORAGE", "성공 URL: " + uri);
                        openUrl(uri.toString());
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("STORAGE", "실패: " + e.getClass().getName() + " / " + e.getMessage());
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        } catch (Exception e) {
            android.util.Log.e("STORAGE", "getReferenceFromUrl 예외: " + e.getMessage());
            Toast.makeText(this, "경로 오류: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
//    private void downloadFile(String gsUrl, String fileName) {
//        StorageReference ref;
//        if (gsUrl.startsWith("gs://")) {
//            ref = FirebaseStorage.getInstance().getReferenceFromUrl(gsUrl);
//        } else {
//            // 이미 https URL인 경우 바로 열기
//            openUrl(gsUrl);
//            return;
//        }
//
//        Toast.makeText(this, "파일 준비 중...", Toast.LENGTH_SHORT).show();
//
//        ref.getDownloadUrl()
//                .addOnSuccessListener(uri -> openUrl(uri.toString()))
//                .addOnFailureListener(e ->
//                        Toast.makeText(this, "파일 열기 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//    }


    private void openUrl(String url) {
        android.content.Intent intent = new android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(url));
        startActivity(intent);
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