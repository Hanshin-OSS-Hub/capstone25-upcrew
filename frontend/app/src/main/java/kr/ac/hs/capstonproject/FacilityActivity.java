package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FacilityActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_facility);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupBottomNavigation();
        loadCongestion();

        if (mAuth.getCurrentUser() == null) {
            finish();
            return;
        }
        String userUid = mAuth.getCurrentUser().getUid();

        // 헤더
        View header = findViewById(R.id.include_header);
        ((TextView) header.findViewById(R.id.tv_subject_name)).setText("학습시설");
        header.findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // 예약 버튼
        TextView btnReserveM = findViewById(R.id.btnReserveMeeting);
        TextView btnReserveS = findViewById(R.id.btnReserveStudyroom);

        btnReserveM.setOnClickListener(v -> {
            Intent intent = new Intent(this, MeetingRoomActivity.class);
            intent.putExtra("userUid", userUid);
            startActivity(intent);
        });

        btnReserveS.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReserveStudyroomActivity.class);
            intent.putExtra("userUid", userUid);
            startActivity(intent);
        });

        // 내역 버튼
        TextView btnHistoryM = findViewById(R.id.btnHistoryMeeting);
        btnHistoryM.setOnClickListener(v -> {
            Intent intent = new Intent(this, MeetingRoomReservationListActivity.class);
            intent.putExtra("userUid", userUid);
            startActivity(intent);
        });

        TextView btnHistoryS = findViewById(R.id.btnHistoryStudyroom);
        btnHistoryS.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReadingRoomReservationListActivity.class);
            intent.putExtra("userUid", userUid);
            startActivity(intent);
        });

        // 토글 설정
        setupToggle(R.id.rowHeader6, R.id.sectionContent6, R.id.btnToggle6);
        setupToggle(R.id.rowHeader18, R.id.sectionContent18, R.id.btnToggle18);
        setupToggle(R.id.rowHeader9, R.id.sectionContent9, R.id.btnToggle9);
    }

    private void loadCongestion() {
        db.collection("congestion").document("latest")
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;

                    String level = snapshot.getString("level"); // 여유 / 보통 / 혼잡

                    TextView tvCongestion = findViewById(R.id.tvCongestion9); // 9관 꼼지락 텍스트뷰 id
                    if (tvCongestion == null) return;

                    tvCongestion.setText(level);

                    // 색상 변경
                    int colorRes;
                    if ("혼잡".equals(level)) {
                        colorRes = R.color.chip_busy;
                    } else if ("보통".equals(level)) {
                        colorRes = R.color.chip_normal;
                    } else {
                        colorRes = R.color.chip_free;
                    }
                    tvCongestion.setBackgroundTintList(
                            getColorStateList(colorRes)
                    );
                });
    }
    private void setupToggle(int headerId, int contentId, int toggleBtnId) {
        View headerRow = findViewById(headerId);
        LinearLayout content = findViewById(contentId);
        android.widget.ImageButton toggleBtn = findViewById(toggleBtnId);

        View.OnClickListener toggleListener = v -> {
            if (content.getVisibility() == View.VISIBLE) {
                content.setVisibility(View.GONE);
                toggleBtn.setRotation(0f);
            } else {
                content.setVisibility(View.VISIBLE);
                toggleBtn.setRotation(180f);
            }
        };

        headerRow.setOnClickListener(toggleListener);
        toggleBtn.setOnClickListener(toggleListener);
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