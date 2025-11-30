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

    @Test
    public void removeProfile_emptyEntrantDeviceId_failsFast() {
        AdminRemoveProfileController.Callback cb = mock(AdminRemoveProfileController.Callback.class);

        controller.removeProfile("", "admin1", cb);

        ArgumentCaptor<AdminRemoveProfileController.RemoveProfileResult> resCap =
                ArgumentCaptor.forClass(AdminRemoveProfileController.RemoveProfileResult.class);
        verify(cb).onResult(resCap.capture());

        AdminRemoveProfileController.RemoveProfileResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("deviceId cannot be null or empty", res.getErrorMessage());

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
        assertEquals("adminDeviceId cannot be null or empty", res.getErrorMessage());

        verifyNoInteractions(mockUserDb, mockEntrantDb, mockEventDb);
    }

    // ---------- admin validation (no Log involved) ----------

    @Test
    public void removeProfile_nonAdmin_returnsAdminRequiredError() {
        AdminRemoveProfileController.Callback cb = mock(AdminRemoveProfileController.Callback.class);

        controller.removeProfile("dev1", "admin1", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq("admin1"), userCap.capture());

        User mockUser = mock(User.class);
        when(mockUser.isAdmin()).thenReturn(false);
        userCap.getValue().onSuccess(mockUser);

        ArgumentCaptor<AdminRemoveProfileController.RemoveProfileResult> resCap =
                ArgumentCaptor.forClass(AdminRemoveProfileController.RemoveProfileResult.class);
        verify(cb).onResult(resCap.capture());

        AdminRemoveProfileController.RemoveProfileResult res = resCap.getValue();
        assertFalse(res.isSuccess());
        assertEquals("Admin access required", res.getErrorMessage());

        verifyNoInteractions(mockEntrantDb, mockEventDb);
    }

    @Test
    public void removeProfile_adminAndEntrantWithEvents_callsExpectedDbOperations() {
        AdminRemoveProfileController.Callback cb = mock(AdminRemoveProfileController.Callback.class);

        controller.removeProfile("dev1", "admin1", cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<UserDB.Callback<User>> userCap =
                ArgumentCaptor.forClass(UserDB.Callback.class);
        verify(mockUserDb).getUser(eq("admin1"), userCap.capture());

        User adminUser = mock(User.class);
        when(adminUser.isAdmin()).thenReturn(true);
        userCap.getValue().onSuccess(adminUser);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<List<String>>> eventsCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getEntrantEvents(eq("dev1"), eventsCap.capture());

        eventsCap.getValue().onSuccess(Arrays.asList("E1", "E2"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<Void>> removeCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb, times(2))
                .removeEntrantFromEvent(anyString(), eq("dev1"), removeCap.capture());

        verify(mockEventDb).removeEntrantFromEvent(eq("E1"), eq("dev1"), any());
        verify(mockEventDb).removeEntrantFromEvent(eq("E2"), eq("dev1"), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Void>> delEventsCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).deleteAllEntrantEvents(eq("dev1"), delEventsCap.capture());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Void>> delProfileCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).deleteProfile(eq("dev1"), eq(true), delProfileCap.capture());

        verifyNoMoreInteractions(cb);
    }
}
