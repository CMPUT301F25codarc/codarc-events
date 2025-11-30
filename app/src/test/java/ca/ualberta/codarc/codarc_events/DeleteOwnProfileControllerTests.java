package ca.ualberta.codarc.codarc_events;

import android.util.Log;
import ca.ualberta.codarc.codarc_events.controllers.DeleteOwnProfileController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
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

public class DeleteOwnProfileControllerTests {

    private EntrantDB mockEntrantDb;
    private EventDB mockEventDb;
    private DeleteOwnProfileController controller;
    private MockedStatic<Log> logMock;

    private static final String DEVICE_ID = "dev-123";
    private static final String EVENT_ID = "event-456";

    @Before
    public void setUp() {
        logMock = Mockito.mockStatic(Log.class);
        mockEntrantDb = mock(EntrantDB.class);
        mockEventDb = mock(EventDB.class);
        controller = new DeleteOwnProfileController(mockEntrantDb, mockEventDb);
    }

    @After
    public void tearDown() {
        if (logMock != null) {
            logMock.close();
        }
    }

    @Test
    public void deleteOwnProfile_emptyDeviceId_failsFast() {
        DeleteOwnProfileController.Callback cb = mock(DeleteOwnProfileController.Callback.class);

        controller.deleteOwnProfile("", cb);

        ArgumentCaptor<DeleteOwnProfileController.DeleteProfileResult> resCap =
                ArgumentCaptor.forClass(DeleteOwnProfileController.DeleteProfileResult.class);
        verify(cb).onResult(resCap.capture());

        DeleteOwnProfileController.DeleteProfileResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Device ID is required", res.getErrorMessage());

        verifyNoInteractions(mockEntrantDb, mockEventDb);
    }

    @Test
    public void deleteOwnProfile_success_removesFromEventsAndClearsProfile() {
        DeleteOwnProfileController.Callback cb = mock(DeleteOwnProfileController.Callback.class);

        controller.deleteOwnProfile(DEVICE_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<String>>> eventsCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getEntrantEvents(eq(DEVICE_ID), eventsCap.capture());

        List<String> eventIds = Arrays.asList(EVENT_ID, "event-789");
        eventsCap.getValue().onSuccess(eventIds);

        verify(mockEventDb, times(2)).removeEntrantFromEvent(anyString(), eq(DEVICE_ID), any());
        verify(mockEntrantDb).deleteAllEntrantEvents(eq(DEVICE_ID), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Void>> deleteCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).deleteProfile(eq(DEVICE_ID), eq(false), deleteCap.capture());
        
        deleteCap.getValue().onSuccess(null);

        ArgumentCaptor<DeleteOwnProfileController.DeleteProfileResult> resCap =
                ArgumentCaptor.forClass(DeleteOwnProfileController.DeleteProfileResult.class);
        verify(cb).onResult(resCap.capture());

        DeleteOwnProfileController.DeleteProfileResult res = resCap.getValue();
        assertTrue(res.isSuccess());
        assertNull(res.getErrorMessage());
    }

    @Test
    public void deleteOwnProfile_noEvents_stillClearsProfile() {
        DeleteOwnProfileController.Callback cb = mock(DeleteOwnProfileController.Callback.class);

        controller.deleteOwnProfile(DEVICE_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<String>>> eventsCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getEntrantEvents(eq(DEVICE_ID), eventsCap.capture());

        eventsCap.getValue().onSuccess(Collections.emptyList());

        verify(mockEventDb, never()).removeEntrantFromEvent(anyString(), anyString(), any());
        verify(mockEntrantDb).deleteAllEntrantEvents(eq(DEVICE_ID), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Void>> deleteCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).deleteProfile(eq(DEVICE_ID), eq(false), deleteCap.capture());
        
        deleteCap.getValue().onSuccess(null);

        ArgumentCaptor<DeleteOwnProfileController.DeleteProfileResult> resCap =
                ArgumentCaptor.forClass(DeleteOwnProfileController.DeleteProfileResult.class);
        verify(cb).onResult(resCap.capture());
        assertTrue(resCap.getValue().isSuccess());
    }

    @Test
    public void deleteOwnProfile_getEventsError_stillProceedsWithCleanup() {
        DeleteOwnProfileController.Callback cb = mock(DeleteOwnProfileController.Callback.class);

        controller.deleteOwnProfile(DEVICE_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<String>>> eventsCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getEntrantEvents(eq(DEVICE_ID), eventsCap.capture());

        eventsCap.getValue().onError(new RuntimeException("DB error"));

        verify(mockEntrantDb).deleteAllEntrantEvents(eq(DEVICE_ID), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Void>> deleteCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).deleteProfile(eq(DEVICE_ID), eq(false), deleteCap.capture());
        
        deleteCap.getValue().onSuccess(null);

        ArgumentCaptor<DeleteOwnProfileController.DeleteProfileResult> resCap =
                ArgumentCaptor.forClass(DeleteOwnProfileController.DeleteProfileResult.class);
        verify(cb).onResult(resCap.capture());
        assertTrue(resCap.getValue().isSuccess());
    }

    @Test
    public void deleteOwnProfile_deleteProfileFailure_returnsError() {
        DeleteOwnProfileController.Callback cb = mock(DeleteOwnProfileController.Callback.class);

        controller.deleteOwnProfile(DEVICE_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<String>>> eventsCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getEntrantEvents(eq(DEVICE_ID), eventsCap.capture());

        eventsCap.getValue().onSuccess(Collections.emptyList());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Void>> deleteCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).deleteProfile(eq(DEVICE_ID), eq(false), deleteCap.capture());
        deleteCap.getValue().onError(new RuntimeException("Delete failed"));

        ArgumentCaptor<DeleteOwnProfileController.DeleteProfileResult> resCap =
                ArgumentCaptor.forClass(DeleteOwnProfileController.DeleteProfileResult.class);
        verify(cb).onResult(resCap.capture());

        DeleteOwnProfileController.DeleteProfileResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Failed to delete profile. Please try again.", res.getErrorMessage());
    }
}
