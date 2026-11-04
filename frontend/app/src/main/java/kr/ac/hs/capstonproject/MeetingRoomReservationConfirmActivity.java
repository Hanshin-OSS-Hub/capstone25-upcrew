package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MeetingRoomReservationConfirmActivity extends AppCompatActivity {

    private TextView tvBuilding, tvDate, tvTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_meeting_room_reservation_confirm);
        setupBottomNavigation();

        setupHeader();
        bindViews();
        loadFromIntent();
        setupConfirmButton();
    }

    // =======================
    // 🔹 Header
    // =======================
    private void setupHeader() {
        View header = findViewById(R.id.include_header);
        ((TextView) header.findViewById(R.id.tv_subject_name)).setText("학습 시설");
        header.findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    // =======================
    // 🔹 View 바인딩
    // =======================
    private void bindViews() {
        tvBuilding = findViewById(R.id.tv_building);
        tvDate     = findViewById(R.id.tv_date);
        tvTime     = findViewById(R.id.tv_time);
    }

    // =======================
    // 🔹 "14:00" → "오후 2시" 변환
    // =======================
    private String formatTime(String time) {
        if (time == null || !time.contains(":")) return "-";

        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);

        if (hour == 0)   return "오전 12시";
        if (hour < 12)   return "오전 " + hour + "시";
        if (hour == 12)  return "오후 12시";
        return "오후 " + (hour - 12) + "시";
    }

    // =======================
    // 🔹 Intent에서 데이터 받아서 표시
    // =======================
    private void loadFromIntent() {
        String room    = getIntent().getStringExtra("room");
        String date    = getIntent().getStringExtra("date");
        String time    = getIntent().getStringExtra("time");
        String endTime = getIntent().getStringExtra("endTime");

        if (room    == null) room    = "-";
        if (date    == null) date    = "-";
        if (time    == null) time    = "-";
        if (endTime == null) endTime = "-";

        tvBuilding.setText(room);
        tvDate.setText(date);
        tvTime.setText(formatTime(time) + " ~ " + formatTime(endTime));
    }

    // =======================
    // 🔹 확인 버튼
    // =======================
    private void setupConfirmButton() {
        findViewById(R.id.btnAssignmentInfo).setOnClickListener(v -> finish());
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