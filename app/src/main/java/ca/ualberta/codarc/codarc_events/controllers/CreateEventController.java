package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.UUID;

import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Handles event creation - validation and business logic.
 */
public class CreateEventController {

    public static class CreateEventResult {
        private final boolean isValid;
        private final String errorMessage;
        private final Event event;

        private CreateEventResult(boolean isValid, String errorMessage, Event event) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.event = event;
        }

        public static CreateEventResult success(Event event) {
            return new CreateEventResult(true, null, event);
        }

        public static CreateEventResult failure(String errorMessage) {
            return new CreateEventResult(false, errorMessage, null);
        }

        public boolean isValid() {
            return isValid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Event getEvent() {
            return event;
        }
    }

    private final EventDB eventDB;
    private final String organizerId;

    public CreateEventController(EventDB eventDB, String organizerId) {
        this.eventDB = eventDB;
        this.organizerId = organizerId;
    }

    /**
     * Validates and creates Event object (doesn't save to DB yet).
     *
     * @param name event name
     * @param description event description
     * @param dateTime event date and time
     * @param location event location
     * @param regOpen registration open date
     * @param regClose registration close date
     * @param capacityStr maximum capacity as string
     * @param tags list of tags
     * @param posterUrl optional poster image URL
     * @param requireGeolocation whether location sharing is required
     * @return CreateEventResult with validation result
     */
    public CreateEventResult validateAndCreateEvent(String name, String description,
                                                    String dateTime, String location,
                                                    String regOpen, String regClose,
                                                    String capacityStr, List<String> tags,
                                                    String posterUrl, boolean requireGeolocation) {
        // Validate required fields
        if (name == null || name.trim().isEmpty()) {
            return CreateEventResult.failure("Event name is required");
        }
        if (dateTime == null || dateTime.trim().isEmpty()) {
            return CreateEventResult.failure("Event date/time is required");
        }
        if (regOpen == null || regOpen.trim().isEmpty()) {
            return CreateEventResult.failure("Registration open date is required");
        }
        if (regClose == null || regClose.trim().isEmpty()) {
            return CreateEventResult.failure("Registration close date is required");
        }

        // Generate event ID and QR code
        String id = UUID.randomUUID().toString();
        String qrData = "event:" + id;

        // Create Event object
        Event event = new Event();
        event.setId(id);
        event.setName(name.trim());
        event.setDescription(description != null ? description.trim() : "");
        event.setEventDateTime(dateTime.trim());
        event.setLocation(location != null ? location.trim() : "");
        event.setRegistrationOpen(regOpen.trim());
        event.setRegistrationClose(regClose.trim());
        event.setOrganizerId(organizerId);
        event.setQrCode(qrData);
        event.setOpen(true);
        event.setRequireGeolocation(requireGeolocation);

        // Parse capacity (optional field)
        if (capacityStr != null && !capacityStr.trim().isEmpty()) {
            try {
                Integer maxCap = Integer.parseInt(capacityStr.trim());
                event.setMaxCapacity(maxCap > 0 ? maxCap : null);
            } catch (NumberFormatException e) {
                event.setMaxCapacity(null);
            }
        } else {
            event.setMaxCapacity(null);
        }

        // Set tags (optional field)
        event.setTags(tags);

        // Set poster URL (optional field)
        event.setPosterUrl(posterUrl);

        return CreateEventResult.success(event);
    }

    // Saves event to Firestore
    public void persistEvent(Event event, EventDB.Callback<Void> callback) {
        if (event == null) {
            callback.onError(new IllegalArgumentException("Event cannot be null"));
            return;
        }
        eventDB.addEvent(event, callback);
    }

    /**
     * Validates that a tag can be added (not duplicate).
     * Business logic for tag validation.
     *
     * @param tag the tag to validate
     * @param existingTags list of already selected tags
     * @return true if tag is valid to add, false if duplicate
     */
    public boolean canAddTag(String tag, List<String> existingTags) {
        if (tag == null || tag.trim().isEmpty()) {
            return false;
        }

        String normalizedTag = ca.ualberta.codarc.codarc_events.utils.TagHelper.normalizeTag(tag);

        if (existingTags == null || existingTags.isEmpty()) {
            return true;
        }

        // Check if tag already exists (case-insensitive)
        for (String existingTag : existingTags) {
            if (ca.ualberta.codarc.codarc_events.utils.TagHelper.normalizeTag(existingTag).equals(normalizedTag)) {
                return false;
            }
        }

        return true;
    }
}