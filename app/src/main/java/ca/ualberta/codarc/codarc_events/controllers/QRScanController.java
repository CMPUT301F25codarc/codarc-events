package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Handles QR code scanning - parsing, validation, and event lookup logic.
 */
public class QRScanController {

    public static class QRScanResult {
        private final boolean isSuccess;
        private final String errorMessage;
        private final Event event;
        private final String eventId;

        private QRScanResult(boolean isSuccess, String errorMessage, Event event, String eventId) {
            this.isSuccess = isSuccess;
            this.errorMessage = errorMessage;
            this.event = event;
            this.eventId = eventId;
        }

        public static QRScanResult success(Event event, String eventId) {
            return new QRScanResult(true, null, event, eventId);
        }

        public static QRScanResult failure(String errorMessage) {
            return new QRScanResult(false, errorMessage, null, null);
        }

        public boolean isSuccess() {
            return isSuccess;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Event getEvent() {
            return event;
        }

        public String getEventId() {
            return eventId;
        }
    }

    // The following function is from Anthropic Claude Sonnet 4.5, "How do I parse a QR code string with an 'event:' prefix and handle invalid formats in Java?", 2024-01-15
    /**
     * Parses an event ID from a QR code string.
     *
     * @param qrData the raw QR code string data
     * @return the extracted event ID, or null if the input is invalid
     */
    public static String parseEventIdFromQR(String qrData) {
        if (qrData == null || qrData.trim().isEmpty()) {
            return null;
        }

        String trimmed = qrData.trim();
        if (trimmed.startsWith("event:")) {
            String eventId = trimmed.substring(6).trim();
            return eventId.isEmpty() ? null : eventId;
        }

        return trimmed;
    }

    /**
     * Validates that QR code data is in an expected format.
     *
     * @param qrData the QR code string to validate
     * @return a QRScanResult indicating validation success or failure
     */
    public QRScanResult validateQRCode(String qrData) {
        if (qrData == null || qrData.trim().isEmpty()) {
            return QRScanResult.failure("QR code is empty");
        }

        String eventId = parseEventIdFromQR(qrData);
        if (eventId == null || eventId.isEmpty()) {
            return QRScanResult.failure("Invalid QR code format. Please scan a valid event QR code.");
        }

        return QRScanResult.success(null, eventId);
    }

    /**
     * Fetches an event from Firestore based on scanned QR code data.
     *
     * @param qrData the raw QR code string data
     * @param eventDB the EventDB instance to use for fetching
     * @param callback callback to receive the scan result
     */
    public void fetchEventFromQR(String qrData, EventDB eventDB, Callback callback) {
        QRScanResult validation = validateQRCode(qrData);
        if (!validation.isSuccess()) {
            callback.onResult(validation);
            return;
        }

        String eventId = validation.getEventId();

        eventDB.getEvent(eventId, new EventDB.Callback<Event>() {
            @Override
            public void onSuccess(Event event) {
                if (event != null) {
                    callback.onResult(QRScanResult.success(event, eventId));
                } else {
                    callback.onResult(QRScanResult.failure("Event not found. The QR code may be invalid or the event may have been deleted."));
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                String errorMessage = "Failed to load event. Please check your connection and try again.";
                if (e.getMessage() != null && e.getMessage().contains("not found")) {
                    errorMessage = "Event not found. The QR code may be invalid or the event may have been deleted.";
                }
                callback.onResult(QRScanResult.failure(errorMessage));
            }
        });
    }

    public interface Callback {
        void onResult(QRScanResult result);
    }
}

