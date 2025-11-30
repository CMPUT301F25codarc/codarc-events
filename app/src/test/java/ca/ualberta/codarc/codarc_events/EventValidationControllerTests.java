package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.EventValidationHelper;
import ca.ualberta.codarc.codarc_events.models.Event;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EventValidationControllerTests {

    private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

    // ---------- isWithinRegistrationWindow ----------

    @Test
    public void isWithinRegistrationWindow_trueWhenNowInsideWindow() throws Exception {
        Event e = new Event();
        long now = System.currentTimeMillis();
        e.setRegistrationOpen(iso.format(new Date(now - 1000)));   // opened 1s ago
        e.setRegistrationClose(iso.format(new Date(now + 1000)));  // closes 1s later

        assertTrue(EventValidationHelper.isWithinRegistrationWindow(e));
    }

    @Test
    public void isWithinRegistrationWindow_falseWhenNowBeforeOpen() throws Exception {
        Event e = new Event();
        long now = System.currentTimeMillis();
        e.setRegistrationOpen(iso.format(new Date(now + 5000)));   // opens in 5s
        e.setRegistrationClose(iso.format(new Date(now + 10000))); // closes later

        assertFalse(EventValidationHelper.isWithinRegistrationWindow(e));
    }

    @Test
    public void isWithinRegistrationWindow_falseWhenNowAfterClose() throws Exception {
        Event e = new Event();
        long now = System.currentTimeMillis();
        e.setRegistrationOpen(iso.format(new Date(now - 10000)));
        e.setRegistrationClose(iso.format(new Date(now - 1000))); // closed 1s ago

        assertFalse(EventValidationHelper.isWithinRegistrationWindow(e));
    }

    @Test
    public void isWithinRegistrationWindow_falseOnBadDateFormat() {
        Event e = new Event();
        e.setRegistrationOpen("not-a-date");
        e.setRegistrationClose("also-bad");

        assertFalse(EventValidationHelper.isWithinRegistrationWindow(e));
    }

    @Test
    public void isWithinRegistrationWindow_falseOnNullEventOrFields() {
        assertFalse(EventValidationHelper.isWithinRegistrationWindow(null));

        Event e = new Event();
        e.setRegistrationOpen(null);
        e.setRegistrationClose(null);
        assertFalse(EventValidationHelper.isWithinRegistrationWindow(e));
    }

    // ---------- hasCapacity ----------

    @Test
    public void hasCapacity_trueWhenNoLimit() {
        Event e = new Event();
        e.setMaxCapacity(null);

        assertTrue(EventValidationHelper.hasCapacity(e, 0));
    }

    @Test
    public void hasCapacity_trueWhenUnderLimit() {
        Event e = new Event();
        e.setMaxCapacity(5);

        assertTrue(EventValidationHelper.hasCapacity(e, 3));
    }

    @Test
    public void hasCapacity_falseWhenAtOrOverLimit() {
        Event e = new Event();
        e.setMaxCapacity(5);

        assertFalse(EventValidationHelper.hasCapacity(e, 5));
        assertFalse(EventValidationHelper.hasCapacity(e, 7));
    }

    @Test
    public void hasCapacity_trueWhenCapacityIsZeroOrNegative() { // 0 or negative means no capacity limit in the implementation
        Event e1 = new Event();
        e1.setMaxCapacity(0);
        Event e2 = new Event();
        e2.setMaxCapacity(-10);

        assertTrue(EventValidationHelper.hasCapacity(e1, 999));
        assertTrue(EventValidationHelper.hasCapacity(e2, 999));
    }

    @Test
    public void hasCapacity_falseWhenEventIsNull() {
        assertFalse(EventValidationHelper.hasCapacity(null, 0));
    }
}
