package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.models.NotificationEntry;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

public class NotificationEntryModelTests {

    @Test
    public void defaultValues_areZeroOrNullOrFalse() {
        NotificationEntry n = new NotificationEntry();

        assertNull(n.getId());
        assertNull(n.getEventId());
        assertNull(n.getEventName());
        assertNull(n.getMessage());
        assertNull(n.getCategory());
        assertEquals(0L, n.getCreatedAt());
        assertFalse(n.isRead());
        assertNull(n.getResponse());
        assertEquals(0L, n.getRespondedAt());
        assertFalse(n.isProcessing()); // transient default should be false
    }

    @Test
    public void setters_roundTripValues() {
        NotificationEntry n = new NotificationEntry();

        n.setId("notif-1");
        n.setEventId("event-42");
        n.setEventName("Pumpkin Spice Coding Night");
        n.setMessage("You’ve been selected. Reply by Friday.");
        n.setCategory("invite");
        n.setCreatedAt(1730937600000L);
        n.setRead(true);
        n.setResponse("accepted");
        n.setRespondedAt(1731024000000L);
        n.setProcessing(true);

        assertEquals("notif-1", n.getId());
        assertEquals("event-42", n.getEventId());
        assertEquals("Pumpkin Spice Coding Night", n.getEventName());
        assertEquals("You’ve been selected. Reply by Friday.", n.getMessage());
        assertEquals("invite", n.getCategory());
        assertEquals(1730937600000L, n.getCreatedAt());
        assertTrue(n.isRead());
        assertEquals("accepted", n.getResponse());
        assertEquals(1731024000000L, n.getRespondedAt());
        assertTrue(n.isProcessing());
    }

    @Test
    public void setters_allowNullsForStrings() {
        NotificationEntry n = new NotificationEntry();

        n.setId(null);
        n.setEventId(null);
        n.setEventName(null);
        n.setMessage(null);
        n.setCategory(null);
        n.setResponse(null);

        assertNull(n.getId());
        assertNull(n.getEventId());
        assertNull(n.getEventName());
        assertNull(n.getMessage());
        assertNull(n.getCategory());
        assertNull(n.getResponse());
    }

    @Test
    public void serialization_roundTripsPersistentFields_butDropsTransientProcessing() throws Exception {
        NotificationEntry original = new NotificationEntry();
        original.setId("n-7");
        original.setEventId("e-9");
        original.setEventName("LAN Party");
        original.setMessage("pls RSVP");
        original.setCategory("reminder");
        original.setCreatedAt(111L);
        original.setRead(true);
        original.setResponse("declined");
        original.setRespondedAt(222L);
        original.setProcessing(true); // should NOT survive serialization

        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(original);
            oos.flush();
            bytes = bos.toByteArray();
        }

        NotificationEntry copy;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            copy = (NotificationEntry) ois.readObject();
        }

        // Not the same instance
        assertNotSame(original, copy);

        // Persistent fields preserved
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getEventId(), copy.getEventId());
        assertEquals(original.getEventName(), copy.getEventName());
        assertEquals(original.getMessage(), copy.getMessage());
        assertEquals(original.getCategory(), copy.getCategory());
        assertEquals(original.getCreatedAt(), copy.getCreatedAt());
        assertEquals(original.isRead(), copy.isRead());
        assertEquals(original.getResponse(), copy.getResponse());
        assertEquals(original.getRespondedAt(), copy.getRespondedAt());

        // Transient flag resets to default false after deserialization
        assertTrue(original.isProcessing());
        assertFalse(copy.isProcessing());
    }
}
