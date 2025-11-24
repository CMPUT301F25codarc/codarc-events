package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Handles QR code scanning - parsing, validation, and event lookup logic.
 * 
 * <p>This controller encapsulates the business logic for:
 * <ul>
 *   <li>Parsing event IDs from QR code strings</li>
 *   <li>Validating QR code format</li>
 *   <li>Fetching events from Firestore based on scanned QR codes</li>
 * </ul>
 * </p>
 */
public class QRScanController {

    /**
     * Result object returned after processing a QR code scan.
     * Contains success status, error message (if any), and the fetched event (if successful).
     */
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

        /**
         * Creates a successful scan result.
         *
         * @param event the event that was found
         * @param eventId the parsed event ID
         * @return a successful QRScanResult
         */
        public static QRScanResult success(Event event, String eventId) {
            return new QRScanResult(true, null, event, eventId);
        }

        /**
         * Creates a failed scan result.
         *
         * @param errorMessage the error message describing why the scan failed
         * @return a failed QRScanResult
         */
        public static QRScanResult failure(String errorMessage) {
            return new QRScanResult(false, errorMessage, null, null);
        }

        /**
         * @return true if the scan was successful, false otherwise
         */
        public boolean isSuccess() {
            return isSuccess;
        }

        /**
         * @return the error message if the scan failed, null otherwise
         */
        public String getErrorMessage() {
            return errorMessage;
        }

        /**
         * @return the event if the scan was successful, null otherwise
         */
        public Event getEvent() {
            return event;
        }

        /**
         * @return the parsed event ID if available, null otherwise
         */
        public String getEventId() {
            return eventId;
        }
    }

    /**
     * Parses an event ID from a QR code string.
     * 
     * <p>Supports two formats:
     * <ul>
     *   <li>Prefixed format: "event:{eventId}" - extracts the ID after the prefix</li>
     *   <li>Plain format: "{eventId}" - treats the entire string as the event ID</li>
     * </ul>
     * </p>
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
     * <p>This method:
     * <ol>
     *   <li>Validates the QR code format</li>
     *   <li>Parses the event ID from the QR code string</li>
     *   <li>Fetches the event from Firestore using EventDB</li>
     *   <li>Returns the result via callback</li>
     * </ol>
     * </p>
     *
     * @param qrData the raw QR code string data
     * @param eventDB the EventDB instance to use for fetching
     * @param callback callback to receive the scan result
     */
    public void fetchEventFromQR(String qrData, EventDB eventDB, Callback callback) {
        // Validate QR code format first
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

    /**
     * Callback interface for receiving QR scan results.
     */
    public interface Callback {
        /**
         * Called when the QR scan operation completes.
         *
         * @param result the result of the scan operation
         */
        void onResult(QRScanResult result);
    }
}

