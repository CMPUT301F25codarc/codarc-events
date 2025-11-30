package ca.ualberta.codarc.codarc_events.services;

import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.utils.Identity;
import ca.ualberta.codarc.codarc_events.utils.NotificationChannelHelper;

import java.util.Map;

/**
 * Handles FCM token registration and message receipt.
 */
public class CodarcFirebaseMessagingService extends FirebaseMessagingService {
    
    private static final String TAG = "CodarcFCMService";
    
    @Override
    public void onNewToken(@NonNull String token) {
        String deviceId = Identity.getOrCreateDeviceId(this);
        EntrantDB entrantDB = new EntrantDB();
        entrantDB.saveFCMToken(deviceId, token, new EntrantDB.Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Log.d(TAG, "FCM token saved for device: " + deviceId);
            }
            
            @Override
            public void onError(@NonNull Exception e) {
                Log.e(TAG, "Failed to save FCM token for device: " + deviceId, e);
            }
        });
    }
    
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        NotificationChannelHelper.createChannel(this);
        
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        if (notification == null) {
            Log.d(TAG, "Received message with no notification payload");
            return;
        }
        
        String title = notification.getTitle();
        String body = notification.getBody();
        Map<String, String> data = remoteMessage.getData();
        
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
            this, NotificationChannelHelper.getChannelId())
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);
        
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
