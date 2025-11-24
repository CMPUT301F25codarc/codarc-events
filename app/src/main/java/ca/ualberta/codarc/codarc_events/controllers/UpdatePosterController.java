package ca.ualberta.codarc.codarc_events.controllers;

import android.net.Uri;
import androidx.annotation.NonNull;

import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.PosterStorage;
import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Handles updating event posters - orchestrates poster upload and event update.
 * 
 * <p>This controller encapsulates the business logic for:
 * <ul>
 *   <li>Uploading a new poster image to Firebase Storage</li>
 *   <li>Updating the event document with the new poster URL</li>
 * </ul>
 * </p>
 * 
 * <p>The new image automatically overwrites the old file in Firebase Storage
 * since both use the same path: posters/{eventId}.jpg</p>
 */
public class UpdatePosterController {

    /**
     * Result object returned after updating a poster.
     * Contains success status, error message (if any), and the updated event (if successful).
     */
    public static class UpdatePosterResult {
        private final boolean isSuccess;
        private final String errorMessage;
        private final Event updatedEvent;

        private UpdatePosterResult(boolean isSuccess, String errorMessage, Event updatedEvent) {
            this.isSuccess = isSuccess;
            this.errorMessage = errorMessage;
            this.updatedEvent = updatedEvent;
        }

        public static UpdatePosterResult success(Event updatedEvent) {
            return new UpdatePosterResult(true, null, updatedEvent);
        }

        public static UpdatePosterResult failure(String errorMessage) {
            return new UpdatePosterResult(false, errorMessage, null);
        }

        public boolean isSuccess() {
            return isSuccess;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Event getUpdatedEvent() {
            return updatedEvent;
        }
    }

    /**
     * Callback interface for poster update operations.
     */
    public interface Callback {
        void onResult(UpdatePosterResult result);
    }

    private final EventDB eventDB;
    private final PosterStorage posterStorage;

    /**
     * Constructs an UpdatePosterController with the given dependencies.
     *
     * @param eventDB the EventDB instance for updating events
     * @param posterStorage the PosterStorage instance for uploading images
     */
    public UpdatePosterController(EventDB eventDB, PosterStorage posterStorage) {
        this.eventDB = eventDB;
        this.posterStorage = posterStorage;
    }

    /**
     * Updates the poster for an event.
     * 
     * <p>This method:
     * <ol>
     *   <li>Uploads the new image to Firebase Storage (overwrites existing file)</li>
     *   <li>Updates the event document with the new poster URL</li>
     * </ol>
     * </p>
     *
     * @param event the event to update (must have a valid ID)
     * @param imageUri the URI of the new poster image
     * @param callback callback that receives the result of the operation
     */
    public void updatePoster(Event event, Uri imageUri, Callback callback) {
        if (event == null || event.getId() == null || event.getId().isEmpty()) {
            callback.onResult(UpdatePosterResult.failure("Event is invalid"));
            return;
        }

        if (imageUri == null) {
            callback.onResult(UpdatePosterResult.failure("Image URI cannot be null"));
            return;
        }

        // Step 1: Upload poster to Firebase Storage
        posterStorage.uploadPoster(event.getId(), imageUri, new PosterStorage.Callback<String>() {
            @Override
            public void onSuccess(String posterUrl) {
                // Step 2: Update event with new poster URL
                event.setPosterUrl(posterUrl);
                eventDB.addEvent(event, new EventDB.Callback<Void>() {
                    @Override
                    public void onSuccess(Void value) {
                        callback.onResult(UpdatePosterResult.success(event));
                    }

                    @Override
                    public void onError(@NonNull Exception e) {
                        callback.onResult(UpdatePosterResult.failure(
                                "Failed to update event: " + e.getMessage()));
                    }
                });
            }

            @Override
            public void onError(@NonNull Exception e) {
                String errorMessage = "Failed to upload poster";
                if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    errorMessage = e.getMessage();
                }
                callback.onResult(UpdatePosterResult.failure(errorMessage));
            }
        });
    }
}

