package ca.ualberta.codarc.codarc_events;

import org.junit.Test;
import static org.junit.Assert.*;

import ca.ualberta.codarc.codarc_events.models.Entrant;

/**
 * Unit tests for the Entrant model class.
 * Verifies basic initialization and getter/setter functionality.
 */
public class EntrantTest {

    /**
     * Tests that an Entrant can be constructed with all required fields
     * and that getters return the expected values.
     */
    @Test
    public void entrantInitialization_isCorrect() {
        String deviceId = "ABC123";
        String name = "User";
        long timestamp = 1730399999L;

        Entrant entrant = new Entrant(deviceId, name, timestamp);

        assertEquals(deviceId, entrant.getDeviceId());
        assertEquals(name, entrant.getName());
        assertEquals(timestamp, entrant.getCreatedAt());
    }

    /**
     * Tests that the empty constructor produces a non-null Entrant instance,
     * as required by Firestore deserialization.
     */
    @Test
    public void entrantDefaultConstructor_isNotNull() {
        Entrant entrant = new Entrant();
        assertNotNull(entrant);
    }

    /**
     * Tests that setters properly update the Entrant's fields
     * and that getters reflect those changes.
     */
    @Test
    public void entrantSetters_updateFieldsCorrectly() {
        Entrant entrant = new Entrant();
        entrant.setDeviceId("XYZ789");
        entrant.setName("TestUser");
        entrant.setCreatedAt(123456789L);

        assertEquals("XYZ789", entrant.getDeviceId());
        assertEquals("TestUser", entrant.getName());
        assertEquals(123456789L, entrant.getCreatedAt());
    }
}


