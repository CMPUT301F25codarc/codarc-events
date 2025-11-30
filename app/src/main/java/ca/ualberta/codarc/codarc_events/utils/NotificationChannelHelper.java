package ca.ualberta.codarc.codarc_events.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

/**
 * Helper for creating notification channels (Android 8.0+).
 */
public class NotificationChannelHelper {
    
    private static final String CHANNEL_ID = "codarc_notifications";
    private static final String CHANNEL_NAME = "Event Notifications";
    private static final String CHANNEL_DESCRIPTION = "Notifications for event updates";
    
    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            
            NotificationManager notificationManager = 
                context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    public static String getChannelId() {
        return CHANNEL_ID;
    }
}
