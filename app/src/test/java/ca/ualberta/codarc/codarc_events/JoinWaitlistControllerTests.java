package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.JoinWaitlistController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.models.Event;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.text.SimpleDateFormat;
import java.util.Locale;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class JoinWaitlistControllerTests {

    private EventDB mockEventDb;
    private EntrantDB mockEntrantDb;
    private JoinWaitlistController controller;

    @Before
    public void setUp() {
        mockEventDb = mock(EventDB.class);
        mockEntrantDb = mock(EntrantDB.class);
        controller = new JoinWaitlistController(mockEventDb, mockEntrantDb);
    }

    private static String isoNowPlusSeconds(int delta) {
        long t = System.currentTimeMillis() + delta * 1000L;
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(t);
    }

    private static Event openWindowEvent(String id, Integer maxCap) {
        Event e = new Event();
        e.setId(id);
        e.setRegistrationOpen(isoNowPlusSeconds(-60));
        e.setRegistrationClose(isoNowPlusSeconds(60));
        if (maxCap != null) e.setMaxCapacity(maxCap);
        return e;
    }

    private static Event closedWindowEvent(String id) {
        Event e = new Event();
        e.setId(id);
        e.setRegistrationOpen(isoNowPlusSeconds(60));
        e.setRegistrationClose(isoNowPlusSeconds(120));
        return e;
    }

    private static Entrant entrant(boolean registered) {
        Entrant en = new Entrant();
        en.setIsRegistered(registered);
        return en;
    }

    @Test
    public void checkProfileRegistration_emptyDeviceId_errors() {
        EntrantDB.Callback<Boolean> cb = mock(EntrantDB.Callback.class);
        controller.checkProfileRegistration("", cb);

        verify(cb).onError(isA(IllegalArgumentException.class));
        verifyNoInteractions(mockEntrantDb);
    }

    @Test
    public void checkProfileRegistration_registeredTrue_whenEntrantRegistered() {
        EntrantDB.Callback<Boolean> cb = mock(EntrantDB.Callback.class);

        controller.checkProfileRegistration("dev1", cb);

        ArgumentCaptor<EntrantDB.Callback<Entrant>> profCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq("dev1"), profCap.capture());

        profCap.getValue().onSuccess(entrant(true));

        verify(cb).onSuccess(true);
        verify(cb, never()).onError(any());
    }

    @Test
    public void checkProfileRegistration_registeredFalse_whenEntrantNullOrFalse() {
        EntrantDB.Callback<Boolean> cb = mock(EntrantDB.Callback.class);

        controller.checkProfileRegistration("dev1", cb);

        ArgumentCaptor<EntrantDB.Callback<Entrant>> profCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq("dev1"), profCap.capture());

        profCap.getValue().onSuccess(null);
        verify(cb).onSuccess(false);
    }

    @Test
    public void checkProfileRegistration_errorBubbles() {
        EntrantDB.Callback<Boolean> cb = mock(EntrantDB.Callback.class);

        controller.checkProfileRegistration("dev1", cb);

        ArgumentCaptor<EntrantDB.Callback<Entrant>> profCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq("dev1"), profCap.capture());

        Exception boom = new RuntimeException("getProfile fail");
        profCap.getValue().onError(boom);
        verify(cb).onError(boom);
    }

    @Test
    public void joinWaitlist_nullEvent_fails() {
        JoinWaitlistController.Callback cb = mock(JoinWaitlistController.Callback.class);
        controller.joinWaitlist(null, "dev1", cb);

        ArgumentCaptor<JoinWaitlistController.JoinResult> resCap =
                ArgumentCaptor.forClass(JoinWaitlistController.JoinResult.class);
        verify(cb).onResult(resCap.capture());
        assertFalse(resCap.getValue().isSuccess());
        assertEquals("event cannot be null", resCap.getValue().getMessage());
        verifyNoInteractions(mockEntrantDb, mockEventDb);
    }

    @Test
    public void joinWaitlist_emptyDeviceId_fails() {
        JoinWaitlistController.Callback cb = mock(JoinWaitlistController.Callback.class);
        controller.joinWaitlist(openWindowEvent("E", null), "", cb);

        ArgumentCaptor<JoinWaitlistController.JoinResult> resCap =
                ArgumentCaptor.forClass(JoinWaitlistController.JoinResult.class);
        verify(cb).onResult(resCap.capture());
        assertFalse(resCap.getValue().isSuccess());
        assertEquals("deviceId cannot be null or empty", resCap.getValue().getMessage());
        verifyNoInteractions(mockEntrantDb, mockEventDb);
    }

    @Test
    public void joinWaitlist_requiresProfileRegistration_ifNotRegistered() {
        JoinWaitlistController.Callback cb = mock(JoinWaitlistController.Callback.class);
        Event e = openWindowEvent("E", null);

        controller.joinWaitlist(e, "dev1", cb);

        ArgumentCaptor<EntrantDB.Callback<Entrant>> profCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq("dev1"), profCap.capture());

        profCap.getValue().onSuccess(entrant(false));

        ArgumentCaptor<JoinWaitlistController.JoinResult> resCap =
                ArgumentCaptor.forClass(JoinWaitlistController.JoinResult.class);
        verify(cb).onResult(resCap.capture());

        assertFalse(resCap.getValue().isSuccess());
        assertTrue(resCap.getValue().needsProfileRegistration());
        assertEquals("Profile registration required", resCap.getValue().getMessage());

        verify(mockEventDb, never()).isEntrantOnWaitlist(anyString(), anyString(), any());
    }

    @Test
    public void joinWaitlist_alreadyJoined_fails() {
        JoinWaitlistController.Callback cb = mock(JoinWaitlistController.Callback.class);
        Event e = openWindowEvent("E", null);

        controller.joinWaitlist(e, "dev1", cb);

        ArgumentCaptor<EntrantDB.Callback<Entrant>> profCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq("dev1"), profCap.capture());
        profCap.getValue().onSuccess(entrant(true));

        ArgumentCaptor<EntrantDB.Callback<Boolean>> banCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).isBanned(eq("dev1"), banCap.capture());
        banCap.getValue().onSuccess(false);

        ArgumentCaptor<EventDB.Callback<Boolean>> onCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantOnWaitlist(eq("E"), eq("dev1"), onCap.capture());
        onCap.getValue().onSuccess(true);

        ArgumentCaptor<JoinWaitlistController.JoinResult> resCap =
                ArgumentCaptor.forClass(JoinWaitlistController.JoinResult.class);
        verify(cb).onResult(resCap.capture());
        assertFalse(resCap.getValue().isSuccess());
        assertEquals("Already joined", resCap.getValue().getMessage());

        verify(mockEventDb, never()).getWaitlistCount(anyString(), any());
        verify(mockEventDb, never()).joinWaitlist(anyString(), anyString(), any());
    }

    // ---------------- joinWaitlist: registration window closed ----------------

    @Test
    public void joinWaitlist_registrationClosed_fails() {
        JoinWaitlistController.Callback cb = mock(JoinWaitlistController.Callback.class);
        Event e = closedWindowEvent("E");

        controller.joinWaitlist(e, "dev1", cb);

        ArgumentCaptor<EntrantDB.Callback<Entrant>> profCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq("dev1"), profCap.capture());
        profCap.getValue().onSuccess(entrant(true));

        ArgumentCaptor<EntrantDB.Callback<Boolean>> banCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).isBanned(eq("dev1"), banCap.capture());
        banCap.getValue().onSuccess(false);

        ArgumentCaptor<EventDB.Callback<Boolean>> onCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantOnWaitlist(eq("E"), eq("dev1"), onCap.capture());
        onCap.getValue().onSuccess(false);

        ArgumentCaptor<JoinWaitlistController.JoinResult> resCap =
                ArgumentCaptor.forClass(JoinWaitlistController.JoinResult.class);
        verify(cb).onResult(resCap.capture());

        assertFalse(resCap.getValue().isSuccess());
        assertEquals("Registration window is closed", resCap.getValue().getMessage());

        verify(mockEventDb, never()).getWaitlistCount(anyString(), any());
        verify(mockEventDb, never()).joinWaitlist(anyString(), anyString(), any());
    }

    @Test
    public void joinWaitlist_fullCapacity_fails() {
        JoinWaitlistController.Callback cb = mock(JoinWaitlistController.Callback.class);
        Event e = openWindowEvent("E", 2);

        controller.joinWaitlist(e, "dev1", cb);

        ArgumentCaptor<EntrantDB.Callback<Entrant>> profCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq("dev1"), profCap.capture());
        profCap.getValue().onSuccess(entrant(true));

        ArgumentCaptor<EntrantDB.Callback<Boolean>> banCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).isBanned(eq("dev1"), banCap.capture());
        banCap.getValue().onSuccess(false);

        ArgumentCaptor<EventDB.Callback<Boolean>> onCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantOnWaitlist(eq("E"), eq("dev1"), onCap.capture());
        onCap.getValue().onSuccess(false);

        ArgumentCaptor<EventDB.Callback<Integer>> countCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlistCount(eq("E"), countCap.capture());
        countCap.getValue().onSuccess(2);

        ArgumentCaptor<JoinWaitlistController.JoinResult> resCap =
                ArgumentCaptor.forClass(JoinWaitlistController.JoinResult.class);
        verify(cb).onResult(resCap.capture());

        assertFalse(resCap.getValue().isSuccess());
        assertEquals("Event is full", resCap.getValue().getMessage());

        verify(mockEventDb, never()).joinWaitlist(anyString(), anyString(), any());
    }

    @Test
    public void joinWaitlist_success() {
        JoinWaitlistController.Callback cb = mock(JoinWaitlistController.Callback.class);
        Event e = openWindowEvent("E", 3);

        controller.joinWaitlist(e, "dev1", cb);

        ArgumentCaptor<EntrantDB.Callback<Entrant>> profCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq("dev1"), profCap.capture());
        profCap.getValue().onSuccess(entrant(true));

        ArgumentCaptor<EntrantDB.Callback<Boolean>> banCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).isBanned(eq("dev1"), banCap.capture());
        banCap.getValue().onSuccess(false);

        ArgumentCaptor<EventDB.Callback<Boolean>> onCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantOnWaitlist(eq("E"), eq("dev1"), onCap.capture());
        onCap.getValue().onSuccess(false);

        ArgumentCaptor<EventDB.Callback<Integer>> countCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlistCount(eq("E"), countCap.capture());
        countCap.getValue().onSuccess(0);

        ArgumentCaptor<EventDB.Callback<Void>> joinCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).joinWaitlist(eq("E"), eq("dev1"), isNull(), joinCap.capture());

        joinCap.getValue().onSuccess(null);

        ArgumentCaptor<JoinWaitlistController.JoinResult> resCap =
                ArgumentCaptor.forClass(JoinWaitlistController.JoinResult.class);
        verify(cb).onResult(resCap.capture());

        assertTrue(resCap.getValue().isSuccess());
        assertEquals("Joined successfully", resCap.getValue().getMessage());
        assertFalse(resCap.getValue().needsProfileRegistration());
    }

    @Test
    public void joinWaitlist_joinDbError_yieldsGenericFailure() {
        JoinWaitlistController.Callback cb = mock(JoinWaitlistController.Callback.class);
        Event e = openWindowEvent("E", 3);

        controller.joinWaitlist(e, "dev1", cb);

        ArgumentCaptor<EntrantDB.Callback<Entrant>> profCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq("dev1"), profCap.capture());
        profCap.getValue().onSuccess(entrant(true));

        ArgumentCaptor<EntrantDB.Callback<Boolean>> banCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).isBanned(eq("dev1"), banCap.capture());
        banCap.getValue().onSuccess(false);

        ArgumentCaptor<EventDB.Callback<Boolean>> onCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantOnWaitlist(eq("E"), eq("dev1"), onCap.capture());
        onCap.getValue().onSuccess(false);

        ArgumentCaptor<EventDB.Callback<Integer>> countCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlistCount(eq("E"), countCap.capture());
        countCap.getValue().onSuccess(0);

        ArgumentCaptor<EventDB.Callback<Void>> joinCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).joinWaitlist(eq("E"), eq("dev1"), isNull(), joinCap.capture());

        joinCap.getValue().onError(new RuntimeException("db sad"));

        ArgumentCaptor<JoinWaitlistController.JoinResult> resCap =
                ArgumentCaptor.forClass(JoinWaitlistController.JoinResult.class);
        verify(cb).onResult(resCap.capture());

        assertFalse(resCap.getValue().isSuccess());
        assertEquals("Failed to join. Please try again.", resCap.getValue().getMessage());
    }

    @Test
    public void joinWaitlist_isEntrantOnWaitlist_error_yieldsStatusFailure() {
        JoinWaitlistController.Callback cb = mock(JoinWaitlistController.Callback.class);
        Event e = openWindowEvent("E", null);

        controller.joinWaitlist(e, "dev1", cb);

        ArgumentCaptor<EntrantDB.Callback<Entrant>> profCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq("dev1"), profCap.capture());
        profCap.getValue().onSuccess(entrant(true));

        ArgumentCaptor<EntrantDB.Callback<Boolean>> banCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).isBanned(eq("dev1"), banCap.capture());
        banCap.getValue().onSuccess(false);

        ArgumentCaptor<EventDB.Callback<Boolean>> onCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantOnWaitlist(eq("E"), eq("dev1"), onCap.capture());
        onCap.getValue().onError(new RuntimeException("status fail"));

        ArgumentCaptor<JoinWaitlistController.JoinResult> resCap =
                ArgumentCaptor.forClass(JoinWaitlistController.JoinResult.class);
        verify(cb).onResult(resCap.capture());

        assertFalse(resCap.getValue().isSuccess());
        assertEquals("Failed to check status. Please try again.", resCap.getValue().getMessage());
    }

    @Test
    public void joinWaitlist_getWaitlistCount_error_yieldsAvailabilityFailure() {
        JoinWaitlistController.Callback cb = mock(JoinWaitlistController.Callback.class);
        Event e = openWindowEvent("E", 3);

        controller.joinWaitlist(e, "dev1", cb);

        ArgumentCaptor<EntrantDB.Callback<Entrant>> profCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq("dev1"), profCap.capture());
        profCap.getValue().onSuccess(entrant(true));

        ArgumentCaptor<EntrantDB.Callback<Boolean>> banCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).isBanned(eq("dev1"), banCap.capture());
        banCap.getValue().onSuccess(false);

        ArgumentCaptor<EventDB.Callback<Boolean>> onCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).isEntrantOnWaitlist(eq("E"), eq("dev1"), onCap.capture());
        onCap.getValue().onSuccess(false);

        ArgumentCaptor<EventDB.Callback<Integer>> countCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlistCount(eq("E"), countCap.capture());
        countCap.getValue().onError(new RuntimeException("count fail"));

        ArgumentCaptor<JoinWaitlistController.JoinResult> resCap =
                ArgumentCaptor.forClass(JoinWaitlistController.JoinResult.class);
        verify(cb).onResult(resCap.capture());

        assertFalse(resCap.getValue().isSuccess());
        assertEquals("Failed to check availability", resCap.getValue().getMessage());
    }

    @Test
    public void joinWaitlist_checkProfile_error_yieldsProfileFailure() {
        JoinWaitlistController.Callback cb = mock(JoinWaitlistController.Callback.class);
        Event e = openWindowEvent("E", null);

        controller.joinWaitlist(e, "dev1", cb);

        ArgumentCaptor<EntrantDB.Callback<Entrant>> profCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq("dev1"), profCap.capture());

        profCap.getValue().onError(new RuntimeException("profile down"));

        ArgumentCaptor<JoinWaitlistController.JoinResult> resCap =
                ArgumentCaptor.forClass(JoinWaitlistController.JoinResult.class);
        verify(cb).onResult(resCap.capture());

        assertFalse(resCap.getValue().isSuccess());
        assertEquals("Failed to check profile", resCap.getValue().getMessage());
    }

    @Test
    public void getWaitlistCount_forwardsToDb() {
        EventDB.Callback<Integer> cb = mock(EventDB.Callback.class);
        controller.getWaitlistCount("E", cb);
        verify(mockEventDb).getWaitlistCount("E", cb);
    }
}
