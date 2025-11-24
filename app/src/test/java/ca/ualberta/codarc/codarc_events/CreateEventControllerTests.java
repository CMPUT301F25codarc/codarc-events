package ca.ualberta.codarc.codarc_events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ca.ualberta.codarc.codarc_events.controllers.CreateEventController;
import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Unit tests for {@link CreateEventController}.
 */
public class CreateEventControllerTests {

    private CreateEventController makeController() {
        // eventDB is not used by validateAndCreateEvent or canAddTag,
        // so we can safely pass null here.
        return new CreateEventController(null, "ORG123");
    }

    @Test
    public void validateAndCreateEvent_success_populatesEventCorrectly() {
        CreateEventController controller = makeController();

        String name = "Test Event";
        String description = "  A cool test event  ";
        String dateTime = "2025-12-01 07:00 PM";
        String location = "Student Union Building";
        String regOpen = "2025-11-20 08:00 AM";
        String regClose = "2025-11-30 10:00 PM";
        String capacityStr = "100";
        List<String> tags = Arrays.asList("fun", "coding");
        String posterUrl = "https://example.com/poster.png";

        CreateEventController.CreateEventResult result =
                controller.validateAndCreateEvent(
                        name,
                        description,
                        dateTime,
                        location,
                        regOpen,
                        regClose,
                        capacityStr,
                        tags,
                        posterUrl
                );

        assertTrue(result.isValid());
        assertEquals(null, result.getErrorMessage());

        Event event = result.getEvent();
        assertNotNull(event);

        // Basic field checks
        assertEquals("Test Event", event.getName());  // trimmed
        assertEquals("A cool test event", event.getDescription()); // trimmed
        assertEquals(dateTime, event.getEventDateTime());
        assertEquals(location, event.getLocation());
        assertEquals(regOpen, event.getRegistrationOpen());
        assertEquals(regClose, event.getRegistrationClose());
        assertEquals("ORG123", event.getOrganizerId());
        assertTrue(event.isOpen());

        // Capacity parsing
        assertEquals(Integer.valueOf(100), event.getMaxCapacity());

        // Tags + poster
        assertEquals(tags, event.getTags());
        assertEquals(posterUrl, event.getPosterUrl());

        // QR code format
        assertNotNull(event.getQrCode());
        assertTrue(event.getQrCode().startsWith("event:"));
    }

    @Test
    public void validateAndCreateEvent_missingName_returnsFailure() {
        CreateEventController controller = makeController();

        CreateEventController.CreateEventResult result =
                controller.validateAndCreateEvent(
                        "   ",                         // name (blank)
                        "desc",
                        "2025-12-01 07:00 PM",
                        "Campus",
                        "2025-11-20 08:00 AM",
                        "2025-11-30 10:00 PM",
                        "50",
                        Collections.emptyList(),
                        null
                );

        assertFalse(result.isValid());
        assertEquals("Event name is required", result.getErrorMessage());
    }

    @Test
    public void validateAndCreateEvent_missingDateTime_returnsFailure() {
        CreateEventController controller = makeController();

        CreateEventController.CreateEventResult result =
                controller.validateAndCreateEvent(
                        "Event Name",
                        "desc",
                        "   ",                         // dateTime blank
                        "Campus",
                        "2025-11-20 08:00 AM",
                        "2025-11-30 10:00 PM",
                        "50",
                        Collections.emptyList(),
                        null
                );

        assertFalse(result.isValid());
        assertEquals("Event date/time is required", result.getErrorMessage());
    }

    @Test
    public void validateAndCreateEvent_invalidCapacity_setsMaxCapacityNull() {
        CreateEventController controller = makeController();

        CreateEventController.CreateEventResult result =
                controller.validateAndCreateEvent(
                        "Event Name",
                        "desc",
                        "2025-12-01 07:00 PM",
                        "Campus",
                        "2025-11-20 08:00 AM",
                        "2025-11-30 10:00 PM",
                        "not-a-number",                // invalid capacity
                        Collections.emptyList(),
                        null
                );

        assertTrue(result.isValid());
        Event event = result.getEvent();
        assertNotNull(event);
        assertEquals(null, event.getMaxCapacity());
    }

    @Test
    public void validateAndCreateEvent_negativeCapacity_setsMaxCapacityNull() {
        CreateEventController controller = makeController();

        CreateEventController.CreateEventResult result =
                controller.validateAndCreateEvent(
                        "Event Name",
                        "desc",
                        "2025-12-01 07:00 PM",
                        "Campus",
                        "2025-11-20 08:00 AM",
                        "2025-11-30 10:00 PM",
                        "-5",                         // negative capacity
                        Collections.emptyList(),
                        null
                );

        assertTrue(result.isValid());
        Event event = result.getEvent();
        assertNotNull(event);
        assertEquals(null, event.getMaxCapacity());
    }

    @Test
    public void validateAndCreateEvent_emptyCapacity_setsMaxCapacityNull() {
        CreateEventController controller = makeController();

        CreateEventController.CreateEventResult result =
                controller.validateAndCreateEvent(
                        "Event Name",
                        "desc",
                        "2025-12-01 07:00 PM",
                        "Campus",
                        "2025-11-20 08:00 AM",
                        "2025-11-30 10:00 PM",
                        "   ",                        // empty capacity
                        Collections.emptyList(),
                        null
                );

        assertTrue(result.isValid());
        Event event = result.getEvent();
        assertNotNull(event);
        assertEquals(null, event.getMaxCapacity());
    }

    @Test
    public void canAddTag_allowsNewTagWhenListEmpty() {
        CreateEventController controller = makeController();

        boolean canAdd = controller.canAddTag("Music", Collections.emptyList());

        assertTrue(canAdd);
    }

    @Test
    public void canAddTag_rejectsDuplicateTagCaseInsensitive() {
        CreateEventController controller = makeController();
        List<String> existing = Arrays.asList("music", "Sports");

        boolean canAdd1 = controller.canAddTag("MUSIC", existing);
        boolean canAdd2 = controller.canAddTag("sports", existing);

        assertFalse(canAdd1);
        assertFalse(canAdd2);
    }

    @Test
    public void canAddTag_rejectsNullOrEmpty() {
        CreateEventController controller = makeController();

        assertFalse(controller.canAddTag(null, Collections.emptyList()));
        assertFalse(controller.canAddTag("   ", Collections.emptyList()));
    }
}
