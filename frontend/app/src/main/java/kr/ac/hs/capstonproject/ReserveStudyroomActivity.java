package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

public class ReserveStudyroomActivity extends AppCompatActivity {

    private LinearLayout timeGrid;
    private Button btnReserve;
    private FirebaseFirestore db;

    private String userUid;
    private String selectedDate = "";
    private String selectedTime = "";

    private final Map<String, Button> seatButtonMap = new HashMap<>();
    private Button selectedSeatBtn = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reserve_studyroom);
        setupBottomNavigation();

        db = FirebaseFirestore.getInstance();
        userUid = getIntent().getStringExtra("userUid");

        setupHeader();

        timeGrid = findViewById(R.id.timeGrid);
        btnReserve = findViewById(R.id.btnReserve);

        TextView tvTodayDate = findViewById(R.id.tvTodayDate);
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH) + 1;
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        selectedDate = year + "-" + month + "-" + day;
        tvTodayDate.setText(year + "년 " + month + "월 " + day + "일");

        setupSeatButtons();
        setupTimeButtons();
        setupReserveButton();
        markExpiredReservations();
        loadReservedSeats();
    }

    // =======================
    // 🔹 경로: reservations/studyroom/studyroom
    // =======================
    private com.google.firebase.firestore.CollectionReference getCollection() {
        return db.collection("reservations")
                .document("studyroom")
                .collection("studyroom");
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
    // 🔹 좌석 버튼
    // =======================
    private void setupSeatButtons() {
        int[] seatIds = {
                R.id.seat_1, R.id.seat_2, R.id.seat_3, R.id.seat_4,
                R.id.seat_5, R.id.seat_6, R.id.seat_7, R.id.seat_8,
                R.id.seat_9, R.id.seat_10, R.id.seat_11, R.id.seat_12,
                R.id.seat_13, R.id.seat_14, R.id.seat_15, R.id.seat_16,
                R.id.seat_17, R.id.seat_18, R.id.seat_19, R.id.seat_20,
                R.id.seat_21, R.id.seat_22, R.id.seat_23, R.id.seat_24,
                R.id.seat_25, R.id.seat_26, R.id.seat_27, R.id.seat_28,
                R.id.seat_29, R.id.seat_30, R.id.seat_31, R.id.seat_32,
                R.id.seat_33, R.id.seat_34, R.id.seat_35, R.id.seat_36,
                R.id.seat_37, R.id.seat_38, R.id.seat_39, R.id.seat_40,
                R.id.seat_41, R.id.seat_42, R.id.seat_43, R.id.seat_44,
                R.id.seat_45, R.id.seat_46, R.id.seat_47, R.id.seat_48,
                R.id.seat_49, R.id.seat_50, R.id.seat_51, R.id.seat_52,
                R.id.seat_53, R.id.seat_54, R.id.seat_55, R.id.seat_56,
                R.id.seat_57, R.id.seat_58, R.id.seat_59, R.id.seat_60,
                R.id.seat_61, R.id.seat_62, R.id.seat_63, R.id.seat_64,
                R.id.seat_65, R.id.seat_66, R.id.seat_67, R.id.seat_68,
                R.id.seat_69, R.id.seat_70, R.id.seat_71, R.id.seat_72,
                R.id.seat_73, R.id.seat_74, R.id.seat_75, R.id.seat_76,
                R.id.seat_77, R.id.seat_78, R.id.seat_79, R.id.seat_80,
                R.id.seat_81, R.id.seat_82, R.id.seat_83, R.id.seat_84,
                R.id.seat_85, R.id.seat_86, R.id.seat_87, R.id.seat_88,
                R.id.seat_89, R.id.seat_90, R.id.seat_91, R.id.seat_92,
                R.id.seat_93, R.id.seat_94
        };

        for (int id : seatIds) {
            Button seat = findViewById(id);
            seatButtonMap.put(seat.getText().toString(), seat);

            seat.setOnClickListener(v -> {
                if (selectedSeatBtn != null) {
                    selectedSeatBtn.setActivated(false);
                }
                selectedSeatBtn = (Button) v;
                selectedSeatBtn.setActivated(true);
                loadReservedTimes(selectedDate);
            });
        }
    }

    // =======================
    // 🔹 시간 버튼 목록
    // =======================
    private List<Button> getAllTimeButtons() {
        List<Button> buttons = new ArrayList<>();
        for (int i = 0; i < timeGrid.getChildCount(); i++) {
            View child = timeGrid.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) child;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View v = row.getChildAt(j);
                    if (v instanceof Button) {
                        buttons.add((Button) v);
                    }
                }
            }
        }
        return buttons;
    }

    // =======================
    // 🔹 시간 버튼 설정 (1시간 단일 선택)
    // =======================
    private void setupTimeButtons() {
        for (Button btn : getAllTimeButtons()) {
            btn.setOnClickListener(v -> {
                if (!btn.isEnabled()) return;
                resetAllButtons();
                selectButton(btn);
                selectedTime = btn.getText().toString();
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
        selectedTime = "";
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
        if (selectedSeatBtn == null) {
            Toast.makeText(this, "좌석을 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "시간을 선택하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userUid == null) {
            Toast.makeText(this, "사용자 정보 오류", Toast.LENGTH_SHORT).show();
            return;
        }

        String seat = selectedSeatBtn.getText().toString();

        // 1인 1예약 체크 (active: true 만)
        getCollection()
                .whereEqualTo("userUid", userUid)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(userQuery -> {
                    if (!userQuery.isEmpty()) {
                        Toast.makeText(this, "이미 예약이 존재합니다", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 좌석+시간 중복 체크 (active: true 만)
                    getCollection()
                            .whereEqualTo("date", selectedDate)
                            .whereEqualTo("seat", seat)
                            .whereEqualTo("time", selectedTime)
                            .whereEqualTo("active", true)
                            .get()
                            .addOnSuccessListener(timeQuery -> {
                                if (!timeQuery.isEmpty()) {
                                    Toast.makeText(this, "이미 예약된 좌석/시간입니다", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                Map<String, Object> data = new HashMap<>();
                                data.put("userUid", userUid);
                                data.put("date", selectedDate);
                                data.put("seat", seat);
                                data.put("time", selectedTime);
                                data.put("room", "경삼관 3F 열람실");
                                data.put("active", true);
                                // 퇴실 시간은 나중에 내역에서 퇴실 버튼으로 처리
                                // data.put("checkOutTime", "");

                                getCollection()
                                        .add(data)
                                        .addOnSuccessListener(doc -> {
                                            Toast.makeText(this, "예약 완료!", Toast.LENGTH_SHORT).show();
                                            loadReservedTimes(selectedDate);
                                            loadReservedSeats();
                                            resetAllButtons();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show()
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
    // 🔹 예약된 시간 로드 (선택한 좌석 + active: true 만)
    // =======================
    private void loadReservedTimes(String date) {
        if (selectedSeatBtn == null) return;

        String seat = selectedSeatBtn.getText().toString();

        getCollection()
                .whereEqualTo("date", date)
                .whereEqualTo("seat", seat)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(query -> {
                    resetButtonsState();

                    query.forEach(doc -> {
                        String reservedTime = doc.getString("time");
                        for (Button btn : getAllTimeButtons()) {
                            if (btn.getText().toString().equals(reservedTime)) {
                                disableButton(btn);
                            }
                        }
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "예약 정보 불러오기 실패", Toast.LENGTH_SHORT).show()
                );
    }

    private void resetButtonsState() {
        for (Button btn : getAllTimeButtons()) {
            btn.setEnabled(true);
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            btn.setTextColor(Color.BLACK);
        }
    }

    // =======================
    // 🔹 좌석 예약 상태 로드 (active: true = 사용중)
    // =======================
    private void loadReservedSeats() {
        // 모든 좌석 초기화
        for (Map.Entry<String, Button> entry : seatButtonMap.entrySet()) {
            String seatNum = entry.getKey();
            Button seatButton = entry.getValue();
            if (isGeneralSeat(seatNum)) {
                seatButton.setBackgroundResource(R.drawable.seat_general_selector);
            } else {
                seatButton.setBackgroundResource(R.drawable.seat_partition_selector);
            }
            seatButton.setActivated(false);
        }

        getCollection()
                .whereEqualTo("date", selectedDate)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(query -> {
                    query.forEach(doc -> {
                        String seat = doc.getString("seat");
                        if (seat != null && seatButtonMap.containsKey(seat)) {
                            // active: true 인 좌석 = 사용중 표시
                            seatButtonMap.get(seat).setBackgroundResource(R.drawable.seat_reserved_selector);
                        }
                    });
                });
    }

    private boolean isGeneralSeat(String seatNum) {
        int num;
        try { num = Integer.parseInt(seatNum); } catch (Exception e) { return false; }
        return (num >= 57 && num <= 84);
    }

    // =======================
    // 🔹 만료 예약 active: false 처리
    // (시작 시간 기준 - 퇴실은 내역에서 퇴실 버튼으로 처리)
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
                        String time = doc.getString("time");
                        if (date == null || time == null) continue;

                        boolean isExpired = false;

                        if (date.compareTo(today) < 0) {
                            // 날짜가 지난 경우
                            isExpired = true;
                        } else if (date.equals(today)) {
                            // 오늘인데 시작 시간이 지난 경우
                            try {
                                int startHour = Integer.parseInt(time.split(":")[0]);
                                if (currentHour > startHour) isExpired = true;
                            } catch (Exception ignored) {}
                        }

                        if (isExpired) {
                            doc.getReference().update("active", false);
                        }
                    }
                    // 만료 처리 후 좌석 상태 갱신
                    loadReservedSeats();
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