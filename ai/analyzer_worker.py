"""
analyzer_worker.py
------------------
Firebase Storage에서 새 사진 감지 → 혼잡도 분석 → Firestore에 결과 저장

실행: python analyzer_worker.py
"""

import time
import datetime
import firebase_admin
from firebase_admin import credentials, storage, firestore
from congestion_analysis import analyze
from config import (
    SERVICE_ACCOUNT_KEY,
    FIREBASE_STORAGE_BUCKET,
    PHOTO_PREFIX,
    RESULT_COLLECTION,
    POLL_INTERVAL,
)


def init_firebase():
    cred = credentials.Certificate(SERVICE_ACCOUNT_KEY)
    firebase_admin.initialize_app(cred, {"storageBucket": FIREBASE_STORAGE_BUCKET})
    return firestore.client()


def upload_result(db, result: dict):
    """
    Firestore에 저장되는 구조:
    congestion/latest  →  { level, score, count, timestamp }
    """
    data = {
        "level":     result["level"],   # "여유" / "보통" / "혼잡"
        "score":     result["score"],   # 0.0 ~ 1.0
        "count":     result["count"],   # 감지 인원 수
        "timestamp": datetime.datetime.utcnow().isoformat(),
    }
    # latest 문서에 항상 덮어씀 → LMS 앱이 이 문서 하나만 보면 됨
    db.collection(RESULT_COLLECTION).document("latest").set(data)
    print(f"  ✔ Firestore 업데이트: level={data['level']}, score={data['score']}, count={data['count']}")

def cleanup_old_files(bucket, max_files=5):
    blobs = sorted(
        [b for b in bucket.list_blobs(prefix=PHOTO_PREFIX)
         if b.name.endswith((".jpg", ".jpeg", ".png"))],
        key=lambda b: b.time_created
    )
    while len(blobs) > max_files:
        oldest = blobs.pop(0)  # 가장 오래된 파일
        oldest.delete()
        print(f"  🗑 오래된 파일 삭제: {oldest.name}")


def main():
    print("=== 혼잡도 분석 시작 ===")
    db = init_firebase()
    bucket = storage.bucket()
    processed: set = set()

    # 시작 시 이미 있는 파일은 건너뜀
    for blob in bucket.list_blobs(prefix=PHOTO_PREFIX):
        processed.add(blob.name)
    print(f"기존 파일 {len(processed)}개 건너뜀.")

    while True:
        try:
            blobs = [
                b for b in bucket.list_blobs(prefix=PHOTO_PREFIX)
                if b.name not in processed and b.name.endswith((".jpg", ".jpeg", ".png"))
            ]

            if blobs:
                # 가장 최신 사진 1장만 분석 (10분 주기니까 보통 1장)
                latest = max(blobs, key=lambda b: b.time_created)
                local_path = "capture_latest.jpg"

                print(f"\n[{datetime.datetime.now().strftime('%H:%M:%S')}] 새 사진 감지: {latest.name}")
                latest.download_to_filename(local_path)

                print("  분석 중...")
                result = analyze(local_path)
                print(f"  결과: {result}")

                upload_result(db, result)
                cleanup_old_files(bucket, max_files=5)

                for b in blobs:
                    processed.add(b.name)
            else:
                print(f"[{datetime.datetime.now().strftime('%H:%M:%S')}] 대기 중... ({POLL_INTERVAL}초)")

        except Exception as e:
            print(f"[오류] {e}")

        time.sleep(POLL_INTERVAL)


if __name__ == "__main__":
    main()
