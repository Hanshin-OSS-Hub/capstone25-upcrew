package kr.ac.hs.capstonproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private ShapeableImageView imgProfile;
    private TextView tvName;
    private TextView tvStudentId;
    private TextView tvDepartment;
    private TextView tvEmail;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        setupHeader();
        initViews();
        setupBottomNavigation();

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        loadCurrentUserProfile();
    }

    private void setupHeader() {
        View header = findViewById(R.id.include_header);

        if (header != null) {
            TextView tvSubjectName = header.findViewById(R.id.tv_subject_name);
            if (tvSubjectName != null) {
                tvSubjectName.setText("내 프로필");
            }

            View btnBack = header.findViewById(R.id.btn_back);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }
        }
    }

    private void initViews() {
        imgProfile = findViewById(R.id.img_profile);
        tvName = findViewById(R.id.tv_name);
        tvStudentId = findViewById(R.id.tv_student_id);
        tvDepartment = findViewById(R.id.tv_department);
        tvEmail = findViewById(R.id.tv_email);
    }

    private void loadCurrentUserProfile() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "로그인 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userUid = auth.getCurrentUser().getUid();

        db.collection("Users")
                .document(userUid)
                .get()
                .addOnSuccessListener(this::setUserProfile)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "프로필 불러오기 실패", e);
                    Toast.makeText(this, "프로필 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setUserProfile(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Toast.makeText(this, "등록된 프로필 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = doc.getString("name");
        String studentID = doc.getString("studentID");
        String department = doc.getString("department");
        String email = doc.getString("email");
        String image = doc.getString("image");

        tvName.setText(name != null ? name : "이름 없음");
        tvStudentId.setText(studentID != null ? studentID : "학번 없음");
        tvDepartment.setText(department != null ? department : "소속 없음");
        tvEmail.setText(email != null ? email : "이메일 없음");

        loadProfileImage(image);
    }

    private void loadProfileImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            imgProfile.setImageResource(R.drawable.sample_profile);
            return;
        }

        // imagePath 예시:
        // gs://capstone-upcrew.firebasestorage.app/photos/sample_profile.png
        StorageReference imageRef = storage.getReferenceFromUrl(imagePath);

        imageRef.getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    Glide.with(this)
                            .load(uri)
                            .placeholder(R.drawable.sample_profile)
                            .error(R.drawable.sample_profile)
                            .into(imgProfile);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "프로필 이미지 불러오기 실패", e);
                    imgProfile.setImageResource(R.drawable.sample_profile);
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