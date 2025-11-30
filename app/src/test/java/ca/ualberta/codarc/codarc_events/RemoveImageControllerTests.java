package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.RemoveImageController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.PosterStorage;
import ca.ualberta.codarc.codarc_events.data.UserDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.User;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RemoveImageControllerTests {

    private EventDB mockEventDb;
    private PosterStorage mockPosterStorage;
    private UserDB mockUserDb;
    private RemoveImageController controller;

    @Before
    public void setUp() {
        mockEventDb = mock(EventDB.class);
        mockPosterStorage = mock(PosterStorage.class);
        mockUserDb = mock(UserDB.class);
        controller = new RemoveImageController(mockEventDb, mockPosterStorage, mockUserDb);
    }

    @Test
    public void removeImage_emptyEventId_failsFast() {
        RemoveImageController.Callback cb = mock(RemoveImageController.Callback.class);

        controller.removeImage("", "admin1", cb);

        ArgumentCaptor<RemoveImageController.RemoveImageResult> resCap =
                ArgumentCaptor.forClass(RemoveImageController.RemoveImageResult.class);
        verify(cb).onResult(resCap.capture());

        RemoveImageController.RemoveImageResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("eventId cannot be null or empty", res.getErrorMessage());

        verifyNoInteractions(mockUserDb, mockEventDb, mockPosterStorage);
    }

    @Test
    public void removeImage_emptyAdminDeviceId_failsFast() {
        RemoveImageController.Callback cb = mock(RemoveImageController.Callback.class);

        controller.removeImage("E1", "", cb);

        ArgumentCaptor<RemoveImageController.RemoveImageResult> resCap =
                ArgumentCaptor.forClass(RemoveImageController.RemoveImageResult.class);
        verify(cb).onResult(resCap.capture());

        RemoveImageController.RemoveImageResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("adminDeviceId cannot be null or empty", res.getErrorMessage());

        verifyNoInteractions(mockUserDb, mockEventDb, mockPosterStorage);
    }

    @Test
    public void removeImage_nonAdmin_returnsAdminRequiredError() {
        RemoveImageController.Callback cb = mock(RemoveImageController.Callback.class);

        controller.removeImage("E1", "dev1", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq("dev1"), userCap.capture());

        User mockUser = mock(User.class);
        when(mockUser.isAdmin()).thenReturn(false);
        userCap.getValue().onSuccess(mockUser);

        ArgumentCaptor<RemoveImageController.RemoveImageResult> resCap =
                ArgumentCaptor.forClass(RemoveImageController.RemoveImageResult.class);
        verify(cb).onResult(resCap.capture());

        RemoveImageController.RemoveImageResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Admin access required", res.getErrorMessage());

        verifyNoInteractions(mockEventDb, mockPosterStorage);
    }

    @Test
    public void removeImage_eventNotFound_returnsFailure() {
        RemoveImageController.Callback cb = mock(RemoveImageController.Callback.class);

        controller.removeImage("E1", "admin1", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq("admin1"), userCap.capture());

        User mockUser = mock(User.class);
        when(mockUser.isAdmin()).thenReturn(true);
        userCap.getValue().onSuccess(mockUser);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq("E1"), eventCap.capture());

        eventCap.getValue().onSuccess(null);

        ArgumentCaptor<RemoveImageController.RemoveImageResult> resCap =
                ArgumentCaptor.forClass(RemoveImageController.RemoveImageResult.class);
        verify(cb).onResult(resCap.capture());

        RemoveImageController.RemoveImageResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Event not found", res.getErrorMessage());

        verifyNoInteractions(mockPosterStorage);
        verify(mockEventDb, never()).addEvent(any(Event.class), any());
    }

    @Test
    public void removeImage_eventHasNoPoster_returnsFailure() {
        RemoveImageController.Callback cb = mock(RemoveImageController.Callback.class);

        controller.removeImage("E1", "admin1", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq("admin1"), userCap.capture());

        User mockUser = mock(User.class);
        when(mockUser.isAdmin()).thenReturn(true);
        userCap.getValue().onSuccess(mockUser);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq("E1"), eventCap.capture());

        Event event = new Event();
        event.setId("E1");
        event.setPosterUrl(null);
        eventCap.getValue().onSuccess(event);

        ArgumentCaptor<RemoveImageController.RemoveImageResult> resCap =
                ArgumentCaptor.forClass(RemoveImageController.RemoveImageResult.class);
        verify(cb).onResult(resCap.capture());

        RemoveImageController.RemoveImageResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("This event has no image to remove", res.getErrorMessage());

        verifyNoInteractions(mockPosterStorage);
        verify(mockEventDb, never()).addEvent(any(Event.class), any());
    }

    @Test
    public void removeImage_adminAndPoster_deletesPosterAndUpdatesEvent() {
        RemoveImageController.Callback cb = mock(RemoveImageController.Callback.class);

        controller.removeImage("E1", "admin1", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq("admin1"), userCap.capture());

        User mockUser = mock(User.class);
        when(mockUser.isAdmin()).thenReturn(true);
        userCap.getValue().onSuccess(mockUser);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Event>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEvent(eq("E1"), eventCap.capture());

        Event event = new Event();
        event.setId("E1");
        event.setPosterUrl("https://example.com/poster.png");
        eventCap.getValue().onSuccess(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<PosterStorage.Callback<Void>> deleteCap =
                ArgumentCaptor.forClass(PosterStorage.Callback.class);
        verify(mockPosterStorage).deletePoster(eq("E1"), deleteCap.capture());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Event> savedEventCap =
                ArgumentCaptor.forClass(Event.class);
        ArgumentCaptor<EventDB.Callback<Void>> updateCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);

        verify(mockEventDb).addEvent(savedEventCap.capture(), updateCap.capture());

        Event updated = savedEventCap.getValue();
        assertEquals("E1", updated.getId());
        assertNull(updated.getPosterUrl());

        verifyNoInteractions(cb);
    }
}
