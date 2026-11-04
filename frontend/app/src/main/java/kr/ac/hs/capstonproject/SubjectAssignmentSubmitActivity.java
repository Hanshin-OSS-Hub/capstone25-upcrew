package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SubjectAssignmentSubmitActivity extends AppCompatActivity {

    private String courseId;
    private String assignmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_subject_assignment_submit);

        courseId = getIntent().getStringExtra("courseId");
        assignmentId = getIntent().getStringExtra("assignmentId");

        setupHeader();
        setupBottomNavigation();

        Button btnSubmitAssignment = findViewById(R.id.btnSubmitAssignment);
        btnSubmitAssignment.setOnClickListener(v -> {
            Intent intent = new Intent(this, SubjectAssignmentSubmittedActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("assignmentId", assignmentId);
            startActivity(intent);
            finish();
        });
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