package ca.ualberta.codarc.codarc_events;

import org.junit.Test;
import static org.junit.Assert.*;

import ca.ualberta.codarc.codarc_events.models.Event;

public class EventTest {

    @Test
    public void eventInitialization_isCorrect() {
        Event event = new Event("E1", "Sample Event", "2025-11-10", true);

        assertEquals("E1", event.getId());
        assertEquals("Sample Event", event.getName());
        assertEquals("2025-11-10", event.getDate());
        assertTrue(event.getIsOpen());
    }
}

