package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.AdminRemoveProfileController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.data.UserDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.models.User;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RemoveProfileControllerTests {

    private EntrantDB mockEntrantDb;
    private EventDB mockEventDb;
    private UserDB mockUserDb;
    private AdminRemoveProfileController controller;

    @Before
    public void setUp() {
        mockEntrantDb = mock(EntrantDB.class);
        mockEventDb = mock(EventDB.class);
        mockUserDb = mock(UserDB.class);
        controller = new AdminRemoveProfileController(mockEntrantDb, mockEventDb, mockUserDb);
    }

    // ---------- validation ----------

    @Test
    public void removeProfile_emptyEntrantDeviceId_failsFast() {
        AdminRemoveProfileController.Callback cb = mock(AdminRemoveProfileController.Callback.class);

        controller.removeProfile("", "admin1", cb);

        ArgumentCaptor<AdminRemoveProfileController.RemoveProfileResult> resCap =
                ArgumentCaptor.forClass(AdminRemoveProfileController.RemoveProfileResult.class);
        verify(cb).onResult(resCap.capture());

        AdminRemoveProfileController.RemoveProfileResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Entrant device ID is required", res.getErrorMessage());

        verifyNoInteractions(mockUserDb, mockEntrantDb, mockEventDb);
    }

    @Test
    public void removeProfile_emptyAdminDeviceId_failsFast() {
        AdminRemoveProfileController.Callback cb = mock(AdminRemoveProfileController.Callback.class);

        controller.removeProfile("dev1", "", cb);

        ArgumentCaptor<AdminRemoveProfileController.RemoveProfileResult> resCap =
                ArgumentCaptor.forClass(AdminRemoveProfileController.RemoveProfileResult.class);
        verify(cb).onResult(resCap.capture());

        AdminRemoveProfileController.RemoveProfileResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Admin device ID is required", res.getErrorMessage());

        verifyNoInteractions(mockUserDb, mockEntrantDb, mockEventDb);
    }

    // ---------- admin validation (no Log involved) ----------

    @Test
    public void removeProfile_nonAdmin_returnsAdminRequiredError() {
        AdminRemoveProfileController.Callback cb = mock(AdminRemoveProfileController.Callback.class);

        controller.removeProfile("dev1", "admin1", cb);

        // userDB.getUser("admin1", callback)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq("admin1"), userCap.capture());

        User mockUser = mock(User.class);
        when(mockUser.isAdmin()).thenReturn(false);

        // Trigger non admin success path (no Log)
        userCap.getValue().onSuccess(mockUser);

        ArgumentCaptor<AdminRemoveProfileController.RemoveProfileResult> resCap =
                ArgumentCaptor.forClass(AdminRemoveProfileController.RemoveProfileResult.class);
        verify(cb).onResult(resCap.capture());

        AdminRemoveProfileController.RemoveProfileResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Admin access required", res.getErrorMessage());

        verifyNoInteractions(mockEntrantDb, mockEventDb);
    }

    // ---------- happy path wiring (avoid calling callbacks that log) ----------

    @Test
    public void removeProfile_adminAndEntrantWithEvents_callsExpectedDbOperations() {
        AdminRemoveProfileController.Callback cb = mock(AdminRemoveProfileController.Callback.class);

        controller.removeProfile("dev1", "admin1", cb);

        // 1) Admin validation
        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq("admin1"), userCap.capture());

        User adminUser = mock(User.class);
        when(adminUser.isAdmin()).thenReturn(true);
        userCap.getValue().onSuccess(adminUser); // no Log in this path

        // 2) Entrant exists
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Entrant>> entrantCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq("dev1"), entrantCap.capture());

        Entrant entrant = new Entrant();
        entrantCap.getValue().onSuccess(entrant); // onSuccess path has no Log

        // 3) Entrant events list
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<String>>> eventsCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getEntrantEvents(eq("dev1"), eventsCap.capture());

        // Use non empty list so removeFromAllEvents does not hit the "empty" Log branch
        eventsCap.getValue().onSuccess(Arrays.asList("E1", "E2"));

        // At this point, controller will:
        // - call eventDB.removeEntrantFromEvent for E1, E2
        // - call entrantDB.deleteAllEntrantEvents("dev1")
        // - call entrantDB.deleteProfile("dev1")
        // We do NOT trigger any of those callbacks to avoid android.util.Log usage.

        // Verify removal from events
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Void>> removeCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb, times(2))
                .removeEntrantFromEvent(anyString(), eq("dev1"), removeCap.capture());

        // The order of calls should correspond to E1 then E2
        verify(mockEventDb).removeEntrantFromEvent(eq("E1"), eq("dev1"), any());
        verify(mockEventDb).removeEntrantFromEvent(eq("E2"), eq("dev1"), any());

        // Verify entrant events subcollection deletion
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Void>> delEventsCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).deleteAllEntrantEvents(eq("dev1"), delEventsCap.capture());

        // Verify profile wipe
        // Note: AdminRemoveProfileController uses DeleteOwnProfileController internally,
        // which calls deleteProfileInternal, which then calls entrantDB.deleteProfile with shouldBan=true
        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Void>> delProfileCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).deleteProfile(eq("dev1"), eq(true), delProfileCap.capture());

        // Important: do NOT call delEventsCap.getValue().onSuccess(...)
        // or delProfileCap.getValue().onSuccess(...), since those paths log

        // Because we never trigger deleteProfile's callback, controller never calls cb.onResult,
        // so we also do not assert on cb here.
        verifyNoMoreInteractions(cb);
    }
}
