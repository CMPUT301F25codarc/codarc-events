package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.models.User;
import org.junit.Test;

import static org.junit.Assert.*;

public class UserModelTests {

    @Test
    public void defaultConstructor_initializesAllFlagsFalse_andDeviceIdNull() {
        User u = new User();

        assertNull(u.getDeviceId());
        assertFalse(u.isEntrant());
        assertFalse(u.isOrganizer());
        assertFalse(u.isAdmin());
    }

    @Test
    public void constructorWithDeviceId_setsDeviceId_andFlagsFalse() {
        User u = new User("user-001");

        assertEquals("user-001", u.getDeviceId());
        assertFalse(u.isEntrant());
        assertFalse(u.isOrganizer());
        assertFalse(u.isAdmin());
    }

    @Test
    public void setters_roundTripValues() {
        User u = new User();

        u.setDeviceId("device-123");
        u.setEntrant(true);
        u.setOrganizer(true);
        u.setAdmin(true);

        assertEquals("device-123", u.getDeviceId());
        assertTrue(u.isEntrant());
        assertTrue(u.isOrganizer());
        assertTrue(u.isAdmin());

        // flip back to false
        u.setEntrant(false);
        u.setOrganizer(false);
        u.setAdmin(false);

        assertFalse(u.isEntrant());
        assertFalse(u.isOrganizer());
        assertFalse(u.isAdmin());
    }

    @Test
    public void allowsNullDeviceId() {
        User u = new User("temp");
        u.setDeviceId(null);
        assertNull(u.getDeviceId());
    }
}
