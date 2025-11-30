package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.FilterEventsController;
import ca.ualberta.codarc.codarc_events.models.Event;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class FilterEventsControllerTests {

    private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
    private final FilterEventsController controller = new FilterEventsController();

    // ---------- Tag Filtering Tests ----------

    @Test
    public void applyFilters_tagFilter_singleTagMatch() {
        Event event1 = createEventWithTags("event1", "sports");
        Event event2 = createEventWithTags("event2", "music");
        List<Event> events = new ArrayList<>();
        events.add(event1);
        events.add(event2);

        FilterEventsController.FilterCriteria criteria = new FilterEventsController.FilterCriteria(
                createTagList("sports"), false);
        Map<String, Integer> acceptedCounts = new HashMap<>();

        FilterEventsController.FilterResult result = controller.applyFilters(events, criteria, acceptedCounts);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getFilteredEvents().size());
        assertEquals("event1", result.getFilteredEvents().get(0).getId());
    }

    @Test
    public void applyFilters_tagFilter_multipleTagsOrLogic() {
        Event event1 = createEventWithTags("event1", "sports");
        Event event2 = createEventWithTags("event2", "music");
        Event event3 = createEventWithTags("event3", "academic");
        List<Event> events = new ArrayList<>();
        events.add(event1);
        events.add(event2);
        events.add(event3);

        FilterEventsController.FilterCriteria criteria = new FilterEventsController.FilterCriteria(
                createTagList("sports", "music"), false);
        Map<String, Integer> acceptedCounts = new HashMap<>();

        FilterEventsController.FilterResult result = controller.applyFilters(events, criteria, acceptedCounts);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getFilteredEvents().size());
    }

    @Test
    public void applyFilters_tagFilter_noMatchingTags() {
        Event event1 = createEventWithTags("event1", "sports");
        Event event2 = createEventWithTags("event2", "music");
        List<Event> events = new ArrayList<>();
        events.add(event1);
        events.add(event2);

        FilterEventsController.FilterCriteria criteria = new FilterEventsController.FilterCriteria(
                createTagList("academic"), false);
        Map<String, Integer> acceptedCounts = new HashMap<>();

        FilterEventsController.FilterResult result = controller.applyFilters(events, criteria, acceptedCounts);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getFilteredEvents().size());
    }

    @Test
    public void applyFilters_tagFilter_caseInsensitive() {
        Event event1 = createEventWithTags("event1", "Sports");
        Event event2 = createEventWithTags("event2", "SPORTS");
        List<Event> events = new ArrayList<>();
        events.add(event1);
        events.add(event2);

        FilterEventsController.FilterCriteria criteria = new FilterEventsController.FilterCriteria(
                createTagList("sports"), false);
        Map<String, Integer> acceptedCounts = new HashMap<>();

        FilterEventsController.FilterResult result = controller.applyFilters(events, criteria, acceptedCounts);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getFilteredEvents().size());
    }

    @Test
    public void applyFilters_tagFilter_eventsWithNoTags() {
        Event event1 = createEventWithTags("event1", "sports");
        Event event2 = createEvent("event2");
        event2.setTags(null);
        List<Event> events = new ArrayList<>();
        events.add(event1);
        events.add(event2);

        FilterEventsController.FilterCriteria criteria = new FilterEventsController.FilterCriteria(
                createTagList("sports"), false);
        Map<String, Integer> acceptedCounts = new HashMap<>();

        FilterEventsController.FilterResult result = controller.applyFilters(events, criteria, acceptedCounts);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getFilteredEvents().size());
        assertEquals("event1", result.getFilteredEvents().get(0).getId());
    }

    // ---------- Availability Filtering Tests ----------

    @Test
    public void applyFilters_availabilityFilter_openRegistrationAndCapacity() throws Exception {
        Event event = createEvent("event1");
        long now = System.currentTimeMillis();
        event.setRegistrationOpen(iso.format(new Date(now - 1000)));
        event.setRegistrationClose(iso.format(new Date(now + 10000)));
        event.setMaxCapacity(10);

        List<Event> events = new ArrayList<>();
        events.add(event);

        FilterEventsController.FilterCriteria criteria = new FilterEventsController.FilterCriteria(
                null, true);
        Map<String, Integer> acceptedCounts = new HashMap<>();
        acceptedCounts.put("event1", 5); // 5 accepted, capacity is 10, so available

        FilterEventsController.FilterResult result = controller.applyFilters(events, criteria, acceptedCounts);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getFilteredEvents().size());
    }

    @Test
    public void applyFilters_availabilityFilter_closedRegistration() throws Exception {
        Event event = createEvent("event1");
        long now = System.currentTimeMillis();
        event.setRegistrationOpen(iso.format(new Date(now - 10000)));
        event.setRegistrationClose(iso.format(new Date(now - 1000))); // Closed
        event.setMaxCapacity(10);

        List<Event> events = new ArrayList<>();
        events.add(event);

        FilterEventsController.FilterCriteria criteria = new FilterEventsController.FilterCriteria(
                null, true);
        Map<String, Integer> acceptedCounts = new HashMap<>();
        acceptedCounts.put("event1", 5);

        FilterEventsController.FilterResult result = controller.applyFilters(events, criteria, acceptedCounts);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getFilteredEvents().size());
    }

    @Test
    public void applyFilters_availabilityFilter_fullCapacity() throws Exception {
        Event event = createEvent("event1");
        long now = System.currentTimeMillis();
        event.setRegistrationOpen(iso.format(new Date(now - 1000)));
        event.setRegistrationClose(iso.format(new Date(now + 10000)));
        event.setMaxCapacity(5);

        List<Event> events = new ArrayList<>();
        events.add(event);

        FilterEventsController.FilterCriteria criteria = new FilterEventsController.FilterCriteria(
                null, true);
        Map<String, Integer> acceptedCounts = new HashMap<>();
        acceptedCounts.put("event1", 5); // 5 accepted, capacity is 5, so full

        FilterEventsController.FilterResult result = controller.applyFilters(events, criteria, acceptedCounts);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getFilteredEvents().size());
    }

    @Test
    public void applyFilters_availabilityFilter_noCapacityLimit() throws Exception {
        Event event = createEvent("event1");
        long now = System.currentTimeMillis();
        event.setRegistrationOpen(iso.format(new Date(now - 1000)));
        event.setRegistrationClose(iso.format(new Date(now + 10000)));
        event.setMaxCapacity(null); // No limit

        List<Event> events = new ArrayList<>();
        events.add(event);

        FilterEventsController.FilterCriteria criteria = new FilterEventsController.FilterCriteria(
                null, true);
        Map<String, Integer> acceptedCounts = new HashMap<>();
        acceptedCounts.put("event1", 100);

        FilterEventsController.FilterResult result = controller.applyFilters(events, criteria, acceptedCounts);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getFilteredEvents().size());
    }

    // ---------- Combined Filtering Tests ----------

    @Test
    public void applyFilters_combinedFilters_tagsAndAvailability() throws Exception {
        Event event1 = createEventWithTags("event1", "sports");
        long now = System.currentTimeMillis();
        event1.setRegistrationOpen(iso.format(new Date(now - 1000)));
        event1.setRegistrationClose(iso.format(new Date(now + 10000)));
        event1.setMaxCapacity(10);

        Event event2 = createEventWithTags("event2", "sports");
        event2.setRegistrationOpen(iso.format(new Date(now - 10000)));
        event2.setRegistrationClose(iso.format(new Date(now - 1000))); // Closed
        event2.setMaxCapacity(10);

        List<Event> events = new ArrayList<>();
        events.add(event1);
        events.add(event2);

        FilterEventsController.FilterCriteria criteria = new FilterEventsController.FilterCriteria(
                createTagList("sports"), true);
        Map<String, Integer> acceptedCounts = new HashMap<>();
        acceptedCounts.put("event1", 5);
        acceptedCounts.put("event2", 5);

        FilterEventsController.FilterResult result = controller.applyFilters(events, criteria, acceptedCounts);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getFilteredEvents().size());
        assertEquals("event1", result.getFilteredEvents().get(0).getId());
    }

    // ---------- Empty Filter Tests ----------

    @Test
    public void applyFilters_emptyFilters_returnsAllEvents() {
        Event event1 = createEvent("event1");
        Event event2 = createEvent("event2");
        List<Event> events = new ArrayList<>();
        events.add(event1);
        events.add(event2);

        FilterEventsController.FilterCriteria criteria = new FilterEventsController.FilterCriteria(
                null, false);
        Map<String, Integer> acceptedCounts = new HashMap<>();

        FilterEventsController.FilterResult result = controller.applyFilters(events, criteria, acceptedCounts);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getFilteredEvents().size());
    }

    @Test
    public void applyFilters_nullEventList_returnsFailure() {
        FilterEventsController.FilterCriteria criteria = new FilterEventsController.FilterCriteria(
                createTagList("sports"), false);
        Map<String, Integer> acceptedCounts = new HashMap<>();

        FilterEventsController.FilterResult result = controller.applyFilters(null, criteria, acceptedCounts);

        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    // ---------- Helper Methods ----------

    private Event createEvent(String id) {
        Event event = new Event();
        event.setId(id);
        event.setName("Test Event " + id);
        return event;
    }

    private Event createEventWithTags(String id, String... tags) {
        Event event = createEvent(id);
        List<String> tagList = new ArrayList<>();
        for (String tag : tags) {
            tagList.add(tag);
        }
        event.setTags(tagList);
        return event;
    }

    private List<String> createTagList(String... tags) {
        List<String> tagList = new ArrayList<>();
        for (String tag : tags) {
            tagList.add(tag);
        }
        return tagList;
    }
}

