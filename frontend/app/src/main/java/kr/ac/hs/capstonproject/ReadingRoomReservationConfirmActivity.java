package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class ReadingRoomReservationConfirmActivity extends AppCompatActivity {

    private TextView tvRoom, tvDate, tvTime, tvSeat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reading_room_reservation_confirm);

        setupHeader();
        setupBottomNavigation();

        tvRoom = findViewById(R.id.tvRoom);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        tvSeat = findViewById(R.id.tvSeat);

        String room = getIntent().getStringExtra("room");
        String date = getIntent().getStringExtra("date");
        String time = getIntent().getStringExtra("time");
        String seat = getIntent().getStringExtra("seat");

        tvRoom.setText(room != null ? room : "경삼관 3F 열람실");
        tvDate.setText(date != null ? date : "");
        tvTime.setText(time != null ? time : "");
        tvSeat.setText(seat != null ? seat + "번" : "");

        MaterialButton btnConfirm = findViewById(R.id.btnConfirm);
        btnConfirm.setOnClickListener(v -> finish());
    }

    private void setupHeader() {
        View header = findViewById(R.id.include_header);
        ((TextView) header.findViewById(R.id.tv_subject_name)).setText("학습시설");
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