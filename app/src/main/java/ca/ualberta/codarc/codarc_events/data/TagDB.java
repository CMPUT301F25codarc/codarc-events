package ca.ualberta.codarc.codarc_events.data;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages tags collection in Firestore.
 * Maintains a separate tags collection for efficient tag queries.
 */
public class TagDB {

    public interface Callback<T> {
        void onSuccess(T value);
        void onError(@NonNull Exception e);
    }

    private final FirebaseFirestore db;

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
                    
                    tags.addAll(ca.ualberta.codarc.codarc_events.utils.TagHelper.getPredefinedTags());
                    
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
     * Adds tags to the tags collection.
     *
     * @param tags list of tags to add
     * @param cb callback for completion
     */
    public void addTags(List<String> tags, Callback<Void> cb) {
        if (tags == null || tags.isEmpty()) {
            cb.onSuccess(null);
            return;
        }

        WriteBatch batch = db.batch();

        for (String tag : tags) {
            if (tag == null || tag.trim().isEmpty()) {
                continue;
            }

            if (ca.ualberta.codarc.codarc_events.utils.TagHelper.isPredefinedTag(tag)) {
                continue;
            }

            String normalizedTag = ca.ualberta.codarc.codarc_events.utils.TagHelper.normalizeTag(tag);
            DocumentReference tagRef = db.collection("tags").document(normalizedTag);

            Map<String, Object> tagData = new HashMap<>();
            tagData.put("usageCount", FieldValue.increment(1));
            tagData.put("createdAt", FieldValue.serverTimestamp());
            batch.set(tagRef, tagData, SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Removes tags from the tags collection.
     *
     * @param tags list of tags to remove
     * @param cb callback for completion
     */
    public void removeTags(List<String> tags, Callback<Void> cb) {
        if (tags == null || tags.isEmpty()) {
            cb.onSuccess(null);
            return;
        }

        WriteBatch batch = db.batch();

        for (String tag : tags) {
            if (tag == null || tag.trim().isEmpty()) {
                continue;
            }

            if (ca.ualberta.codarc.codarc_events.utils.TagHelper.isPredefinedTag(tag)) {
                continue;
            }

            String normalizedTag = ca.ualberta.codarc.codarc_events.utils.TagHelper.normalizeTag(tag);
            DocumentReference tagRef = db.collection("tags").document(normalizedTag);

            batch.update(tagRef, "usageCount", FieldValue.increment(-1));
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> cb.onSuccess(null))
                .addOnFailureListener(cb::onError);
    }

    /**
     * Updates tags when an event's tags are modified.
     *
     * @param oldTags previous tags
     * @param newTags new tags
     * @param cb callback for completion
     */
    public void updateTags(List<String> oldTags, List<String> newTags, Callback<Void> cb) {
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

