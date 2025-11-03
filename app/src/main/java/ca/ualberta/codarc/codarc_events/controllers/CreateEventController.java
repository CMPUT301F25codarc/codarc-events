package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import java.util.UUID;

import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Controller for creating events.
 * Handles validation and business logic for event creation.
 * Separated from UI to enable unit testing.
 */
public class CreateEventController {

    /**
     * Result class for event creation validation.
     */
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

    /**
     * Constructs a CreateEventController.
     *
     * @param eventDB the EventDB instance for database operations
     * @param organizerId the ID of the organizer creating the event
     */
    public CreateEventController(EventDB eventDB, String organizerId) {
        this.eventDB = eventDB;
        this.organizerId = organizerId;
    }

    /**
     * Validates event data and creates an Event object.
     * Does not persist to database - caller should call persistEvent() after validation.
     *
     * @param name event name
     * @param description event description
     * @param dateTime event date/time (ISO format string)
     * @param location event location
     * @param regOpen registration open date/time (ISO format string)
     * @param regClose registration close date/time (ISO format string)
     * @param capacityStr capacity as string (can be empty/null)
     * @return CreateEventResult containing validation status and Event if valid
     */
    public CreateEventResult validateAndCreateEvent(String name, String description,
                                                     String dateTime, String location,
                                                     String regOpen, String regClose,
                                                     String capacityStr) {
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

        return CreateEventResult.success(event);
    }

    /**
     * Persists an event to the database.
     *
     * @param event the event to persist
     * @param callback callback for success/error
     */
    public void persistEvent(Event event, EventDB.Callback<Void> callback) {
        if (event == null) {
            callback.onError(new IllegalArgumentException("Event cannot be null"));
            return;
        }
        eventDB.addEvent(event, callback);
    }
}
