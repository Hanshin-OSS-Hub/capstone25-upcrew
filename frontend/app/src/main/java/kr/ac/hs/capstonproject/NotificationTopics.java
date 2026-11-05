package kr.ac.hs.capstonproject;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.messaging.FirebaseMessaging;

import java.util.List;

/**
 * 강의별 FCM 토픽 구독을 관리한다.
 * 토픽 이름: {category}_{courseId}  (예: notice_2026_1_CSE101_A)
 * 각 카테고리 on/off는 PREFS에 저장된 값을 따른다 (AlarmSettingActivity에서 변경).
 */
public class NotificationTopics {

    public static final String PREFS_NAME = "notification_prefs";

    public static final String KEY_PUSH = "pref_push";
    public static final String KEY_NOTICE = "pref_notice";
    public static final String KEY_ASSIGNMENT = "pref_assignment";
    public static final String KEY_ASSIGNMENT_DEADLINE = "pref_assignment_deadline";
    public static final String KEY_CLASS = "pref_class";
    public static final String KEY_CLASS_DEADLINE = "pref_class_deadline";

    private static final String TOPIC_NOTICE = "notice_";
    private static final String TOPIC_ASSIGNMENT = "assignment_";
    private static final String TOPIC_ASSIGNMENT_DEADLINE = "assignment_deadline_";
    private static final String TOPIC_CLASS = "class_";
    private static final String TOPIC_CLASS_DEADLINE = "class_deadline_";

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context context, String key, boolean defaultValue) {
        return prefs(context).getBoolean(key, defaultValue);
    }

    public static void setEnabled(Context context, String key, boolean value) {
        prefs(context).edit().putBoolean(key, value).apply();
    }

    /**
     * 수강 중인 강의 목록과 현재 알림 설정에 맞춰 토픽 구독 상태를 동기화한다.
     * 알림 설정 화면에서 스위치를 바꾸거나, 수강 목록을 새로 불러올 때 호출한다.
     */
    public static void syncSubscriptions(Context context, List<String> activeCourseIds) {
        SharedPreferences p = prefs(context);
        boolean pushEnabled = p.getBoolean(KEY_PUSH, true);

        boolean notice = pushEnabled && p.getBoolean(KEY_NOTICE, true);
        boolean assignment = pushEnabled && p.getBoolean(KEY_ASSIGNMENT, true);
        boolean assignmentDeadline = pushEnabled && p.getBoolean(KEY_ASSIGNMENT_DEADLINE, true);
        boolean clazz = pushEnabled && p.getBoolean(KEY_CLASS, true);
        boolean classDeadline = pushEnabled && p.getBoolean(KEY_CLASS_DEADLINE, false);

        FirebaseMessaging messaging = FirebaseMessaging.getInstance();

        for (String courseId : activeCourseIds) {
            applyTopic(messaging, TOPIC_NOTICE + courseId, notice);
            applyTopic(messaging, TOPIC_ASSIGNMENT + courseId, assignment);
            applyTopic(messaging, TOPIC_ASSIGNMENT_DEADLINE + courseId, assignmentDeadline);
            applyTopic(messaging, TOPIC_CLASS + courseId, clazz);
            applyTopic(messaging, TOPIC_CLASS_DEADLINE + courseId, classDeadline);
        }
    }

    private static void applyTopic(FirebaseMessaging messaging, String topic, boolean subscribe) {
        if (subscribe) {
            messaging.subscribeToTopic(topic);
        } else {
            messaging.unsubscribeFromTopic(topic);
        }
    }
}
