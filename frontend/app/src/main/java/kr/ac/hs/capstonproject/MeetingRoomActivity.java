package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeetingRoomActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private Button btnReserve;
    private LinearLayout timeGrid;

    private FirebaseFirestore db;

    private String userUid;
    private String selectedDate = "";
    private String selectedTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_meeting_room);
        setupBottomNavigation();

        db = FirebaseFirestore.getInstance();
        userUid = getIntent().getStringExtra("userUid");

        setupHeader();

        calendarView = findViewById(R.id.calendarView);
        btnReserve = findViewById(R.id.btnReserve);
        timeGrid = findViewById(R.id.timeGrid);

        setupCalendar();
        setupTimeButtons();
        setupReserveButton();
        markExpiredReservations();
    }

    // =======================
    // 🔹 경로: reservations/meetingroom
    // =======================
    private com.google.firebase.firestore.CollectionReference getCollection() {
        return db.collection("reservations")
                .document("meetingroom")
                .collection("meetingroom");
    }

    // =======================
    // 🔹 만료 예약 active: false 처리
    // =======================
    private void markExpiredReservations() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int year = cal.get(java.util.Calendar.YEAR);
        int month = cal.get(java.util.Calendar.MONTH) + 1;
        int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
        int currentHour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        String today = year + "-" + month + "-" + day;

        getCollection()
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(query -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : query.getDocuments()) {
                        String date = doc.getString("date");
                        String endTime = doc.getString("endTime");
                        if (date == null || endTime == null) continue;

                        boolean isExpired = false;

                        if (date.compareTo(today) < 0) {
                            // 날짜가 지난 경우
                            isExpired = true;
                        } else if (date.equals(today)) {
                            // 오늘인데 endTime 시간이 지난 경우
                            try {
                                int endHour = Integer.parseInt(endTime.split(":")[0]);
                                if (currentHour > endHour) isExpired = true;
                            } catch (Exception ignored) {}
                        }

                        if (isExpired) {
                            doc.getReference().update("active", false);
                        }
                    }
                });
    }

    // =======================
    // 🔹 버튼 전체 리스트
    // =======================
    private List<Button> getAllTimeButtons() {
        List<Button> buttons = new ArrayList<>();
        for (int i = 0; i < timeGrid.getChildCount(); i++) {
            LinearLayout row = (LinearLayout) timeGrid.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                buttons.add((Button) row.getChildAt(j));
            }
        }
        return buttons;
    }

    // =======================
    // 🔹 Header
    // =======================
    private void setupHeader() {
        View header = findViewById(R.id.include_header);
        TextView title = header.findViewById(R.id.tv_subject_name);
        title.setText("학습 시설");
        header.findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    // =======================
    // 🔹 Calendar
    // =======================
    private void setupCalendar() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            month = month + 1;
            selectedDate = year + "-" + month + "-" + dayOfMonth;
            loadReservedTimes(selectedDate);
        });
    }

    // =======================
    // 🔹 시간 버튼
    // =======================
    private void setupTimeButtons() {
        List<Button> buttons = getAllTimeButtons();
        for (Button btn : buttons) {
            btn.setOnClickListener(v -> {
                if (!btn.isEnabled()) return;

                resetAllButtons();

                List<Button> allBtns = getAllTimeButtons();
                int clickedIndex = allBtns.indexOf(btn);

                if (clickedIndex != -1 && clickedIndex + 2 < allBtns.size()) {
                    boolean canSelect = true;
                    for (int k = clickedIndex; k < clickedIndex + 3; k++) {
                        if (!allBtns.get(k).isEnabled()) {
                            canSelect = false;
                            break;
                        }
                    }

                    if (canSelect) {
                        for (int k = clickedIndex; k < clickedIndex + 3; k++) {
                            selectButton(allBtns.get(k));
                        }
                        selectedTime = btn.getText().toString();
                    } else {
                        Toast.makeText(this, "연속 3시간 예약이 불가능합니다", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "연속 3시간 예약이 불가능합니다", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void resetAllButtons() {
        for (Button b : getAllTimeButtons()) {
            if (b.isEnabled()) {
                b.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                b.setTextColor(Color.BLACK);
            }
        }
    }

    private void selectButton(Button btn) {
        btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#6C5CE7")));
        btn.setTextColor(Color.WHITE);
    }

    private void disableButton(Button btn) {
        btn.setEnabled(false);
        btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EEEEEE")));
        btn.setTextColor(Color.parseColor("#AAAAAA"));
    }

    // =======================
    // 🔹 예약 버튼
    // =======================
    private void setupReserveButton() {
        btnReserve.setOnClickListener(v -> saveToFirestore());
    }

    // =======================
    // 🔹 Firestore 저장
    // =======================
    private void saveToFirestore() {
        if (selectedDate.isEmpty() || selectedTime.isEmpty()) {
            Toast.makeText(this, "날짜와 시간을 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userUid == null) {
            Toast.makeText(this, "사용자 정보 오류", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1인 1예약 체크 (active: true 인 것만)
        getCollection()
                .whereEqualTo("userUid", userUid)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(userQuery -> {
                    if (!userQuery.isEmpty()) {
                        Toast.makeText(this, "이미 예약이 존재합니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 시간 중복 체크 (active: true 인 것만)
                    getCollection()
                            .whereEqualTo("date", selectedDate)
                            .whereEqualTo("time", selectedTime)
                            .whereEqualTo("active", true)
                            .get()
                            .addOnSuccessListener(timeQuery -> {
                                if (!timeQuery.isEmpty()) {
                                    Toast.makeText(this, "이미 예약된 시간입니다", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // endTime 계산
                                List<Button> buttons = getAllTimeButtons();
                                int startIndex = -1;
                                for (int i = 0; i < buttons.size(); i++) {
                                    if (buttons.get(i).getText().toString().equals(selectedTime)) {
                                        startIndex = i;
                                        break;
                                    }
                                }

                                String endTime = "";
                                if (startIndex != -1 && startIndex + 2 < buttons.size()) {
                                    endTime = buttons.get(startIndex + 2).getText().toString();
                                }

                                final String finalEndTime = endTime;
                                Map<String, Object> data = new HashMap<>();
                                data.put("userUid", userUid);
                                data.put("date", selectedDate);
                                data.put("time", selectedTime);
                                data.put("endTime", finalEndTime);
                                data.put("room", "경삼관 3F 회의실");
                                data.put("active", true);  // 활성 상태

                                getCollection()
                                        .add(data)
                                        .addOnSuccessListener(doc -> {
                                            Toast.makeText(this, "예약 완료", Toast.LENGTH_SHORT).show();
                                            loadReservedTimes(selectedDate);
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "예약 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                        );
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "조회 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "조회 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    // =======================
    // 🔹 예약된 시간 로드 (active: true 만)
    // =======================
    private void loadReservedTimes(String date) {
        getCollection()
                .whereEqualTo("date", date)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(query -> {
                    resetButtonsState();

                    query.forEach(doc -> {
                        String reservedTime = doc.getString("time");
                        String reservedEndTime = doc.getString("endTime");

                        boolean disabling = false;
                        for (Button btn : getAllTimeButtons()) {
                            String btnTime = btn.getText().toString();
                            if (btnTime.equals(reservedTime)) disabling = true;
                            if (disabling) disableButton(btn);
                            if (btnTime.equals(reservedEndTime)) disabling = false;
                        }
                    });
                });
    }

    private void resetButtonsState() {
        for (Button btn : getAllTimeButtons()) {
            btn.setEnabled(true);
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btn.setTextColor(Color.BLACK);
        }
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