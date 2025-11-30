package ca.ualberta.codarc.codarc_events.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

/**
 * Utility class for creating simplified TextWatchers.
 */
public class TextWatcherHelper {

    /**
     * Creates a TextWatcher that updates a character count TextView.
     *
     * @param charCountView the TextView to update with character count
     * @param maxLength the maximum length to display (e.g., 500)
     * @return a TextWatcher instance
     */
    public static TextWatcher createCharCountWatcher(TextView charCountView, int maxLength) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s != null ? s.length() : 0;
                charCountView.setText(length + "/" + maxLength);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not needed
            }
        };
    }
}
