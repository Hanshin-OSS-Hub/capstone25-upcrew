package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AlarmSettingActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alarm_setting);

        db = FirebaseFirestore.getInstance();

        setupHeader();
        setupBottomNavigation();
        setupSwitches();
    }

    private void setupSwitches() {
        bindSwitch(R.id.switch_push, NotificationTopics.KEY_PUSH, true);
        bindSwitch(R.id.switch_notice_alarm, NotificationTopics.KEY_NOTICE, true);
        bindSwitch(R.id.switch_assignment_alarm, NotificationTopics.KEY_ASSIGNMENT, true);
        bindSwitch(R.id.switch_assignment_deadline_alarm, NotificationTopics.KEY_ASSIGNMENT_DEADLINE, true);
        bindSwitch(R.id.switch_course_alarm, NotificationTopics.KEY_CLASS, true);
        bindSwitch(R.id.switch_course_deadline_alarm, NotificationTopics.KEY_CLASS_DEADLINE, false);
    }

    private void bindSwitch(int viewId, String prefKey, boolean defaultValue) {
        SwitchMaterial sw = findViewById(viewId);
        sw.setChecked(NotificationTopics.isEnabled(this, prefKey, defaultValue));
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            NotificationTopics.setEnabled(this, prefKey, isChecked);
            resyncTopicsForMyCourses();
        });
    }

    private void resyncTopicsForMyCourses() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("Users").document(uid).collection("myCourses")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> activeCourseIds = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        String id = doc.getString("courseId");
                        if (id != null) activeCourseIds.add(id);
                    }
                    NotificationTopics.syncSubscriptions(this, activeCourseIds);
                });
    }

    // =======================
    // Header 설정
    // =======================
    private void setupHeader() {
        View header = findViewById(R.id.include_header);

        TextView tvTitle = header.findViewById(R.id.tv_subject_name);
        tvTitle.setText("알림 설정");

        header.findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    // =======================
    // 하단 네비게이션
    // =======================
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
