package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class SettingsActivity extends AppCompatActivity {

    private MaterialCardView btnProfile;
    private MaterialCardView btnNotice;
    private MaterialCardView btnAlarmSetting;
    private MaterialCardView btnAppFeedback;
    private Switch switchDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        View headerView = findViewById(R.id.include_header);
        TextView tvHeaderTitle = headerView.findViewById(R.id.tv_header_title);

        tvHeaderTitle.setText("설정");
        setupHeaderTitle();
        setupBottomNavigation();


        btnProfile = findViewById(R.id.btnProfile);
        btnNotice = findViewById(R.id.btnNotice);
        btnAlarmSetting = findViewById(R.id.btnAlarmSetting);
        btnAppFeedback = findViewById(R.id.btnAppFeedback);
        switchDarkMode = findViewById(R.id.switchDarkMode);

        setupButtonClickEvents();
    }

    private void setupButtonClickEvents() {
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        btnNotice.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, NoticeActivity.class);
            startActivity(intent);
        });

        btnAlarmSetting.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AlarmSettingActivity.class);
            startActivity(intent);
        });

        btnAppFeedback.setOnClickListener(v -> {
            // 미정
        });
    }

    private void setupHeaderTitle() {
        View header = findViewById(R.id.include_header);
        TextView tvTitle = header.findViewById(R.id.tv_subject_name);

        if (tvTitle != null) {
            tvTitle.setText("설정");
        }

        // 뒤로가기 버튼
        View btnBack = header.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.include_bottom_nav);

        bottomNavigationView.setSelectedItemId(R.id.tab_settings);

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
                return true;
            }

            return false;
        });
    }
}