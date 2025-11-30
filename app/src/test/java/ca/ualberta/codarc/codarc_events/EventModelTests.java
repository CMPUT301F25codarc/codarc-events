package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.models.Event;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.*;

public class EventModelTests {

    @Test
    public void defaultConstructor_startsWithNullsAndFalse() {
        Event e = new Event();

        assertNull(e.getId());
        assertNull(e.getName());
        assertNull(e.getDescription());
        assertNull(e.getEventDateTime());
        assertNull(e.getRegistrationOpen());
        assertNull(e.getRegistrationClose());
        assertFalse(e.isOpen());
        assertNull(e.getOrganizerId());
        assertNull(e.getQrCode());
        assertNull(e.getMaxCapacity());
        assertNull(e.getLocation());
    }

    @Test
    public void fullConstructor_setsAllProvidedFields() {
        Event e = new Event(
                "id123",
                "Lan Party",
                "Bring snacks",
                "2025-12-31T23:59:00",
                "2025-12-01T00:00:00",
                "2025-12-30T23:59:59",
                true,
                "org-abc",
                "event:id123"
        );

        assertEquals("id123", e.getId());
        assertEquals("Lan Party", e.getName());
        assertEquals("Bring snacks", e.getDescription());
        assertEquals("2025-12-31T23:59:00", e.getEventDateTime());
        assertEquals("2025-12-01T00:00:00", e.getRegistrationOpen());
        assertEquals("2025-12-30T23:59:59", e.getRegistrationClose());
        assertTrue(e.isOpen());
        assertEquals("org-abc", e.getOrganizerId());
        assertEquals("event:id123", e.getQrCode());
        assertNull(e.getMaxCapacity());
        assertNull(e.getLocation());
    }

    @Test
    public void setters_roundTripValues() {
        Event e = new Event();

        e.setId("E-001");
        e.setName("Code Jam");
        e.setDescription("Solve puzzles. Win clout.");
        e.setEventDateTime("2026-01-15T18:00:00");
        e.setRegistrationOpen("2026-01-01T00:00:00");
        e.setRegistrationClose("2026-01-14T23:59:59");
        e.setOpen(true);
        e.setOrganizerId("uofa-cs");
        e.setQrCode("event:E-001");
        e.setMaxCapacity(120);
        e.setLocation("CAB 265");

        assertEquals("E-001", e.getId());
        assertEquals("Code Jam", e.getName());
        assertEquals("Solve puzzles. Win clout.", e.getDescription());
        assertEquals("2026-01-15T18:00:00", e.getEventDateTime());
        assertEquals("2026-01-01T00:00:00", e.getRegistrationOpen());
        assertEquals("2026-01-14T23:59:59", e.getRegistrationClose());
        assertTrue(e.isOpen());
        assertEquals("uofa-cs", e.getOrganizerId());
        assertEquals("event:E-001", e.getQrCode());
        assertEquals(Integer.valueOf(120), e.getMaxCapacity());
        assertEquals("CAB 265", e.getLocation());
    }

    @Test
    public void setters_allowNullsForOptionalFields() {
        Event e = new Event();

        e.setDescription(null);
        e.setEventDateTime(null);
        e.setRegistrationOpen(null);
        e.setRegistrationClose(null);
        e.setQrCode(null);
        e.setMaxCapacity(null);
        e.setLocation(null);

        assertNull(e.getDescription());
        assertNull(e.getEventDateTime());
        assertNull(e.getRegistrationOpen());
        assertNull(e.getRegistrationClose());
        assertNull(e.getQrCode());
        assertNull(e.getMaxCapacity());
        assertNull(e.getLocation());
    }

    @Test
    public void serializable_roundTripsAllFields() throws Exception {
        Event original = new Event(
                "idX",
                "Hack Night",
                "BYO laptop",
                "2025-11-15T19:00:00",
                "2025-11-01T00:00:00",
                "2025-11-14T23:59:59",
                false,
                "orgX",
                "event:idX"
        );
        original.setMaxCapacity(50);
        original.setLocation("CCIS 1-160");

        byte[] bytes;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(original);
            oos.flush();
            bytes = bos.toByteArray();
        }

        Event copy;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            copy = (Event) ois.readObject();
        }

        assertNotSame(original, copy);
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getName(), copy.getName());
        assertEquals(original.getDescription(), copy.getDescription());
        assertEquals(original.getEventDateTime(), copy.getEventDateTime());
        assertEquals(original.getRegistrationOpen(), copy.getRegistrationOpen());
        assertEquals(original.getRegistrationClose(), copy.getRegistrationClose());
        assertEquals(original.isOpen(), copy.isOpen());
        assertEquals(original.getOrganizerId(), copy.getOrganizerId());
        assertEquals(original.getQrCode(), copy.getQrCode());
        assertEquals(original.getMaxCapacity(), copy.getMaxCapacity());
        assertEquals(original.getLocation(), copy.getLocation());
    }
}
