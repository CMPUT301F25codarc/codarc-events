package ca.ualberta.codarc.codarc_events.utils;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Helper for sending FCM notifications via Cloud Function.
 */
public class FCMHelper {
    
    private static final String TAG = "FCMHelper";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final String functionUrl;
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    public FCMHelper(String functionUrl) {
        this.functionUrl = functionUrl;
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * Sends push notifications to a list of device tokens.
     *
     * @param tokens list of FCM device tokens
     * @param title notification title
     * @param body notification body
     * @param data optional data payload
     */
    public void sendNotifications(List<String> tokens, String title, String body, 
                                  Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            Log.d(TAG, "No tokens to send notifications to");
            return;
        }
        
        JsonObject requestBody = new JsonObject();
        requestBody.add("tokens", gson.toJsonTree(tokens));
        requestBody.addProperty("title", title);
        requestBody.addProperty("body", body);
        if (data != null && !data.isEmpty()) {
            requestBody.add("data", gson.toJsonTree(data));
        }
        
        RequestBody bodyRequest = RequestBody.create(requestBody.toString(), JSON);
        Request request = new Request.Builder()
            .url(functionUrl)
            .post(bodyRequest)
            .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to send FCM notifications", e);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "FCM function returned error: " + response.code());
                } else {
                    Log.d(TAG, "FCM notifications sent successfully");
                }
                response.close();
            }
        });
    }
}
