package kr.ac.hs.spacesense;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
public class CameraService extends Service {

    private static final long INTERVAL_MS = 1 * 60 * 1000; // 1분
    private static final int NOTIF_ID = 1;
    private static final String CHANNEL_ID = "camera_service_channel";

    private Handler handler;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;

    // ── 서비스 시작 ──────────────────────────────────────────────
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());

        handler = new Handler(Looper.getMainLooper());
        startCamera();

        return START_STICKY; // 시스템이 죽여도 재시작
    }

    // ── CameraX 초기화 ───────────────────────────────────────────
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                cameraProvider = future.get();

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector selector = CameraSelector.DEFAULT_FRONT_CAMERA;

                // 카메라 바인딩 (서비스는 LifecycleOwner가 없으므로 직접 구현 필요)
                cameraProvider.bindToLifecycle(
                        getFakeLifecycleOwner(), selector, imageCapture);

                // 첫 촬영 즉시 + 이후 10분마다
                scheduleCapture();

            } catch (Exception e) {
                Log.e("CameraService", "카메라 초기화 실패", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ── 10분 간격 반복 촬영 ──────────────────────────────────────
    private final Runnable captureRunnable = new Runnable() {
        @Override
        public void run() {
            takePhotoAndUpload();
            handler.postDelayed(this, INTERVAL_MS); // 다음 촬영 예약
        }
    };

    private void scheduleCapture() {
        handler.post(captureRunnable);
    }

    // ── 사진 촬영 ────────────────────────────────────────────────
    private void takePhotoAndUpload() {
        if (imageCapture == null) return;

        // 저장 경로: 앱 내부 캐시
        File photoFile = new File(getCacheDir(),
                "photo_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(options,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults result) {
                        Log.d("CameraService", "사진 저장 성공: " + photoFile.getPath());
                        FirebaseUploader.upload(photoFile); // Firebase로 전송
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        Log.e("CameraService", "촬영 실패", e);
                    }
                });
    }

    // ── 서비스용 LifecycleOwner ──────────────────────────────────
    // CameraX는 LifecycleOwner가 필요하므로 서비스에서는 직접 구현
    private LifecycleOwner getFakeLifecycleOwner() {
        LifecycleOwner owner = new LifecycleOwner() {
            private final LifecycleRegistry registry = new LifecycleRegistry(this);

            {
                registry.setCurrentState(Lifecycle.State.STARTED);
            }

            @NonNull
            @Override
            public Lifecycle getLifecycle() {
                return registry;
            }
        };
        return owner;
    }

    // ── 포그라운드 알림 ──────────────────────────────────────────
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "카메라 서비스", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("모니터링 중")
                .setContentText("1분마다 자동 촬영 중...")
                .setSmallIcon(android.R.drawable.ic_menu_camera) // 아이콘 필요
                .build();
    }

    // ── 정리 ─────────────────────────────────────────────────────
    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(captureRunnable);
        if (cameraProvider != null) cameraProvider.unbindAll();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}