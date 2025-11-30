package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.NotifyEnrolledController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class NotifyEnrolledControllerTests {

    private EventDB mockEventDb;
    private EntrantDB mockEntrantDb;
    private NotifyEnrolledController controller;

    @Before
    public void setUp() {
        mockEventDb = mock(EventDB.class);
        mockEntrantDb = mock(EntrantDB.class);
        controller = new NotifyEnrolledController(mockEventDb, mockEntrantDb);
    }

    @Test
    public void validateMessage_nullOrEmpty_fails() {
        NotifyEnrolledController.ValidationResult res1 = controller.validateMessage(null);
        assertFalse(res1.isValid());
        assertEquals("Message cannot be empty", res1.getErrorMessage());

        NotifyEnrolledController.ValidationResult res2 = controller.validateMessage("   ");
        assertFalse(res2.isValid());
        assertEquals("Message cannot be empty", res2.getErrorMessage());
    }

    @Test
    public void validateMessage_tooLong_fails() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 501; i++) {
            sb.append("x");
        }
        String longMsg = sb.toString();

        NotifyEnrolledController.ValidationResult res = controller.validateMessage(longMsg);
        assertFalse(res.isValid());
        assertEquals("Message cannot exceed 500 characters", res.getErrorMessage());
    }

    @Test
    public void validateMessage_valid_succeeds() {
        NotifyEnrolledController.ValidationResult res = controller.validateMessage("Valid message");
        assertTrue(res.isValid());
        assertNull(res.getErrorMessage());
    }

    @Test
    public void notifyEnrolled_delegatesToNotificationController() {
        NotifyEnrolledController.NotifyEnrolledCallback cb =
                mock(NotifyEnrolledController.NotifyEnrolledCallback.class);

        controller.notifyEnrolled("event-123", "Test message", cb);

        verify(mockEventDb).getEnrolled(eq("event-123"), any());
    }
}
