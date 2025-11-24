package ca.ualberta.codarc.codarc_events.views;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import ca.ualberta.codarc.codarc_events.R;

/**
 * Full-screen image viewer activity.
 * Displays an image in full screen with the ability to close.
 */
public class FullScreenImageActivity extends AppCompatActivity {

    private static final String TAG = "FullScreenImageActivity";
    public static final String EXTRA_IMAGE_URL = "image_url";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide system UI for immersive full-screen experience
        hideSystemUI();
        
        setContentView(R.layout.activity_fullscreen_image);

        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.e(TAG, "No image URL provided");
            finish();
            return;
        }

        ImageView fullscreenImage = findViewById(R.id.fullscreen_image);
        ImageButton closeButton = findViewById(R.id.btn_close);

        // Load image using Glide
        Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(R.drawable.sample_event_banner)
                .into(fullscreenImage);

        // Close button click listener
        closeButton.setOnClickListener(v -> finish());

        // Also allow tapping the image to close
        fullscreenImage.setOnClickListener(v -> finish());
    }

    /**
     * Hides the system UI for an immersive full-screen experience.
     */
    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
        
        // Keep screen on while viewing
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }
}

