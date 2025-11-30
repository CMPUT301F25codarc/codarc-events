package ca.ualberta.codarc.codarc_events;

import android.util.Log;
import ca.ualberta.codarc.codarc_events.controllers.DeleteEventController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.OrganizerDB;
import ca.ualberta.codarc.codarc_events.data.PosterStorage;
import ca.ualberta.codarc.codarc_events.data.TagDB;
import ca.ualberta.codarc.codarc_events.data.UserDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DeleteEventControllerTests {

    private EventDB mockEventDb;
    private PosterStorage mockPosterStorage;
    private OrganizerDB mockOrganizerDb;
    private TagDB mockTagDb;
    private UserDB mockUserDb;
    private DeleteEventController controller;
    private MockedStatic<Log> logMock;

    private static final String EVENT_ID = "event-123";
    private static final String ADMIN_ID = "admin-456";
    private static final String ORGANIZER_ID = "org-789";

    @Before
    public void setUp() {
        logMock = Mockito.mockStatic(Log.class);
        mockEventDb = mock(EventDB.class);
        mockPosterStorage = mock(PosterStorage.class);
        mockOrganizerDb = mock(OrganizerDB.class);
        mockTagDb = mock(TagDB.class);
        mockUserDb = mock(UserDB.class);
        controller = new DeleteEventController(mockEventDb, mockPosterStorage,
                mockOrganizerDb, mockTagDb, mockUserDb);
    }

    @After
    public void tearDown() {
        if (logMock != null) {
            logMock.close();
        }
    }

    @Test
    public void deleteEvent_emptyEventId_failsFast() {
        DeleteEventController.Callback cb = mock(DeleteEventController.Callback.class);

        controller.deleteEvent("", ADMIN_ID, cb);

        ArgumentCaptor<DeleteEventController.DeleteEventResult> resCap =
                ArgumentCaptor.forClass(DeleteEventController.DeleteEventResult.class);
        verify(cb).onResult(resCap.capture());

        DeleteEventController.DeleteEventResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("eventId cannot be null or empty", res.getErrorMessage());

        verifyNoInteractions(mockUserDb, mockEventDb, mockPosterStorage, mockOrganizerDb, mockTagDb);
    }

    @Test
    public void deleteEvent_emptyAdminId_failsFast() {
        DeleteEventController.Callback cb = mock(DeleteEventController.Callback.class);

        controller.deleteEvent(EVENT_ID, "", cb);

        ArgumentCaptor<DeleteEventController.DeleteEventResult> resCap =
                ArgumentCaptor.forClass(DeleteEventController.DeleteEventResult.class);
        verify(cb).onResult(resCap.capture());

        DeleteEventController.DeleteEventResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("adminDeviceId cannot be null or empty", res.getErrorMessage());

        verifyNoInteractions(mockUserDb, mockEventDb, mockPosterStorage, mockOrganizerDb, mockTagDb);
    }

    @Test
    public void deleteEvent_nonAdmin_returnsError() {
        DeleteEventController.Callback cb = mock(DeleteEventController.Callback.class);

        controller.deleteEvent(EVENT_ID, ADMIN_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq(ADMIN_ID), userCap.capture());

        User nonAdmin = new User();
        nonAdmin.setAdmin(false);
        userCap.getValue().onSuccess(nonAdmin);

        ArgumentCaptor<DeleteEventController.DeleteEventResult> resCap =
                ArgumentCaptor.forClass(DeleteEventController.DeleteEventResult.class);
        verify(cb).onResult(resCap.capture());

        DeleteEventController.DeleteEventResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Admin access required", res.getErrorMessage());

        verify(mockEventDb, never()).getEvent(anyString(), any());
    }

    @Test
    public void deleteEvent_adminNotFound_returnsError() {
        DeleteEventController.Callback cb = mock(DeleteEventController.Callback.class);

        controller.deleteEvent(EVENT_ID, ADMIN_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq(ADMIN_ID), userCap.capture());

        userCap.getValue().onError(new RuntimeException("User not found"));

        ArgumentCaptor<DeleteEventController.DeleteEventResult> resCap =
                ArgumentCaptor.forClass(DeleteEventController.DeleteEventResult.class);
        verify(cb).onResult(resCap.capture());

        DeleteEventController.DeleteEventResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Failed to verify admin status", res.getErrorMessage());
    }

    @Test
    public void deleteEvent_eventNotFound_returnsError() {
        DeleteEventController.Callback cb = mock(DeleteEventController.Callback.class);

        controller.deleteEvent(EVENT_ID, ADMIN_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq(ADMIN_ID), userCap.capture());

        User admin = new User();
        admin.setAdmin(true);
        userCap.getValue().onSuccess(admin);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq(EVENT_ID), eventCap.capture());

        eventCap.getValue().onError(new RuntimeException("Event not found"));

        ArgumentCaptor<DeleteEventController.DeleteEventResult> resCap =
                ArgumentCaptor.forClass(DeleteEventController.DeleteEventResult.class);
        verify(cb).onResult(resCap.capture());

        DeleteEventController.DeleteEventResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Event not found", res.getErrorMessage());
    }

    @Test
    public void deleteEvent_success_deletesEventAndPerformsCleanup() {
        DeleteEventController.Callback cb = mock(DeleteEventController.Callback.class);

        controller.deleteEvent(EVENT_ID, ADMIN_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq(ADMIN_ID), userCap.capture());

        User admin = new User();
        admin.setAdmin(true);
        userCap.getValue().onSuccess(admin);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq(EVENT_ID), eventCap.capture());

        Event event = new Event();
        event.setId(EVENT_ID);
        event.setOrganizerId(ORGANIZER_ID);
        event.setTags(Arrays.asList("music", "outdoor"));
        eventCap.getValue().onSuccess(event);

        verify(mockPosterStorage).deletePoster(eq(EVENT_ID), any());
        verify(mockOrganizerDb).removeEventFromOrganizer(eq(ORGANIZER_ID), eq(EVENT_ID), any());
        verify(mockTagDb).removeTags(eq(Arrays.asList("music", "outdoor")), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Void>> deleteCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).deleteEvent(eq(EVENT_ID), deleteCap.capture());
        
        deleteCap.getValue().onSuccess(null);

        ArgumentCaptor<DeleteEventController.DeleteEventResult> resCap =
                ArgumentCaptor.forClass(DeleteEventController.DeleteEventResult.class);
        verify(cb).onResult(resCap.capture());

        DeleteEventController.DeleteEventResult res = resCap.getValue();
        assertTrue(res.isSuccess());
        assertNull(res.getErrorMessage());
    }

    @Test
    public void deleteEvent_deleteEventFailure_returnsError() {
        DeleteEventController.Callback cb = mock(DeleteEventController.Callback.class);

        controller.deleteEvent(EVENT_ID, ADMIN_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq(ADMIN_ID), userCap.capture());

        User admin = new User();
        admin.setAdmin(true);
        userCap.getValue().onSuccess(admin);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq(EVENT_ID), eventCap.capture());

        Event event = new Event();
        event.setId(EVENT_ID);
        eventCap.getValue().onSuccess(event);

        verify(mockPosterStorage).deletePoster(eq(EVENT_ID), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Void>> deleteCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).deleteEvent(eq(EVENT_ID), deleteCap.capture());
        deleteCap.getValue().onError(new RuntimeException("Delete failed"));

        ArgumentCaptor<DeleteEventController.DeleteEventResult> resCap =
                ArgumentCaptor.forClass(DeleteEventController.DeleteEventResult.class);
        verify(cb).onResult(resCap.capture());

        DeleteEventController.DeleteEventResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Failed to delete event. Please try again.", res.getErrorMessage());
    }

    @Test
    public void deleteEvent_noOrganizer_skipsOrganizerCleanup() {
        DeleteEventController.Callback cb = mock(DeleteEventController.Callback.class);

        controller.deleteEvent(EVENT_ID, ADMIN_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq(ADMIN_ID), userCap.capture());

        User admin = new User();
        admin.setAdmin(true);
        userCap.getValue().onSuccess(admin);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq(EVENT_ID), eventCap.capture());

        Event event = new Event();
        event.setId(EVENT_ID);
        event.setOrganizerId(null);
        eventCap.getValue().onSuccess(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Void>> deleteCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).deleteEvent(eq(EVENT_ID), deleteCap.capture());
        deleteCap.getValue().onSuccess(null);

        verify(mockPosterStorage).deletePoster(eq(EVENT_ID), any());
        verify(mockOrganizerDb, never()).removeEventFromOrganizer(anyString(), anyString(), any());

        ArgumentCaptor<DeleteEventController.DeleteEventResult> resCap =
                ArgumentCaptor.forClass(DeleteEventController.DeleteEventResult.class);
        verify(cb).onResult(resCap.capture());
        assertTrue(resCap.getValue().isSuccess());
    }

    @Test
    public void deleteEvent_noTags_skipsTagCleanup() {
        DeleteEventController.Callback cb = mock(DeleteEventController.Callback.class);

        controller.deleteEvent(EVENT_ID, ADMIN_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq(ADMIN_ID), userCap.capture());

        User admin = new User();
        admin.setAdmin(true);
        userCap.getValue().onSuccess(admin);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq(EVENT_ID), eventCap.capture());

        Event event = new Event();
        event.setId(EVENT_ID);
        event.setTags(null);
        eventCap.getValue().onSuccess(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Void>> deleteCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).deleteEvent(eq(EVENT_ID), deleteCap.capture());
        deleteCap.getValue().onSuccess(null);

        verify(mockPosterStorage).deletePoster(eq(EVENT_ID), any());
        verify(mockTagDb, never()).removeTags(anyList(), any());

        ArgumentCaptor<DeleteEventController.DeleteEventResult> resCap =
                ArgumentCaptor.forClass(DeleteEventController.DeleteEventResult.class);
        verify(cb).onResult(resCap.capture());
        assertTrue(resCap.getValue().isSuccess());
    }
}
