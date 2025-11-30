package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.ProfileController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import androidx.annotation.NonNull;

/**
 * Tests for ProfileController.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class ProfileControllerTests {

    private EntrantDB mockEntrantDb;
    private ProfileController controller;

    @Before
    public void setUp() {
        mockEntrantDb = mock(EntrantDB.class);
        controller = new ProfileController(mockEntrantDb);
    }

    @Test
    public void validate_nullDeviceId_fails() {
        ProfileController.ProfileResult res =
                controller.validateAndCreateProfile(null, "Alice", "a@b.com", "555");
        assertFalse(res.isValid());
        assertEquals("Device ID is required", res.getErrorMessage());
        assertNull(res.getEntrant());
    }

    @Test
    public void validate_emptyName_fails() {
        ProfileController.ProfileResult res =
                controller.validateAndCreateProfile("dev1", "   ", "a@b.com", "555");
        assertFalse(res.isValid());
        assertEquals("Name is required", res.getErrorMessage());
        assertNull(res.getEntrant());
    }

    @Test
    public void validate_emptyEmail_fails() {
        ProfileController.ProfileResult res =
                controller.validateAndCreateProfile("dev1", "Alice", "   ", "555");
        assertFalse(res.isValid());
        assertEquals("Email is required", res.getErrorMessage());
        assertNull(res.getEntrant());
    }

    @Test
    public void validate_badEmail_fails() {
        ProfileController.ProfileResult res =
                controller.validateAndCreateProfile("dev1", "Alice", "not-an-email", "555");
        assertFalse(res.isValid());
        assertEquals("Valid email address is required", res.getErrorMessage());
        assertNull(res.getEntrant());
    }

    @Test
    public void validate_success_allowsNullPhone_setsEmptyString() {
        ProfileController.ProfileResult res =
                controller.validateAndCreateProfile("dev1", "Alice", "a@b.com", null);

        assertTrue(res.isValid());
        Entrant e = res.getEntrant();
        assertNotNull(e);
        assertEquals("", e.getPhone());
    }

    @Test
    public void saveProfile_nullDeviceId_onError() {
        Entrant e = new Entrant("dev1", "Alice", System.currentTimeMillis());
        ProfileControllerTests.VoidCb cb = new ProfileControllerTests.VoidCb();

        controller.saveProfile(null, e, cb);

        assertTrue(cb.errored);
        assertTrue(cb.error instanceof IllegalArgumentException);
        verifyNoInteractions(mockEntrantDb);
    }

    @Test
    public void saveProfile_nullEntrant_onError() {
        ProfileControllerTests.VoidCb cb = new ProfileControllerTests.VoidCb();

        controller.saveProfile("dev1", null, cb);

        assertTrue(cb.errored);
        assertTrue(cb.error instanceof IllegalArgumentException);
        verifyNoInteractions(mockEntrantDb);
    }

    @Test
    public void saveProfile_callsUpsert_andBubblesSuccess() {
        Entrant e = new Entrant("dev1", "Alice", System.currentTimeMillis());
        ProfileControllerTests.VoidCb cb = new ProfileControllerTests.VoidCb();

        controller.saveProfile("dev1", e, cb);

        ArgumentCaptor<EntrantDB.Callback<Void>> cap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).upsertProfile(eq("dev1"), same(e), cap.capture());

        cap.getValue().onSuccess(null);

        assertTrue(cb.succeeded);
        assertFalse(cb.errored);
    }

    @Test
    public void saveProfile_callsUpsert_andBubblesError() {
        Entrant e = new Entrant("dev1", "Alice", System.currentTimeMillis());
        ProfileControllerTests.VoidCb cb = new ProfileControllerTests.VoidCb();

        controller.saveProfile("dev1", e, cb);

        ArgumentCaptor<EntrantDB.Callback<Void>> cap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).upsertProfile(eq("dev1"), same(e), cap.capture());

        RuntimeException boom = new RuntimeException("kaboom");
        cap.getValue().onError(boom);

        assertTrue(cb.errored);
        assertSame(boom, cb.error);
    }

    // ---------- deleteProfile ----------

    @Test
    public void deleteProfile_nullDeviceId_onError() {
        ProfileControllerTests.VoidCb cb = new ProfileControllerTests.VoidCb();

        controller.deleteProfile(null, cb);

        assertTrue(cb.errored);
        assertTrue(cb.error instanceof IllegalArgumentException);
        verifyNoInteractions(mockEntrantDb);
    }

    @Test
    public void deleteProfile_callsDb_andBubblesSuccess() {
        ProfileControllerTests.VoidCb cb = new ProfileControllerTests.VoidCb();

        controller.deleteProfile("dev1", cb);

        ArgumentCaptor<EntrantDB.Callback<Void>> cap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).deleteProfile(eq("dev1"), eq(false), cap.capture());

        cap.getValue().onSuccess(null);

        assertTrue(cb.succeeded);
        assertFalse(cb.errored);
    }

    @Test
    public void deleteProfile_callsDb_andBubblesError() {
        ProfileControllerTests.VoidCb cb = new ProfileControllerTests.VoidCb();

        controller.deleteProfile("dev1", cb);

        ArgumentCaptor<EntrantDB.Callback<Void>> cap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).deleteProfile(eq("dev1"), eq(false), cap.capture());

        Exception oof = new Exception("nope");
        cap.getValue().onError(oof);

        assertTrue(cb.errored);
        assertSame(oof, cb.error);
    }

    private static class VoidCb implements EntrantDB.Callback<Void> {
        boolean succeeded = false;
        boolean errored = false;
        Exception error;

        @Override
        public void onSuccess(Void value) { succeeded = true; }

        @Override
        public void onError(@NonNull Exception e) {
            errored = true;
            error = e;
        }
    }
}
