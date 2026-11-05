package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class SubjectAssignmentSubmitActivity extends AppCompatActivity {

    private String courseId;
    private String assignmentId;

    private EditText etSubmitContent;
    private Button btnFileSelect;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private Uri pickedFileUri;
    private String existingFileName;
    private String existingFileUrl;
    private java.util.Date endAtDate;

    private final ActivityResultLauncher<String> filePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                pickedFileUri = uri;
                String name = queryFileName(uri);
                btnFileSelect.setText(name != null ? name : "파일 선택됨");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_subject_assignment_submit);

        courseId = getIntent().getStringExtra("courseId");
        assignmentId = getIntent().getStringExtra("assignmentId");

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupHeader();
        setupBottomNavigation();

        etSubmitContent = findViewById(R.id.etSubmitContent);
        btnFileSelect = findViewById(R.id.btnFileSelect);
        Button btnTempSave = findViewById(R.id.btnTempSave);
        Button btnSubmitAssignment = findViewById(R.id.btnSubmitAssignment);

        btnFileSelect.setOnClickListener(v -> filePicker.launch("*/*"));

        btnTempSave.setOnClickListener(v -> saveSubmission(false));
        btnSubmitAssignment.setOnClickListener(v -> saveSubmission(true));

        loadExistingSubmission();
        loadDeadline();
    }

    private void loadDeadline() {
        db.collection("Courses").document(courseId)
                .collection("assignments").document(assignmentId)
                .get()
                .addOnSuccessListener(doc -> {
                    com.google.firebase.Timestamp endAt = doc.getTimestamp("endAt");
                    endAtDate = endAt != null ? endAt.toDate() : null;

                    if (endAtDate != null && endAtDate.before(new java.util.Date())) {
                        Toast.makeText(this, "제출 기간이 마감되었습니다.", Toast.LENGTH_LONG).show();
                        etSubmitContent.setEnabled(false);
                        btnFileSelect.setEnabled(false);
                        findViewById(R.id.btnTempSave).setEnabled(false);
                        findViewById(R.id.btnSubmitAssignment).setEnabled(false);
                    }
                });
    }

    private void loadExistingSubmission() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("Courses").document(courseId)
                .collection("assignments").document(assignmentId)
                .collection("submissions").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    etSubmitContent.setText(doc.getString("content"));
                    existingFileName = doc.getString("fileName");
                    existingFileUrl = doc.getString("fileUrl");

                    if (existingFileName != null && !existingFileName.isEmpty()) {
                        btnFileSelect.setText(existingFileName);
                    }
                });
    }

    private void saveSubmission(boolean isSubmitted) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "로그인 정보를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endAtDate != null && endAtDate.before(new java.util.Date())) {
            Toast.makeText(this, "제출 기간이 마감되었습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = etSubmitContent.getText().toString().trim();
        if (content.isEmpty() && pickedFileUri == null && existingFileUrl == null) {
            Toast.makeText(this, "제출 내용을 입력하거나 파일을 첨부해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        if (pickedFileUri != null) {
            String fileName = queryFileName(pickedFileUri);
            if (fileName == null) fileName = "첨부파일";

            StorageReference ref = FirebaseStorage.getInstance().getReference()
                    .child("assignment_submissions")
                    .child(courseId)
                    .child(assignmentId)
                    .child(uid)
                    .child(fileName);

            String finalFileName = fileName;
            Toast.makeText(this, "파일 업로드 중...", Toast.LENGTH_SHORT).show();

            ref.putFile(pickedFileUri)
                    .addOnSuccessListener(taskSnapshot ->
                            writeSubmissionDoc(uid, content, finalFileName, ref.toString(), isSubmitted))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "파일 업로드 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            writeSubmissionDoc(uid, content, existingFileName, existingFileUrl, isSubmitted);
        }
    }

    private void writeSubmissionDoc(String uid, String content, String fileName, String fileUrl, boolean isSubmitted) {
        Map<String, Object> data = new HashMap<>();
        data.put("content", content);
        data.put("fileName", fileName);
        data.put("fileUrl", fileUrl);
        data.put("isSubmitted", isSubmitted);
        data.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        if (isSubmitted) {
            data.put("submittedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        }

        db.collection("Courses").document(courseId)
                .collection("assignments").document(assignmentId)
                .collection("submissions").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (isSubmitted) {
                        markMyTaskDone(uid);
                        Intent intent = new Intent(this, SubjectAssignmentSubmittedActivity.class);
                        intent.putExtra("courseId", courseId);
                        intent.putExtra("assignmentId", assignmentId);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "임시저장되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // 홈/강의실 화면의 "제출완료" 표시는 Users/{uid}/myTasks.isDone 을 보고 그려지므로
    // 실제 제출이 완료되면 해당 myTasks 문서도 같이 done 처리한다.
    private void markMyTaskDone(String uid) {
        String refPath = "/Courses/" + courseId + "/assignments/" + assignmentId;

        db.collection("Users").document(uid).collection("myTasks")
                .whereEqualTo("type", "assignment")
                .whereEqualTo("refPath", refPath)
                .get()
                .addOnSuccessListener(query -> {
                    for (var taskDoc : query.getDocuments()) {
                        taskDoc.getReference().update("isDone", true);
                    }
                });
    }

    private String queryFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            } catch (Exception ignored) {
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
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
