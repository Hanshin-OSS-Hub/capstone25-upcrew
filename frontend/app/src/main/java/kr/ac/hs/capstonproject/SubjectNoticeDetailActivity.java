package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.annotation.Native;

public class SubjectNoticeDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_subject_notice_detail);
        setupBottomNavigation();

        // 헤더 뒤로가기
        View header = findViewById(R.id.include_header);
        header.findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 인텐트에서 데이터 수신
        String courseId  = getIntent().getStringExtra("courseId");
        String noticeId  = getIntent().getStringExtra("noticeId");
        String professor = getIntent().getStringExtra("professor");

        TextView tvTitle   = findViewById(R.id.tv_notice_title);
        TextView tvAuthor  = findViewById(R.id.tv_notice_author);
        TextView tvDate    = findViewById(R.id.tv_notice_date);
        TextView tvContent = findViewById(R.id.tv_notice_content);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Courses")
                .document(courseId)
                .collection("notices")
                .document(noticeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String title   = doc.getString("title");
                    String content = doc.getString("content");
                    com.google.firebase.Timestamp ts = doc.getTimestamp("createdAt");
                    String date = ts != null
                            ? new java.text.SimpleDateFormat("yy.MM.dd", java.util.Locale.KOREA).format(ts.toDate())
                            : "";

                    TextView tvSubjectName = header.findViewById(R.id.tv_subject_name);
                    if (tvSubjectName != null) tvSubjectName.setText("공지사항");

                    tvTitle.setText(title != null ? title : "");
                    tvAuthor.setText(professor != null ? professor : "");
                    tvDate.setText(date);
                    tvContent.setText(content != null ? content : "");
                })
                .addOnFailureListener(e -> {
                    tvTitle.setText("불러오기 실패");
                    tvContent.setText(e.getMessage());
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