package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView rvNotification;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private final List<NotificationFeedItem> feedItems = new ArrayList<>();
    private NotificationAdapter adapter;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ((TextView) findViewById(R.id.tv_header_title)).setText("알림");

        rvNotification = findViewById(R.id.rvNotification);
        rvNotification.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(feedItems);
        rvNotification.setAdapter(adapter);

        setupBottomNavigation();
        loadFeed();
    }

    private void loadFeed() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("Users").document(uid).collection("myCourses")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> courseIds = new ArrayList<>();
                    for (var doc : querySnapshot.getDocuments()) {
                        String id = doc.getString("courseId");
                        if (id != null) courseIds.add(id);
                    }

                    if (courseIds.isEmpty()) {
                        renderFeed();
                        return;
                    }

                    int[] remaining = {courseIds.size()};
                    for (String courseId : courseIds) {
                        loadCourseFeed(courseId, remaining);
                    }
                });
    }

    private void loadCourseFeed(String courseId, int[] remaining) {
        db.collection("Courses").document(courseId).get()
                .addOnSuccessListener(courseDoc -> {
                    String courseName = courseDoc.getString("courseName");
                    if (courseName == null) courseName = "";

                    String finalCourseName = courseName;
                    int[] subRemaining = {3};

                    db.collection("Courses").document(courseId).collection("notices")
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .limit(10).get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    for (var doc : task.getResult()) {
                                        Timestamp ts = doc.getTimestamp("createdAt");
                                        feedItems.add(new NotificationFeedItem(
                                                "[공지] " + finalCourseName,
                                                nullToDash(doc.getString("title")),
                                                ts != null ? dateFormat.format(ts.toDate()) : "",
                                                ts != null ? ts.toDate() : null
                                        ));
                                    }
                                }
                                if (--subRemaining[0] == 0) onCourseFeedDone(remaining);
                            });

                    db.collection("Courses").document(courseId).collection("assignments")
                            .orderBy("endAt", Query.Direction.DESCENDING)
                            .limit(10).get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    for (var doc : task.getResult()) {
                                        Timestamp ts = doc.getTimestamp("endAt");
                                        feedItems.add(new NotificationFeedItem(
                                                "[과제등록] " + finalCourseName,
                                                nullToDash(doc.getString("title")),
                                                ts != null ? "~" + dateFormat.format(ts.toDate()) : "",
                                                ts != null ? ts.toDate() : null
                                        ));
                                    }
                                }
                                if (--subRemaining[0] == 0) onCourseFeedDone(remaining);
                            });

                    db.collection("Courses").document(courseId).collection("classes")
                            .orderBy("startAt", Query.Direction.DESCENDING)
                            .limit(10).get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    for (var doc : task.getResult()) {
                                        Timestamp ts = doc.getTimestamp("startAt");
                                        feedItems.add(new NotificationFeedItem(
                                                "[강의등록] " + finalCourseName,
                                                nullToDash(doc.getString("title")),
                                                ts != null ? dateFormat.format(ts.toDate()) : "",
                                                ts != null ? ts.toDate() : null
                                        ));
                                    }
                                }
                                if (--subRemaining[0] == 0) onCourseFeedDone(remaining);
                            });
                })
                .addOnFailureListener(e -> {
                    if (--remaining[0] == 0) renderFeed();
                });
    }

    private void onCourseFeedDone(int[] remaining) {
        if (--remaining[0] == 0) renderFeed();
    }

    private void renderFeed() {
        Collections.sort(feedItems, (a, b) -> {
            if (a.sortDate == null && b.sortDate == null) return 0;
            if (a.sortDate == null) return 1;
            if (b.sortDate == null) return -1;
            return b.sortDate.compareTo(a.sortDate);
        });
        adapter.notifyDataSetChanged();
    }

    private String nullToDash(String s) {
        return s != null ? s : "-";
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.include_bottom_nav);
        bottomNavigationView.setSelectedItemId(R.id.tab_board);
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
