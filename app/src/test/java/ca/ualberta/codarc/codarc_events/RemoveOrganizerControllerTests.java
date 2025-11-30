package ca.ualberta.codarc.codarc_events;

import android.util.Log;
import ca.ualberta.codarc.codarc_events.controllers.DeleteEventController;
import ca.ualberta.codarc.codarc_events.controllers.RemoveOrganizerController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.OrganizerDB;
import ca.ualberta.codarc.codarc_events.data.PosterStorage;
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
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RemoveOrganizerControllerTests {

    private OrganizerDB mockOrganizerDb;
    private EventDB mockEventDb;
    private EntrantDB mockEntrantDb;
    private DeleteEventController mockDeleteEventController;
    private PosterStorage mockPosterStorage;
    private UserDB mockUserDb;
    private RemoveOrganizerController controller;
    private MockedStatic<Log> logMock;

    private static final String ORGANIZER_ID = "org-123";
    private static final String ADMIN_ID = "admin-456";
    private static final String EVENT_ID = "event-789";

    @Before
    public void setUp() {
        logMock = Mockito.mockStatic(Log.class);
        mockOrganizerDb = mock(OrganizerDB.class);
        mockEventDb = mock(EventDB.class);
        mockEntrantDb = mock(EntrantDB.class);
        mockDeleteEventController = mock(DeleteEventController.class);
        mockPosterStorage = mock(PosterStorage.class);
        mockUserDb = mock(UserDB.class);
        controller = new RemoveOrganizerController(mockOrganizerDb, mockEventDb, mockEntrantDb,
                mockDeleteEventController, mockPosterStorage, mockUserDb);
    }

    @After
    public void tearDown() {
        if (logMock != null) {
            logMock.close();
        }
    }

    @Test
    public void banOrganizer_emptyOrganizerId_failsFast() {
        RemoveOrganizerController.Callback cb = mock(RemoveOrganizerController.Callback.class);

        controller.banOrganizer("", ADMIN_ID, cb);

        ArgumentCaptor<RemoveOrganizerController.RemoveOrganizerResult> resCap =
                ArgumentCaptor.forClass(RemoveOrganizerController.RemoveOrganizerResult.class);
        verify(cb).onResult(resCap.capture());

        RemoveOrganizerController.RemoveOrganizerResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("organizerId cannot be null or empty", res.getErrorMessage());

        verifyNoInteractions(mockUserDb, mockOrganizerDb, mockEventDb);
    }

    @Test
    public void banOrganizer_emptyAdminId_failsFast() {
        RemoveOrganizerController.Callback cb = mock(RemoveOrganizerController.Callback.class);

        controller.banOrganizer(ORGANIZER_ID, "", cb);

        ArgumentCaptor<RemoveOrganizerController.RemoveOrganizerResult> resCap =
                ArgumentCaptor.forClass(RemoveOrganizerController.RemoveOrganizerResult.class);
        verify(cb).onResult(resCap.capture());

        RemoveOrganizerController.RemoveOrganizerResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("adminDeviceId cannot be null or empty", res.getErrorMessage());

        verifyNoInteractions(mockUserDb, mockOrganizerDb, mockEventDb);
    }

    @Test
    public void banOrganizer_nonAdmin_returnsError() {
        RemoveOrganizerController.Callback cb = mock(RemoveOrganizerController.Callback.class);

        controller.banOrganizer(ORGANIZER_ID, ADMIN_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq(ADMIN_ID), userCap.capture());

        User nonAdmin = new User();
        nonAdmin.setAdmin(false);
        userCap.getValue().onSuccess(nonAdmin);

        ArgumentCaptor<RemoveOrganizerController.RemoveOrganizerResult> resCap =
                ArgumentCaptor.forClass(RemoveOrganizerController.RemoveOrganizerResult.class);
        verify(cb).onResult(resCap.capture());

        RemoveOrganizerController.RemoveOrganizerResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Admin access required", res.getErrorMessage());

        verify(mockOrganizerDb, never()).getOrganizerEvents(anyString(), any());
    }

    @Test
    public void banOrganizer_withEvents_initiatesEventDeletion() {
        RemoveOrganizerController.Callback cb = mock(RemoveOrganizerController.Callback.class);

        controller.banOrganizer(ORGANIZER_ID, ADMIN_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq(ADMIN_ID), userCap.capture());

        User admin = new User();
        admin.setAdmin(true);
        userCap.getValue().onSuccess(admin);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<OrganizerDB.Callback<List<String>>> eventsCap =
                ArgumentCaptor.forClass(OrganizerDB.Callback.class);
        verify(mockOrganizerDb).getOrganizerEvents(eq(ORGANIZER_ID), eventsCap.capture());

        List<String> eventIds = Arrays.asList(EVENT_ID);
        eventsCap.getValue().onSuccess(eventIds);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq(EVENT_ID), eventCap.capture());

        Event event = new Event();
        event.setId(EVENT_ID);
        event.setOrganizerId(ORGANIZER_ID);
        eventCap.getValue().onSuccess(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<PosterStorage.Callback<Void>> posterCap =
                ArgumentCaptor.forClass(PosterStorage.Callback.class);
        verify(mockPosterStorage).deletePoster(eq(EVENT_ID), posterCap.capture());
        posterCap.getValue().onSuccess(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<DeleteEventController.Callback> deleteCap =
                ArgumentCaptor.forClass(DeleteEventController.Callback.class);
        verify(mockDeleteEventController).deleteEvent(eq(EVENT_ID), eq(ADMIN_ID), deleteCap.capture());
        deleteCap.getValue().onResult(DeleteEventController.DeleteEventResult.success());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<OrganizerDB.Callback<Void>> removeCap =
                ArgumentCaptor.forClass(OrganizerDB.Callback.class);
        verify(mockOrganizerDb).removeEventFromOrganizer(eq(ORGANIZER_ID), eq(EVENT_ID), removeCap.capture());
        removeCap.getValue().onSuccess(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Void>> entrantCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).removeEventFromAllEntrants(eq(EVENT_ID), entrantCap.capture());
        entrantCap.getValue().onSuccess(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<OrganizerDB.Callback<Void>> banCap =
                ArgumentCaptor.forClass(OrganizerDB.Callback.class);
        verify(mockOrganizerDb).setBannedStatus(eq(ORGANIZER_ID), eq(true), banCap.capture());
        banCap.getValue().onSuccess(null);

        ArgumentCaptor<RemoveOrganizerController.RemoveOrganizerResult> resCap =
                ArgumentCaptor.forClass(RemoveOrganizerController.RemoveOrganizerResult.class);
        verify(cb).onResult(resCap.capture());
        assertTrue(resCap.getValue().isSuccess());
    }

    @Test
    public void banOrganizer_noEvents_stillBansOrganizer() {
        RemoveOrganizerController.Callback cb = mock(RemoveOrganizerController.Callback.class);

        controller.banOrganizer(ORGANIZER_ID, ADMIN_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq(ADMIN_ID), userCap.capture());

        User admin = new User();
        admin.setAdmin(true);
        userCap.getValue().onSuccess(admin);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<OrganizerDB.Callback<List<String>>> eventsCap =
                ArgumentCaptor.forClass(OrganizerDB.Callback.class);
        verify(mockOrganizerDb).getOrganizerEvents(eq(ORGANIZER_ID), eventsCap.capture());

        eventsCap.getValue().onSuccess(Collections.emptyList());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<OrganizerDB.Callback<Void>> banCap =
                ArgumentCaptor.forClass(OrganizerDB.Callback.class);
        verify(mockOrganizerDb).setBannedStatus(eq(ORGANIZER_ID), eq(true), banCap.capture());
        
        banCap.getValue().onSuccess(null);

        ArgumentCaptor<RemoveOrganizerController.RemoveOrganizerResult> resCap =
                ArgumentCaptor.forClass(RemoveOrganizerController.RemoveOrganizerResult.class);
        verify(cb).onResult(resCap.capture());
        assertTrue(resCap.getValue().isSuccess());
    }
}
