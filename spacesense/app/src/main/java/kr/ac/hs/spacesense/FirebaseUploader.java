package kr.ac.hs.spacesense;

import android.net.Uri;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class FirebaseUploader {

    public static void upload(File photoFile) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            uploadFile(photoFile);
        } else {
            // Storage 규칙이 로그인을 요구하므로 익명 로그인 후 업로드
            auth.signInAnonymously()
                    .addOnSuccessListener(result -> uploadFile(photoFile))
                    .addOnFailureListener(e -> Log.e("Upload", "❌ 익명 로그인 실패", e));
        }
    }

    private static void uploadFile(File photoFile) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        String uid = FirebaseAuth.getInstance().getUid();

        // 저장 경로 예: photos/{uid}/2024-01-15_14:30:00.jpg
        String fileName = "photos/" + uid + "/" + new SimpleDateFormat(
                "yyyy-MM-dd_HH:mm:ss", Locale.getDefault())
                .format(new Date()) + ".jpg";

        StorageReference ref = storage.getReference().child(fileName);
        Uri fileUri = Uri.fromFile(photoFile);

        ref.putFile(fileUri)
                .addOnSuccessListener(snapshot -> {
                    Log.d("Upload", "✅ 업로드 성공: " + fileName);
                    photoFile.delete(); // 로컬 파일 삭제
                })
                .addOnFailureListener(e -> {
                    Log.e("Upload", "❌ 업로드 실패", e);
                    // 실패 시 파일 보존 → 나중에 재시도 가능
                });
    }
}