package ca.ualberta.codarc.codarc_events;

import android.net.Uri;

import ca.ualberta.codarc.codarc_events.controllers.UpdatePosterController;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.PosterStorage;
import ca.ualberta.codarc.codarc_events.models.Event;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class UpdatePosterControllerTests {

    private EventDB mockEventDb;
    private PosterStorage mockPosterStorage;
    private UpdatePosterController controller;

    @Before
    public void setUp() {
        mockEventDb = mock(EventDB.class);
        mockPosterStorage = mock(PosterStorage.class);
        controller = new UpdatePosterController(mockEventDb, mockPosterStorage);
    }

    // ---------- validation ----------

    @Test
    public void updatePoster_nullEvent_failsFast() {
        UpdatePosterController.Callback cb = mock(UpdatePosterController.Callback.class);
        Uri mockUri = mock(Uri.class);

        controller.updatePoster(null, mockUri, cb);

        ArgumentCaptor<UpdatePosterController.UpdatePosterResult> resCap =
                ArgumentCaptor.forClass(UpdatePosterController.UpdatePosterResult.class);
        verify(cb).onResult(resCap.capture());

        UpdatePosterController.UpdatePosterResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Event is invalid", res.getErrorMessage());
        assertNull(res.getUpdatedEvent());

        verifyNoInteractions(mockPosterStorage, mockEventDb);
    }

    @Test
    public void updatePoster_eventWithNullId_failsFast() {
        UpdatePosterController.Callback cb = mock(UpdatePosterController.Callback.class);
        Uri mockUri = mock(Uri.class);

        Event event = new Event(); // id is null
        controller.updatePoster(event, mockUri, cb);

        ArgumentCaptor<UpdatePosterController.UpdatePosterResult> resCap =
                ArgumentCaptor.forClass(UpdatePosterController.UpdatePosterResult.class);
        verify(cb).onResult(resCap.capture());

        UpdatePosterController.UpdatePosterResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Event is invalid", res.getErrorMessage());
        assertNull(res.getUpdatedEvent());

        verifyNoInteractions(mockPosterStorage, mockEventDb);
    }

    @Test
    public void updatePoster_nullImageUri_failsFast() {
        UpdatePosterController.Callback cb = mock(UpdatePosterController.Callback.class);

        Event event = new Event();
        event.setId("E1");

        controller.updatePoster(event, null, cb);

        ArgumentCaptor<UpdatePosterController.UpdatePosterResult> resCap =
                ArgumentCaptor.forClass(UpdatePosterController.UpdatePosterResult.class);
        verify(cb).onResult(resCap.capture());

        UpdatePosterController.UpdatePosterResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Image URI cannot be null", res.getErrorMessage());
        assertNull(res.getUpdatedEvent());

        verifyNoInteractions(mockPosterStorage, mockEventDb);
    }

    // ---------- upload failure ----------

    @Test
    public void updatePoster_uploadFails_returnsFailureAndDoesNotTouchEventDb() {
        UpdatePosterController.Callback cb = mock(UpdatePosterController.Callback.class);

        Event event = new Event();
        event.setId("E1");
        Uri mockUri = mock(Uri.class);

        controller.updatePoster(event, mockUri, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<PosterStorage.Callback<String>> uploadCap =
                ArgumentCaptor.forClass(PosterStorage.Callback.class);
        verify(mockPosterStorage).uploadPoster(eq("E1"), eq(mockUri), uploadCap.capture());

        // Simulate upload error with a specific message
        uploadCap.getValue().onError(new Exception("upload exploded"));

        ArgumentCaptor<UpdatePosterController.UpdatePosterResult> resCap =
                ArgumentCaptor.forClass(UpdatePosterController.UpdatePosterResult.class);
        verify(cb).onResult(resCap.capture());

        UpdatePosterController.UpdatePosterResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        // Controller uses the exception message if non empty
        assertEquals("upload exploded", res.getErrorMessage());
        assertNull(res.getUpdatedEvent());

        verifyNoInteractions(mockEventDb);
    }

    // ---------- event update failure after successful upload ----------

    @Test
    public void updatePoster_eventUpdateFails_returnsFailure() {
        UpdatePosterController.Callback cb = mock(UpdatePosterController.Callback.class);

        Event event = new Event();
        event.setId("E1");
        Uri mockUri = mock(Uri.class);

        controller.updatePoster(event, mockUri, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<PosterStorage.Callback<String>> uploadCap =
                ArgumentCaptor.forClass(PosterStorage.Callback.class);
        verify(mockPosterStorage).uploadPoster(eq("E1"), eq(mockUri), uploadCap.capture());

        // Simulate successful upload
        uploadCap.getValue().onSuccess("https://example.com/poster.jpg");
        assertEquals("https://example.com/poster.jpg", event.getPosterUrl());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Void>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).addEvent(eq(event), eventCap.capture());

        // Simulate Firestore failure
        eventCap.getValue().onError(new Exception("firestore RIP"));

        ArgumentCaptor<UpdatePosterController.UpdatePosterResult> resCap =
                ArgumentCaptor.forClass(UpdatePosterController.UpdatePosterResult.class);
        verify(cb).onResult(resCap.capture());

        UpdatePosterController.UpdatePosterResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Failed to update event: firestore RIP", res.getErrorMessage());
        assertNull(res.getUpdatedEvent());
    }

    // ---------- happy path ----------

    @Test
    public void updatePoster_success_updatesPosterUrlAndReturnsUpdatedEvent() {
        UpdatePosterController.Callback cb = mock(UpdatePosterController.Callback.class);

        Event event = new Event();
        event.setId("E1");
        Uri mockUri = mock(Uri.class);

        controller.updatePoster(event, mockUri, cb);

        // Step 1: capture upload callback
        @SuppressWarnings("unchecked")
        ArgumentCaptor<PosterStorage.Callback<String>> uploadCap =
                ArgumentCaptor.forClass(PosterStorage.Callback.class);
        verify(mockPosterStorage).uploadPoster(eq("E1"), eq(mockUri), uploadCap.capture());

        // Simulate upload success
        String newUrl = "https://example.com/posters/E1.jpg";
        uploadCap.getValue().onSuccess(newUrl);

        // Event should now carry the new URL before hitting EventDB
        assertEquals(newUrl, event.getPosterUrl());

        // Step 2: capture event update callback
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Void>> eventCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).addEvent(eq(event), eventCap.capture());

        // Simulate event update success
        eventCap.getValue().onSuccess(null);

        // Step 3: verify final result
        ArgumentCaptor<UpdatePosterController.UpdatePosterResult> resCap =
                ArgumentCaptor.forClass(UpdatePosterController.UpdatePosterResult.class);
        verify(cb).onResult(resCap.capture());

        UpdatePosterController.UpdatePosterResult res = resCap.getValue();
        assertTrue(res.isSuccess());
        assertNull(res.getErrorMessage());
        assertNotNull(res.getUpdatedEvent());
        assertSame(event, res.getUpdatedEvent());
        assertEquals(newUrl, res.getUpdatedEvent().getPosterUrl());
    }
}
