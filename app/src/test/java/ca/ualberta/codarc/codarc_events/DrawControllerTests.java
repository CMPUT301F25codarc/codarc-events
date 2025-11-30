package ca.ualberta.codarc.codarc_events;

import ca.ualberta.codarc.codarc_events.controllers.DrawController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Order-agnostic tests for DrawController.
 * We don't mock Collections.shuffle; instead we assert sizes/membership/disjointness.
 */
public class DrawControllerTests {

    private EventDB mockEventDb;
    private EntrantDB mockEntrantDb;
    private DrawController controller;

    @Before
    public void setUp() {
        // Mock both DB dependencies so no Firebase / Android APIs are touched
        mockEventDb = mock(EventDB.class);
        mockEntrantDb = mock(EntrantDB.class);

        // Uses a DI constructor in DrawController: DrawController(EventDB, EntrantDB)
        controller = new DrawController(mockEventDb, mockEntrantDb);
    }

    // ---------------- loadEntrantCount ----------------

    @Test
    public void loadEntrantCount_forwardsSuccess() {
        DrawController.CountCallback cb = mock(DrawController.CountCallback.class);
        String eventId = "E1";

        controller.loadEntrantCount(eventId, cb);

        ArgumentCaptor<EventDB.Callback<Integer>> cap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlistCount(eq(eventId), cap.capture());

        cap.getValue().onSuccess(42);

        verify(cb).onSuccess(42);
        verify(cb, never()).onError(any());
    }

    @Test
    public void loadEntrantCount_forwardsError() {
        DrawController.CountCallback cb = mock(DrawController.CountCallback.class);
        String eventId = "E2";

        controller.loadEntrantCount(eventId, cb);

        ArgumentCaptor<EventDB.Callback<Integer>> cap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlistCount(eq(eventId), cap.capture());

        Exception boom = new RuntimeException("count fail");
        cap.getValue().onError(boom);

        verify(cb).onError(boom);
        verify(cb, never()).onSuccess(anyInt());
    }

    // ---------------- runDraw: validations ----------------

    @Test
    public void runDraw_validation_nullEventId() {
        DrawController.DrawCallback cb = mock(DrawController.DrawCallback.class);

        controller.runDraw(null, 1, 3, cb);

        verify(cb).onError(isA(IllegalArgumentException.class));
        verify(mockEventDb, never()).getWaitlist(anyString(), any());
    }

    @Test
    public void runDraw_validation_emptyEventId() {
        DrawController.DrawCallback cb = mock(DrawController.DrawCallback.class);

        controller.runDraw("", 1, 3, cb);

        verify(cb).onError(isA(IllegalArgumentException.class));
        verify(mockEventDb, never()).getWaitlist(anyString(), any());
    }

    @Test
    public void runDraw_validation_nonPositiveWinners() {
        DrawController.DrawCallback cb = mock(DrawController.DrawCallback.class);

        controller.runDraw("E", 0, 3, cb);

        verify(cb).onError(isA(IllegalArgumentException.class));
        verify(mockEventDb, never()).getWaitlist(anyString(), any());
    }

    @Test
    public void runDraw_validation_negativeReplacementPool() {
        DrawController.DrawCallback cb = mock(DrawController.DrawCallback.class);

        controller.runDraw("E", 2, -1, cb);

        verify(cb).onError(isA(IllegalArgumentException.class));
        verify(mockEventDb, never()).getWaitlist(anyString(), any());
    }

    // ---------------- runDraw: default overload uses pool size 3 ----------------

    @Test
    public void runDraw_defaultReplacementPool_isThree() {
        DrawController.DrawCallback cb = mock(DrawController.DrawCallback.class);

        controller.runDraw("E", 2, cb); // uses DEFAULT_REPLACEMENT_POOL_SIZE = 3

        // capture waitlist callback
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> wlCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("E"), wlCap.capture());

        List<Map<String, Object>> waitlist = ids("A","B","C","D"); // 4 entrants
        wlCap.getValue().onSuccess(waitlist);

