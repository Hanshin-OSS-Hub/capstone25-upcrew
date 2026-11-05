package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SubjectClassDetailActivity extends AppCompatActivity {

    private TextView tvWeekNumber;
    private TextView tvClassRound;
    private TextView tvStudyTime;
    private PlayerView playerView;

    private ExoPlayer player;

    private String courseId;
    private String classId;

    private FirebaseFirestore db;
    private String uid;

    private long requiredSeconds = 0;
    private long watchedSeconds = 0;
    private long lastSyncedSeconds = 0;
    private boolean completed = false;

    private final Handler tickHandler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                watchedSeconds++;
                updateStudyTimeLabel();

                if (!completed && watchedSeconds >= requiredSeconds && requiredSeconds > 0) {
                    completed = true;
                    Toast.makeText(SubjectClassDetailActivity.this, "수강이 완료되었습니다!", Toast.LENGTH_SHORT).show();
                    syncProgress();
                } else if (watchedSeconds - lastSyncedSeconds >= 5) {
                    syncProgress();
                }
            }
            tickHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_subject_class_detail);

        courseId = getIntent().getStringExtra("courseId");
        classId = getIntent().getStringExtra("classId");

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        initViews();
        setupHeaderTitle();
        setupBottomNavigation();

        if (courseId == null || classId == null) {
            Toast.makeText(this, "강의 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadClassDetail();
    }

    private void initViews() {
        tvWeekNumber = findViewById(R.id.tvWeekNumber);
        tvClassRound = findViewById(R.id.tvClassRound);
        tvStudyTime = findViewById(R.id.tvStudyTime);
        playerView = findViewById(R.id.playerView);
    }

    private void setupHeaderTitle() {
        View header = findViewById(R.id.include_header);
        TextView tvTitle = header.findViewById(R.id.tv_subject_name);

        if (tvTitle != null) {
            tvTitle.setText("강의수강");
        }

        View backBtn = header.findViewById(R.id.btn_back);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }
    }

    private void loadClassDetail() {
        db.collection("Courses")
                .document(courseId)
                .collection("classes")
                .document(classId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "강의가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String title = doc.getString("title");
                    Long weekNumber = doc.getLong("weekNumber");
                    Long durationSeconds = doc.getLong("durationSeconds");
                    String videoUrl = doc.getString("videoUrl");

                    requiredSeconds = durationSeconds != null ? durationSeconds : 0;

                    if (weekNumber != null) {
                        tvWeekNumber.setText(String.valueOf(weekNumber));
                    }

                    // 아직 회차 필드가 없으면 일단 1회차로 고정
                    tvClassRound.setText("1");

                    loadExistingProgress();

                    if (videoUrl == null || videoUrl.isEmpty()) {
                        Toast.makeText(this, "등록된 강의 영상이 없습니다.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    playVideo(videoUrl);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "강의 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadExistingProgress() {
        updateStudyTimeLabel();

        if (uid == null) return;

        db.collection("Courses").document(courseId)
                .collection("classes").document(classId)
                .collection("progress").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long saved = doc.getLong("watchedSeconds");
                        watchedSeconds = saved != null ? saved : 0;
                        lastSyncedSeconds = watchedSeconds;
                        completed = Boolean.TRUE.equals(doc.getBoolean("completed"));
                    }
                    updateStudyTimeLabel();
                });
    }

    private void updateStudyTimeLabel() {
        long h = watchedSeconds / 3600;
        long m = (watchedSeconds % 3600) / 60;
        long s = watchedSeconds % 60;
        tvStudyTime.setText(String.format(Locale.KOREA, "학습시간: %02d:%02d:%02d", h, m, s));
    }

    private void syncProgress() {
        if (uid == null) return;
        lastSyncedSeconds = watchedSeconds;

        Map<String, Object> data = new HashMap<>();
        data.put("watchedSeconds", watchedSeconds);
        data.put("completed", completed);
        data.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("Courses").document(courseId)
                .collection("classes").document(classId)
                .collection("progress").document(uid)
                .set(data, SetOptions.merge());
    }

    private void playVideo(String videoUrl) {
        if (videoUrl.startsWith("gs://")) {
            FirebaseStorage.getInstance()
                    .getReferenceFromUrl(videoUrl)
                    .getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        initializePlayer(uri);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "영상 URL을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    });

        } else {
            initializePlayer(Uri.parse(videoUrl));
        }
    }

    private void initializePlayer(Uri videoUri) {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem mediaItem = MediaItem.fromUri(videoUri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(false);

        tickHandler.post(tick);
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

    @Override
    protected void onStop() {
        super.onStop();
        syncProgress();
        releasePlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tickHandler.removeCallbacks(tick);
        syncProgress();
        releasePlayer();
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
