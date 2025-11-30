package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.QRScanController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class QRScanControllerTests {

    private QRScanController controller;
    private EventDB mockEventDb;

    @Before
    public void setUp() {
        controller = new QRScanController();
        mockEventDb = mock(EventDB.class);
    }

    @Test
    public void parseEventIdFromQR_nullOrEmpty_returnsNull() {
        assertNull(QRScanController.parseEventIdFromQR(null));
        assertNull(QRScanController.parseEventIdFromQR(""));
        assertNull(QRScanController.parseEventIdFromQR("   "));
    }

    @Test
    public void parseEventIdFromQR_plainId_returnsTrimmed() {
        assertEquals("E1", QRScanController.parseEventIdFromQR("E1"));
        assertEquals("E1", QRScanController.parseEventIdFromQR("  E1  "));
    }

    @Test
    public void parseEventIdFromQR_prefixedFormat_parsesId() {
        assertEquals("E123", QRScanController.parseEventIdFromQR("event:E123"));
        assertEquals("E123", QRScanController.parseEventIdFromQR("  event: E123  "));
    }

    @Test
    public void parseEventIdFromQR_prefixedWithoutId_returnsNull() {
        assertNull(QRScanController.parseEventIdFromQR("event:"));
        assertNull(QRScanController.parseEventIdFromQR("event:   "));
    }

    @Test
    public void validateQRCode_nullOrEmpty_failsWithMessage() {
        QRScanController.QRScanResult res1 = controller.validateQRCode(null);
        assertFalse(res1.isSuccess());
        assertEquals("QR code is empty", res1.getErrorMessage());
        assertNull(res1.getEvent());
        assertNull(res1.getEventId());

        QRScanController.QRScanResult res2 = controller.validateQRCode("   ");
        assertFalse(res2.isSuccess());
        assertEquals("QR code is empty", res2.getErrorMessage());
        assertNull(res2.getEvent());
        assertNull(res2.getEventId());
    }

    @Test
    public void validateQRCode_invalidFormat_prefixedWithoutId_fails() {
        QRScanController.QRScanResult res =
                controller.validateQRCode("event:   ");

        assertFalse(res.isSuccess());
        assertEquals("Invalid QR code format. Please scan a valid event QR code.", res.getErrorMessage());
        assertNull(res.getEvent());
        assertNull(res.getEventId());
    }

    @Test
    public void validateQRCode_validPlainId_succeeds() {
        QRScanController.QRScanResult res =
                controller.validateQRCode("E42");

        assertTrue(res.isSuccess());
        assertNull(res.getErrorMessage());
        assertNull(res.getEvent());
        assertEquals("E42", res.getEventId());
    }

    @Test
    public void validateQRCode_validPrefixedId_succeeds() {
        QRScanController.QRScanResult res =
                controller.validateQRCode("event:E999");

        assertTrue(res.isSuccess());
        assertNull(res.getErrorMessage());
        assertNull(res.getEvent());
        assertEquals("E999", res.getEventId());
    }

    @Test
    public void fetchEventFromQR_invalidQR_returnsValidationFailure_andSkipsDb() {
        QRScanController.Callback cb = mock(QRScanController.Callback.class);

        controller.fetchEventFromQR("   ", mockEventDb, cb);

        ArgumentCaptor<QRScanController.QRScanResult> resCap =
                ArgumentCaptor.forClass(QRScanController.QRScanResult.class);
        verify(cb).onResult(resCap.capture());

        QRScanController.QRScanResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("QR code is empty", res.getErrorMessage());
        assertNull(res.getEvent());
        assertNull(res.getEventId());

        verifyNoInteractions(mockEventDb);
    }

    @Test
    public void fetchEventFromQR_validQR_eventFound_returnsSuccess() {
        QRScanController.Callback cb = mock(QRScanController.Callback.class);

        controller.fetchEventFromQR("event:E1", mockEventDb, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> dbCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq("E1"), dbCap.capture());

        Event event = new Event();
        dbCap.getValue().onSuccess(event);

        ArgumentCaptor<QRScanController.QRScanResult> resCap =
                ArgumentCaptor.forClass(QRScanController.QRScanResult.class);
        verify(cb, times(1)).onResult(resCap.capture());

        QRScanController.QRScanResult res = resCap.getValue();
        assertTrue(res.isSuccess());
        assertNull(res.getErrorMessage());
        assertEquals(event, res.getEvent());
        assertEquals("E1", res.getEventId());
    }

    @Test
    public void fetchEventFromQR_validQR_eventNull_returnsNotFoundFailure() {
        QRScanController.Callback cb = mock(QRScanController.Callback.class);

        controller.fetchEventFromQR("event:E1", mockEventDb, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> dbCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq("E1"), dbCap.capture());

        dbCap.getValue().onSuccess(null);

        ArgumentCaptor<QRScanController.QRScanResult> resCap =
                ArgumentCaptor.forClass(QRScanController.QRScanResult.class);
        verify(cb).onResult(resCap.capture());

        QRScanController.QRScanResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals(
                "Event not found. The QR code may be invalid or the event may have been deleted.",
                res.getErrorMessage()
        );
        assertNull(res.getEvent());
        assertNull(res.getEventId());
    }

    @Test
    public void fetchEventFromQR_dbError_genericMessageWhenNotContainingNotFound() {
        QRScanController.Callback cb = mock(QRScanController.Callback.class);

        controller.fetchEventFromQR("E1", mockEventDb, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> dbCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq("E1"), dbCap.capture());

        dbCap.getValue().onError(new RuntimeException("firestore offline"));

        ArgumentCaptor<QRScanController.QRScanResult> resCap =
                ArgumentCaptor.forClass(QRScanController.QRScanResult.class);
        verify(cb).onResult(resCap.capture());

        QRScanController.QRScanResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals(
                "Failed to load event. Please check your connection and try again.",
                res.getErrorMessage()
        );
        assertNull(res.getEvent());
        assertNull(res.getEventId());
    }

    @Test
    public void fetchEventFromQR_dbError_notFoundMessageUsesNotFoundCopy() {
        QRScanController.Callback cb = mock(QRScanController.Callback.class);

        controller.fetchEventFromQR("E1", mockEventDb, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> dbCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq("E1"), dbCap.capture());

        dbCap.getValue().onError(new RuntimeException("document not found in collection"));

        ArgumentCaptor<QRScanController.QRScanResult> resCap =
                ArgumentCaptor.forClass(QRScanController.QRScanResult.class);
        verify(cb).onResult(resCap.capture());

        QRScanController.QRScanResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals(
                "Event not found. The QR code may be invalid or the event may have been deleted.",
                res.getErrorMessage()
        );
        assertNull(res.getEvent());
        assertNull(res.getEventId());
    }
}