        // capture markWinners
        ArgumentCaptor<List<String>> winnersCap = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> repsCap = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<EventDB.Callback<Void>> mwCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).markWinners(eq("E"), winnersCap.capture(), repsCap.capture(), mwCap.capture());

        List<String> winners = winnersCap.getValue();
        List<String> reps = repsCap.getValue();

        // sizes: winners=min(2,4)=2; replacements=min(3, 4-2)=2
        assertEquals(2, winners.size());
        assertEquals(2, reps.size());

        // disjoint and members from original set
        assertDisjoint(winners, reps);
        assertMembersOf(winners, "A","B","C","D");
        assertMembersOf(reps, "A","B","C","D");

        // simulate DB success
        mwCap.getValue().onSuccess(null);

        // Controller may not call callback on success, just assert it did not report error
        verify(cb, never()).onError(any());
    }

    // ---------------- runDraw: branches ----------------

    @Test
    public void runDraw_noEntrants_isError() {
        DrawController.DrawCallback cb = mock(DrawController.DrawCallback.class);

        controller.runDraw("E", 2, 3, cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> wlCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("E"), wlCap.capture());

        wlCap.getValue().onSuccess(Collections.emptyList());

        verify(cb).onError(isA(RuntimeException.class));
        verify(mockEventDb, never()).markWinners(anyString(), anyList(), anyList(), any());
    }

    @Test
    public void runDraw_selectsWinnersAndReplacements_basic_orderAgnostic() {
        DrawController.DrawCallback cb = mock(DrawController.DrawCallback.class);

        controller.runDraw("E", 2, 3, cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> wlCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("E"), wlCap.capture());

        List<Map<String, Object>> waitlist = ids("A","B","C","D","E");
        wlCap.getValue().onSuccess(waitlist);

        ArgumentCaptor<List<String>> winnersCap = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> repsCap = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<EventDB.Callback<Void>> mwCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).markWinners(eq("E"), winnersCap.capture(), repsCap.capture(), mwCap.capture());

        List<String> winners = winnersCap.getValue();
        List<String> reps = repsCap.getValue();

        // winners=min(2,5)=2; replacements=min(3, 5-2)=3
        assertEquals(2, winners.size());
        assertEquals(3, reps.size());
        assertDisjoint(winners, reps);
        assertMembersOf(winners, "A","B","C","D","E");
        assertMembersOf(reps, "A","B","C","D","E");

        // union size should be 5 unique
        Set<String> union = new HashSet<>(winners);
        union.addAll(reps);
        assertEquals(5, union.size());

        mwCap.getValue().onSuccess(null);

        // Just ensure no error callback is fired
        verify(cb, never()).onError(any());
    }

    @Test
    public void runDraw_winnersMoreThanTotal_replacementsEmpty() {
        DrawController.DrawCallback cb = mock(DrawController.DrawCallback.class);

        controller.runDraw("E", 5, 3, cb); // ask for more winners than entrants

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> wlCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("E"), wlCap.capture());

        List<Map<String, Object>> waitlist = ids("X","Y");
        wlCap.getValue().onSuccess(waitlist);

        ArgumentCaptor<List<String>> winnersCap = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> repsCap = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<EventDB.Callback<Void>> mwCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).markWinners(eq("E"), winnersCap.capture(), repsCap.capture(), mwCap.capture());

        List<String> winners = winnersCap.getValue();
        List<String> reps = repsCap.getValue();

        // winners=min(5,2)=2; replacements=min(3, 2-2)=0
        assertEquals(2, winners.size());
        assertTrue(reps.isEmpty());
        assertMembersOf(winners, "X","Y");

        mwCap.getValue().onSuccess(null);

        // No error expected
        verify(cb, never()).onError(any());
    }

    @Test
    public void runDraw_nullDeviceIds_areSkippedButDoNotCrash() {
        DrawController.DrawCallback cb = mock(DrawController.DrawCallback.class);

        controller.runDraw("E", 3, 3, cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> wlCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("E"), wlCap.capture());

        List<Map<String, Object>> waitlist = new ArrayList<>();
        waitlist.add(mapId("A"));
        waitlist.add(Collections.singletonMap("deviceId", null)); // null id
        waitlist.add(mapId("B"));
        waitlist.add(mapId("C"));
        waitlist.add(mapId("D"));
        wlCap.getValue().onSuccess(waitlist);

        ArgumentCaptor<List<String>> winnersCap = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> repsCap = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<EventDB.Callback<Void>> mwCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).markWinners(eq("E"), winnersCap.capture(), repsCap.capture(), mwCap.capture());

        List<String> winners = winnersCap.getValue();
        List<String> reps = repsCap.getValue();

        // We asked for 3 winners, but one ID in the first 3 could be null.
        // So winners.size() can be 2 or 3 depending on shuffle. We only assert it never exceeds 3 and members are valid.
        assertTrue(winners.size() <= 3);
        assertMembersOf(winners, "A","B","C","D"); // null is skipped, so not present
        assertMembersOf(reps, "A","B","C","D");    // replacements also skip nulls implicitly
        assertDisjoint(winners, reps);

        mwCap.getValue().onSuccess(null);

        // Again, only care that it did not treat this as an error
        verify(cb, never()).onError(any());
    }

    @Test
    public void runDraw_getWaitlist_errorPropagates() {
        DrawController.DrawCallback cb = mock(DrawController.DrawCallback.class);

        controller.runDraw("E", 2, 3, cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> wlCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("E"), wlCap.capture());

        Exception boom = new RuntimeException("getWaitlist fail");
        wlCap.getValue().onError(boom);

        verify(cb).onError(boom);
        verify(mockEventDb, never()).markWinners(anyString(), anyList(), anyList(), any());
    }

    @Test
    public void runDraw_markWinners_errorPropagates() {
        DrawController.DrawCallback cb = mock(DrawController.DrawCallback.class);

        controller.runDraw("E", 2, 1, cb);

        // getWaitlist
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> wlCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("E"), wlCap.capture());
        wlCap.getValue().onSuccess(ids("A","B"));

        // markWinners
        ArgumentCaptor<EventDB.Callback<Void>> mwCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).markWinners(eq("E"), anyList(), anyList(), mwCap.capture());

        Exception boom = new RuntimeException("markWinners fail");
        mwCap.getValue().onError(boom);

        verify(cb).onError(boom);
        verify(cb, never()).onSuccess(anyList(), anyList());
    }

    // ---------------- helpers ----------------

    private static List<Map<String, Object>> ids(String... deviceIds) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String id : deviceIds) {
            list.add(mapId(id));
        }
        return list;
    }

    private static Map<String, Object> mapId(String id) {
        return Collections.singletonMap("deviceId", id);
    }

    private static void assertMembersOf(Collection<String> actual, String... domain) {
        Set<String> allowed = new HashSet<>(Arrays.asList(domain));
        for (String s : actual) {
            assertTrue("unexpected id: " + s, allowed.contains(s));
        }
    }

    private static void assertDisjoint(Collection<String> a, Collection<String> b) {
        Set<String> s = new HashSet<>(a);
        s.retainAll(b);
        assertTrue("expected disjoint sets, intersection=" + s, s.isEmpty());
    }
}
