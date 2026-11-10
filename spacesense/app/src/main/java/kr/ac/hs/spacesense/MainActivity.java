package kr.ac.hs.spacesense;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST = 100;

    private Button btnStart, btnStop;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btn_start);
        btnStop  = findViewById(R.id.btn_stop);
        tvStatus = findViewById(R.id.tv_status);

        btnStart.setOnClickListener(v -> checkAndRequestPermissions());

        btnStop.setOnClickListener(v -> {
            stopService(new Intent(this, CameraService.class));
            tvStatus.setText("중지됨");
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        });
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.POST_NOTIFICATIONS
        };

        List<String> needed = new ArrayList<>();
        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    needed.toArray(new String[0]), PERMISSION_REQUEST);
        } else {
            startCameraService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == PERMISSION_REQUEST) {
            boolean allGranted = true;
            for (int r : results) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) startCameraService();
            else Toast.makeText(this, "권한이 필요합니다", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCameraService() {
        Intent intent = new Intent(this, CameraService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        tvStatus.setText("촬영 중 (1분 간격)");
        btnStart.setEnabled(false);  // 시작 버튼 비활성화
        btnStop.setEnabled(true);    // 멈춤 버튼 활성화
    }
}
//
//        ---
//
//        ## 동작 흐름
//        [시작] 버튼 클릭
//    → 권한 확인
//    → CameraService 시작
//    → 즉시 1회 촬영 + 10분마다 반복
//    → 버튼: 시작(비활성) / 멈춤(활성)
//
//[멈춤] 버튼 클릭
//    → CameraService 종료
//    → 버튼: 시작(활성) / 멈춤(비활성)