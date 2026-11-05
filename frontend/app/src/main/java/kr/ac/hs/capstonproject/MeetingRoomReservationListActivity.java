package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MeetingRoomReservationListActivity extends AppCompatActivity {

    private LinearLayout listContainer;
    private FirebaseFirestore db;
    private String userUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_meeting_room_reservation_list);
        setupBottomNavigation();

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        userUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupHeader();

        listContainer = findViewById(R.id.listContainer);

        loadReservations();
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
    // 🔹 예약 내역 불러오기
    // =======================
    private void loadReservations() {
        db.collection("reservations")
                .document("meetingroom")
                .collection("meetingroom")
                .whereEqualTo("userUid", userUid)
                .orderBy("date")
                .get()
                .addOnSuccessListener(query -> {
                    listContainer.removeAllViews();

                    if (query.isEmpty()) {
                        // 예약 내역 없을 때
                        TextView empty = new TextView(this);
                        empty.setText("예약 내역이 없습니다");
                        empty.setTextSize(16f);
                        empty.setPadding(0, 32, 0, 0);
                        listContainer.addView(empty);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : query) {
                        String date    = doc.getString("date");
                        String time    = doc.getString("time");
                        String endTime = doc.getString("endTime");
                        String room    = doc.getString("room");
                        Boolean active = doc.getBoolean("active");

                        View card = LayoutInflater.from(this)
                                .inflate(R.layout.item_meeting_room_reservation, listContainer, false);

                        TextView tvTitle = card.findViewById(R.id.tv_material_title);
                        TextView tvInfo  = card.findViewById(R.id.tv_material_info);

                        tvTitle.setText("경삼관 3F 회의실");

                        String status = (active != null && active) ? "예약중" : "만료";
                        String info   = date + " | " + time + " ~ " + endTime + " | " + status;
                        tvInfo.setText(info);

                        // 🔹 클릭 시 상세 페이지 이동
                        card.setOnClickListener(v -> {
                            Intent intent = new Intent(this, MeetingRoomReservationConfirmActivity.class);
                            intent.putExtra("room",    room);
                            intent.putExtra("date",    date);
                            intent.putExtra("time",    time);
                            intent.putExtra("endTime", endTime);
                            intent.putExtra("active",  active != null && active);
                            startActivity(intent);
                        });

                        listContainer.addView(card);
                    }
                })
                .addOnFailureListener(e -> {
                    TextView error = new TextView(this);
                    error.setText("불러오기 실패: " + e.getMessage());
                    listContainer.addView(error);
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