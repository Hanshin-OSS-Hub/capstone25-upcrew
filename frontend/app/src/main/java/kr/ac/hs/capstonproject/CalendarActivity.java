package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;

import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private LinearLayout layoutTaskContainer;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Date selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calendar);

        View headerView = findViewById(R.id.include_header);
        TextView tvHeaderTitle = headerView.findViewById(R.id.tv_header_title);
        tvHeaderTitle.setText("캘린더");

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        calendarView = findViewById(R.id.calendarView);
        layoutTaskContainer = findViewById(R.id.layout_task_container);

        setupBottomNavigation();

        // 처음 화면 들어왔을 때는 오늘 날짜 기준으로 할 일 출력
        selectedDate = new Date();
        loadTasksUntilDate(selectedDate);

        // 캘린더 날짜 선택 시 해당 날짜까지의 할 일 출력
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth, 23, 59, 59);

            selectedDate = calendar.getTime();
            loadTasksUntilDate(selectedDate);
        });
    }

    // =======================
    // 선택한 날짜까지의 할 일 불러오기
    // =======================
    private void loadTasksUntilDate(Date targetDate) {
        layoutTaskContainer.removeAllViews();

        if (mAuth.getCurrentUser() == null) {
            addEmptyText("로그인 정보를 불러올 수 없습니다.");
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("Users")
                .document(uid)
                .collection("myTasks")
                .whereEqualTo("isDone", false)
                .whereEqualTo("type", "assignment")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DocumentSnapshot> tasks = querySnapshot.getDocuments();
                    List<Object[]> taskInfoList = new ArrayList<>();

                    int total = tasks.size();
                    int[] loadedCount = {0};

                    if (total == 0) {
                        addEmptyText("등록된 할 일이 없습니다.");
                        return;
                    }

                    for (DocumentSnapshot task : tasks) {
                        String assignmentRefPath = task.getString("refPath");

                        if (assignmentRefPath == null) {
                            loadedCount[0]++;

                            if (loadedCount[0] == total) {
                                showTaskList(taskInfoList);
                            }
                            continue;
                        }

                        db.document(assignmentRefPath)
                                .get()
                                .addOnSuccessListener(assignmentDoc -> {
                                    if (assignmentDoc.exists()) {
                                        Date endAt = assignmentDoc.getDate("endAt");
                                        String courseName = assignmentDoc.getString("courseName");
                                        String title = assignmentDoc.getString("title");

                                        // 선택한 날짜까지 마감인 과제만 추가
                                        if (endAt != null && !endAt.after(targetDate)) {
                                            taskInfoList.add(new Object[]{
                                                    endAt,
                                                    courseName,
                                                    title,
                                                    assignmentRefPath
                                            });
                                        }
                                    }

                                    loadedCount[0]++;

                                    if (loadedCount[0] == total) {
                                        showTaskList(taskInfoList);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    loadedCount[0]++;

                                    if (loadedCount[0] == total) {
                                        showTaskList(taskInfoList);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    addEmptyText("할 일을 불러오지 못했습니다.");
                });
    }

    // =======================
    // 할 일 목록 화면에 출력
    // =======================
    private void showTaskList(List<Object[]> taskInfoList) {
        layoutTaskContainer.removeAllViews();

        if (taskInfoList.isEmpty()) {
            addEmptyText("선택한 날짜까지의 할 일이 없습니다.");
            return;
        }

        // 마감일 빠른 순으로 정렬
        taskInfoList.sort((a, b) -> ((Date) a[0]).compareTo((Date) b[0]));

        for (Object[] info : taskInfoList) {
            Date endAt = (Date) info[0];
            String courseName = (String) info[1];
            String title = (String) info[2];
            String refPath = (String) info[3];

            addTaskCard(courseName, title, endAt, refPath);
        }
    }

    // =======================
    // 카드 하나 추가
    // =======================
    private void addTaskCard(String courseName, String title, Date endAt, String refPath) {
        MaterialCardView card = new MaterialCardView(this);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dpToPx(12));
        card.setLayoutParams(cardParams);

        card.setCardBackgroundColor(getColor(android.R.color.white));
        card.setRadius(dpToPx(12));
        card.setCardElevation(0);
        card.setStrokeWidth(0);

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.VERTICAL);
        innerLayout.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        TextView tvTitle = new TextView(this);
        tvTitle.setText("[과제] " + courseName);
        tvTitle.setTextColor(getColor(R.color.black));
        tvTitle.setTextSize(16);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(title + "\n마감일: " + formatDate(endAt));
        tvDesc.setTextColor(getColor(R.color.gray));
        tvDesc.setTextSize(15);
        tvDesc.setPadding(0, dpToPx(4), 0, 0);

        innerLayout.addView(tvTitle);
        innerLayout.addView(tvDesc);

        card.addView(innerLayout);

        card.setOnClickListener(v -> {
            String[] parts = refPath.split("/");

            if (parts.length >= 3) {
                String courseId = parts[2];

                Intent intent = new Intent(this, SubjectMainActivity.class);
                intent.putExtra("courseId", courseId);
                startActivity(intent);
            }
        });

        layoutTaskContainer.addView(card);
    }

    // =======================
    // 빈 화면 문구
    // =======================
    private void addEmptyText(String message) {
        layoutTaskContainer.removeAllViews();

        TextView empty = new TextView(this);
        empty.setText(message);
        empty.setTextSize(16);
        empty.setTextColor(getColor(R.color.gray));
        empty.setPadding(0, dpToPx(24), 0, 0);

        layoutTaskContainer.addView(empty);
    }

    // =======================
    // 날짜 포맷
    // =======================
    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);
        return sdf.format(date);
    }

    // =======================
    // dp 변환
    // =======================
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // =======================
    // 하단 네비게이션
    // =======================
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.include_bottom_nav);
        bottomNavigationView.setSelectedItemId(R.id.tab_timetable);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.tab_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;

            } else if (itemId == R.id.tab_timetable) {
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