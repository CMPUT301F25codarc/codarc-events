package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.models.Entrant;
import com.google.firebase.firestore.PropertyName;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class EntrantModelTests {

    @Test
    public void defaultConstructor_setsSafeDefaults() {
        Entrant e = new Entrant();

        // Defaults are null/zero/false except strings you set later
        assertNull(e.getDeviceId());
        assertNull(e.getName());
        assertEquals(0L, e.getCreatedAtUtc());
        assertNull(e.getEmail());
        assertNull(e.getPhone());
        assertFalse(e.getIsRegistered());
        assertFalse(e.isBanned());
    }

    @Test
    public void convenienceConstructor_setsCoreFields_andInitialDefaults() {
        long ts = 1234567890L;
        Entrant e = new Entrant("dev123", "Taylor Swift", ts);

        assertEquals("dev123", e.getDeviceId());
        assertEquals("Taylor Swift", e.getName());
        assertEquals(ts, e.getCreatedAtUtc());

        // These default to empty strings in the constructor
        assertEquals("", e.getEmail());
        assertEquals("", e.getPhone());

        // Flags default false
        assertFalse(e.getIsRegistered());
        assertFalse(e.isBanned());
    }

    @Test
    public void settersAndGetters_roundTripValues() {
        Entrant e = new Entrant();

        e.setDeviceId("dev42");
        e.setName("Alyx Vance");
        e.setCreatedAtUtc(42L);
        e.setCreatedAt(99L); // alias setter should overwrite
        e.setEmail("alyx@city17.example");
        e.setPhone("555-1234");
        e.setIsRegistered(true);
        e.setBanned(true);

        assertEquals("dev42", e.getDeviceId());
        assertEquals("Alyx Vance", e.getName());
        // alias getter matches utc field
        assertEquals(99L, e.getCreatedAtUtc());
        assertEquals(99L, e.getCreatedAt());
        assertEquals("alyx@city17.example", e.getEmail());
        assertEquals("555-1234", e.getPhone());
        assertTrue(e.getIsRegistered());
        assertTrue(e.isBanned());
    }

    @Test
    public void setters_allowNulls_forOptionalStrings() {
        Entrant e = new Entrant();

        e.setEmail(null);
        e.setPhone(null);
        e.setName(null);
        e.setDeviceId(null);

        assertNull(e.getEmail());
        assertNull(e.getPhone());
        assertNull(e.getName());
        assertNull(e.getDeviceId());
    }

    @Test
    public void propertyNameAnnotations_present_andCorrect() throws Exception {
        // createdAtUtc mappings
        Method getCreatedAtUtc = Entrant.class.getMethod("getCreatedAtUtc");
        Method setCreatedAtUtc = Entrant.class.getMethod("setCreatedAtUtc", long.class);
        PropertyName getCreatedAnn = getCreatedAtUtc.getAnnotation(PropertyName.class);
        PropertyName setCreatedAnn = setCreatedAtUtc.getAnnotation(PropertyName.class);
        assertNotNull(getCreatedAnn);
        assertNotNull(setCreatedAnn);
        assertEquals("createdAtUtc", getCreatedAnn.value());
        assertEquals("createdAtUtc", setCreatedAnn.value());

        // is_registered mappings
        Method getIsRegistered = Entrant.class.getMethod("getIsRegistered");
        Method setIsRegistered = Entrant.class.getMethod("setIsRegistered", boolean.class);
        PropertyName getRegAnn = getIsRegistered.getAnnotation(PropertyName.class);
        PropertyName setRegAnn = setIsRegistered.getAnnotation(PropertyName.class);
        assertNotNull(getRegAnn);
        assertNotNull(setRegAnn);
        assertEquals("is_registered", getRegAnn.value());
        assertEquals("is_registered", setRegAnn.value());

        // banned mappings
        Method isBanned = Entrant.class.getMethod("isBanned");
        Method setBanned = Entrant.class.getMethod("setBanned", boolean.class);
        PropertyName getBannedAnn = isBanned.getAnnotation(PropertyName.class);
        PropertyName setBannedAnn = setBanned.getAnnotation(PropertyName.class);
        assertNotNull(getBannedAnn);
        assertNotNull(setBannedAnn);
        assertEquals("banned", getBannedAnn.value());
        assertEquals("banned", setBannedAnn.value());
    }

    @Test
    public void createdAt_aliasMethods_stayInSync() {
        Entrant e = new Entrant();

        e.setCreatedAtUtc(111L);
        assertEquals(111L, e.getCreatedAt());
        assertEquals(111L, e.getCreatedAtUtc());

        e.setCreatedAt(222L);
        assertEquals(222L, e.getCreatedAt());
        assertEquals(222L, e.getCreatedAtUtc());
    }
}
