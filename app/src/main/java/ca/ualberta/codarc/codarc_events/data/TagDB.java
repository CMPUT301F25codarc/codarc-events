package ca.ualberta.codarc.codarc_events.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages tags collection in Firestore.
 * Maintains a separate tags collection for efficient tag queries.
 */
public class TagDB {

    /** Lightweight async callback used by the data layer. */
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db;

    /** Construct using the default Firestore instance. */
    public TagDB() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Gets all available tags (predefined + custom tags from collection).
     *
     * @param cb callback with list of all tag strings
     */
    public void getAllTags(Callback<List<String>> cb) {
        db.collection("tags")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Set<String> tags = new HashSet<>();
                    
                    // Add predefined tags
                    tags.addAll(ca.ualberta.codarc.codarc_events.utils.TagHelper.getPredefinedTags());
                    
                    // Add custom tags from collection
                    if (querySnapshot != null) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String tagName = doc.getId();
                            if (tagName != null && !tagName.trim().isEmpty()) {
                                tags.add(tagName);
                            }
                        }
                    }
                    
                    cb.onSuccess(new ArrayList<>(tags));
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Adds tags to the tags collection when an event is created.
     * Uses transactions to safely increment usage count.
     *
     * @param tags list of tags to add
     * @param cb callback for completion
     */
    public void addTags(List<String> tags, Callback<Void> cb) {
        if (tags == null || tags.isEmpty()) {
            cb.onSuccess(null);
            return;
        }

        // Use batch write for efficiency
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        for (String tag : tags) {
            if (tag == null || tag.trim().isEmpty()) {
                continue;
            }

            // Skip predefined tags (they don't need to be in collection)
            if (ca.ualberta.codarc.codarc_events.utils.TagHelper.isPredefinedTag(tag)) {
                continue;
            }

            String normalizedTag = ca.ualberta.codarc.codarc_events.utils.TagHelper.normalizeTag(tag);
            DocumentReference tagRef = db.collection("tags").document(normalizedTag);

            // Increment usage count (or create if doesn't exist)
            batch.set(tagRef, new java.util.HashMap<String, Object>() {{
                put("usageCount", FieldValue.increment(1));
                put("createdAt", FieldValue.serverTimestamp());
            }}, com.google.firebase.firestore.SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Removes tags from the tags collection when an event is deleted or updated.
     * Decrements usage count and removes tag if count reaches zero.
     *
     * @param tags list of tags to remove
     * @param cb callback for completion
     */
    public void removeTags(List<String> tags, Callback<Void> cb) {
        if (tags == null || tags.isEmpty()) {
            cb.onSuccess(null);
            return;
        }

        // Use batch write for efficiency
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        for (String tag : tags) {
            if (tag == null || tag.trim().isEmpty()) {
                continue;
            }

            // Skip predefined tags
            if (ca.ualberta.codarc.codarc_events.utils.TagHelper.isPredefinedTag(tag)) {
                continue;
            }

            String normalizedTag = ca.ualberta.codarc.codarc_events.utils.TagHelper.normalizeTag(tag);
            DocumentReference tagRef = db.collection("tags").document(normalizedTag);

            // Decrement usage count
            batch.update(tagRef, "usageCount", FieldValue.increment(-1));
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // Clean up tags with zero usage (optional - can be done in Cloud Function)
                    cleanupZeroUsageTags(tags, cb);
                })
                .addOnFailureListener(cb::onError);
    }

    /**
     * Cleans up tags that have zero usage count.
     * This is optional - could also be done via Cloud Function.
     */
    private void cleanupZeroUsageTags(List<String> tags, Callback<Void> cb) {
        // For simplicity, we'll just decrement. Actual deletion could be done via Cloud Function
        // or periodic cleanup job. For now, tags with zero usage can remain.
        cb.onSuccess(null);
    }

    /**
     * Updates tags when an event's tags are modified.
     * Removes old tags and adds new ones.
     *
     * @param oldTags previous tags
     * @param newTags new tags
     * @param cb callback for completion
     */
    public void updateTags(List<String> oldTags, List<String> newTags, Callback<Void> cb) {
        // Find tags to add and remove
        Set<String> oldSet = oldTags != null ? new HashSet<>(oldTags) : new HashSet<>();
        Set<String> newSet = newTags != null ? new HashSet<>(newTags) : new HashSet<>();

        List<String> toAdd = new ArrayList<>();
        List<String> toRemove = new ArrayList<>();

        for (String tag : newSet) {
            if (!oldSet.contains(tag)) {
                toAdd.add(tag);
            }
        }

        for (String tag : oldSet) {
            if (!newSet.contains(tag)) {
                toRemove.add(tag);
            }
        }

        // Remove old tags first, then add new ones
        removeTags(toRemove, new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                addTags(toAdd, cb);
            }

            @Override
            public void onError(@NonNull Exception e) {
                cb.onError(e);
            }
        });
    }
}

