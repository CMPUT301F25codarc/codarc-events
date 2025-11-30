package ca.ualberta.codarc.codarc_events.data;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.InputStream;

import ca.ualberta.codarc.codarc_events.utils.ValidationHelper;

/**
 * Handles Firebase Storage operations for event posters.
 * Stores posters at the path: posters/{eventId}.jpg
 */
public class PosterStorage {

    private static final String TAG = "PosterStorage";
    private static final String POSTERS_PATH = "posters";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception e);
    }

    private final FirebaseStorage storage;

    public PosterStorage() {
        this.storage = FirebaseStorage.getInstance();
    }

    /**
     * Uploads a poster image for an event.
     * The file will be stored at posters/{eventId}.jpg
     *
     * @param eventId the event ID (used as filename)
     * @param imageUri the URI of the image to upload
     * @param callback callback that receives the download URL on success
     */
    public void uploadPoster(String eventId, Uri imageUri, Callback<String> callback) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
            ValidationHelper.requireNonNull(imageUri, "imageUri");
        } catch (IllegalArgumentException e) {
            callback.onError(e);
            return;
        }

        try {
            StorageReference posterRef = storage.getReference()
                    .child(POSTERS_PATH)
                    .child(eventId + ".jpg");

            UploadTask uploadTask = posterRef.putFile(imageUri);

            uploadTask.addOnSuccessListener(taskSnapshot -> {
                posterRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            Log.d(TAG, "Poster uploaded successfully: " + downloadUrl);
                            callback.onSuccess(downloadUrl);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to get download URL", e);
                            callback.onError(new RuntimeException("Failed to get download URL", e));
                        });
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed to upload poster", e);
                String errorMessage = "Failed to upload poster";
                if (e.getMessage() != null) {
                    if (e.getMessage().contains("size")) {
                        errorMessage = "Image is too large. Maximum size is 5MB.";
                    } else if (e.getMessage().contains("content")) {
                        errorMessage = "Invalid file type. Please select an image file.";
                    }
                }
                callback.onError(new RuntimeException(errorMessage, e));
            });

        } catch (Exception e) {
            Log.e(TAG, "Error uploading poster", e);
            callback.onError(new RuntimeException("Error uploading poster", e));
        }
    }

    /**
     * Deletes a poster image for an event.
     *
     * @param eventId the event ID
     * @param callback callback for completion
     */
    public void deletePoster(String eventId, Callback<Void> callback) {
        try {
            ValidationHelper.requireNonEmpty(eventId, "eventId");
        } catch (IllegalArgumentException e) {
            callback.onError(e);
            return;
        }

        StorageReference posterRef = storage.getReference()
                .child(POSTERS_PATH)
                .child(eventId + ".jpg");

        posterRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Poster deleted successfully for event: " + eventId);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete poster", e);
                    callback.onError(e);
                });
    }
}

