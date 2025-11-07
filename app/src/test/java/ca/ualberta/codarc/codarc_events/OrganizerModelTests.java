package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.models.Organizer;
import org.junit.Test;

import static org.junit.Assert.*;

public class OrganizerModelTests {

    @Test
    public void defaultConstructor_setsBannedFalse_andDeviceIdNull() {
        Organizer o = new Organizer();
        assertNull(o.getDeviceId());
        assertFalse(o.isBanned());
    }

    @Test
    public void constructorWithDeviceId_setsDeviceId_andBannedFalse() {
        Organizer o = new Organizer("org-123");
        assertEquals("org-123", o.getDeviceId());
        assertFalse(o.isBanned());
    }

    @Test
    public void setters_roundTripValues() {
        Organizer o = new Organizer();

        o.setDeviceId("dev-999");
        o.setBanned(true);

        assertEquals("dev-999", o.getDeviceId());
        assertTrue(o.isBanned());

        o.setBanned(false);
        assertFalse(o.isBanned());
    }

    @Test
    public void setters_allowNullDeviceId() {
        Organizer o = new Organizer("temp");
        o.setDeviceId(null);
        assertNull(o.getDeviceId());
    }
}
