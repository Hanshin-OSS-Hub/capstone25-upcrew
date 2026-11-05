const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");

admin.initializeApp();
setGlobalOptions({ region: "asia-northeast3" });

const db = admin.firestore();
const messaging = admin.messaging();

/**
 * 토픽 이름 규칙(Android NotificationTopics.java와 반드시 동일해야 함):
 *   notice_{courseId}             공지 등록
 *   assignment_{courseId}         과제 등록
 *   assignment_deadline_{courseId} 과제 마감 D-3/D-1/D-0
 *   class_{courseId}              온라인강의 등록
 *   class_deadline_{courseId}     온라인강의 마감 D-3/D-1/D-0
 */
async function sendToTopic(topic, title, body) {
  try {
    await messaging.send({ topic, notification: { title, body } });
  } catch (err) {
    console.error(`FCM send failed (topic=${topic}):`, err);
  }
}

/**
 * 해당 강의를 수강 중(isActive=true)인 학생 전원의 Users/{uid}/myTasks 에
 * 할 일을 생성한다. refPath/classRefPath는 앱 코드(CalendarActivity의
 * split("/")[2] 코스ID 추출, SubjectMainActivity의 상태맵 키)가 선행 "/"가
 * 붙은 경로를 기대하므로 반드시 "/Courses/..." 형식으로 맞춘다.
 */
async function fanOutMyTasks(courseId, taskData) {
  const enrolledSnap = await db.collectionGroup("myCourses")
    .where("courseId", "==", courseId)
    .where("isActive", "==", true)
    .get();

  if (enrolledSnap.empty) return;

  const batch = db.batch();
  for (const enrolledDoc of enrolledSnap.docs) {
    const uid = enrolledDoc.ref.parent.parent.id;
    const taskRef = db.collection("Users").doc(uid).collection("myTasks").doc();
    batch.set(taskRef, taskData);
  }
  await batch.commit();
}

// 공지 등록 시 알림
exports.onNoticeCreated = onDocumentCreated(
  "Courses/{courseId}/notices/{noticeId}",
  async (event) => {
    const data = event.data.data();
    const courseId = event.params.courseId;
    const courseSnap = await db.collection("Courses").doc(courseId).get();
    const courseName = courseSnap.exists ? courseSnap.data().courseName : "";

    await sendToTopic(`notice_${courseId}`, `[공지] ${courseName}`, data.title || "");
  }
);

// 과제 등록 시: 수강생 myTasks 생성 + 알림
exports.onAssignmentCreated = onDocumentCreated(
  "Courses/{courseId}/assignments/{assignmentId}",
  async (event) => {
    const data = event.data.data();
    const courseId = event.params.courseId;
    const assignmentId = event.params.assignmentId;
    const courseName = data.courseName || "";

    await fanOutMyTasks(courseId, {
      type: "assignment",
      isDone: false,
      refPath: `/Courses/${courseId}/assignments/${assignmentId}`,
    });

    await sendToTopic(`assignment_${courseId}`, `[과제등록] ${courseName}`, data.title || "");
  }
);

// 온라인강의 등록 시: 수강생 myTasks 생성 + 알림
exports.onClassCreated = onDocumentCreated(
  "Courses/{courseId}/classes/{classId}",
  async (event) => {
    const data = event.data.data();
    const courseId = event.params.courseId;
    const classId = event.params.classId;
    const courseSnap = await db.collection("Courses").doc(courseId).get();
    const courseName = courseSnap.exists ? courseSnap.data().courseName : "";

    await fanOutMyTasks(courseId, {
      type: "class",
      status: "not_started",
      classRefPath: `/Courses/${courseId}/classes/${classId}`,
    });

    await sendToTopic(`class_${courseId}`, `[강의등록] ${courseName}`, data.title || "");
  }
);

// 매일 09:00(Asia/Seoul)에 과제/온라인강의 마감 D-3, D-1, 당일(D-0)을 확인해 알림
exports.checkDeadlines = onSchedule(
  { schedule: "0 9 * * *", timeZone: "Asia/Seoul" },
  async () => {
    const coursesSnap = await db.collection("Courses").get();

    for (const courseDoc of coursesSnap.docs) {
      const courseId = courseDoc.id;
      const courseName = courseDoc.data().courseName || "";

      await checkDeadlineCollection(courseId, courseName, "assignments", "assignment_deadline_", "과제");
      await checkDeadlineCollection(courseId, courseName, "classes", "class_deadline_", "온라인강의");
    }
  }
);

async function checkDeadlineCollection(courseId, courseName, subName, topicPrefix, label) {
  const snap = await db.collection("Courses").doc(courseId).collection(subName).get();

  for (const doc of snap.docs) {
    const data = doc.data();
    const endAt = data.endAt;
    if (!endAt || typeof endAt.toDate !== "function") continue;

    // schedule이 매일 09:00 KST(=00:00 UTC)에 실행되고 endAt은 항상 00:00 KST 기준이므로
    // 반올림한 날짜 차이가 정확히 3 / 1 / 0으로 떨어진다.
    const diffDays = Math.round((endAt.toDate().getTime() - Date.now()) / 86400000);

    let flagField;
    let dLabel;
    if (diffDays === 3) { flagField = "reminded3"; dLabel = "D-3"; }
    else if (diffDays === 1) { flagField = "reminded1"; dLabel = "D-1"; }
    else if (diffDays === 0) { flagField = "reminded0"; dLabel = "오늘 마감"; }
    else continue;

    if (data[flagField]) continue;

    await sendToTopic(
      `${topicPrefix}${courseId}`,
      `[${label} 마감 ${dLabel}] ${courseName}`,
      data.title || ""
    );

    await doc.ref.update({ [flagField]: true });
  }
}
