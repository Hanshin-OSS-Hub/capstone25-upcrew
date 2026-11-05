package kr.ac.hs.capstonproject;

import java.util.Date;

public class NotificationFeedItem {
    public final String title;
    public final String content;
    public final String dateLabel;
    public final Date sortDate;

    public NotificationFeedItem(String title, String content, String dateLabel, Date sortDate) {
        this.title = title;
        this.content = content;
        this.dateLabel = dateLabel;
        this.sortDate = sortDate;
    }
}
