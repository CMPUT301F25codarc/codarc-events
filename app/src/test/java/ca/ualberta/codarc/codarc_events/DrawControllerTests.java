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
        mockEventDb = mock(EventDB.class);
        mockEntrantDb = mock(EntrantDB.class);
        controller = new DrawController(mockEventDb, mockEntrantDb);
    }

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

    @Test
    public void runDraw_defaultReplacementPool_isThree() {
        DrawController.DrawCallback cb = mock(DrawController.DrawCallback.class);

        controller.runDraw("E", 2, cb);

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> wlCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("E"), wlCap.capture());

        List<Map<String, Object>> waitlist = ids("A","B","C","D");
        wlCap.getValue().onSuccess(waitlist);

        ArgumentCaptor<List<String>> winnersCap = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<String>> repsCap = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<EventDB.Callback<Void>> mwCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).markWinners(eq("E"), winnersCap.capture(), repsCap.capture(), mwCap.capture());

        List<String> winners = winnersCap.getValue();
        List<String> reps = repsCap.getValue();

        assertEquals(2, winners.size());
        assertEquals(2, reps.size());
        assertDisjoint(winners, reps);
        assertMembersOf(winners, "A","B","C","D");
        assertMembersOf(reps, "A","B","C","D");

        mwCap.getValue().onSuccess(null);
        verify(cb, never()).onError(any());
    }

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

        assertEquals(2, winners.size());
        assertEquals(3, reps.size());
        assertDisjoint(winners, reps);
        assertMembersOf(winners, "A","B","C","D","E");
        assertMembersOf(reps, "A","B","C","D","E");

        Set<String> union = new HashSet<>(winners);
        union.addAll(reps);
        assertEquals(5, union.size());

        mwCap.getValue().onSuccess(null);
        verify(cb, never()).onError(any());
    }

    @Test
    public void runDraw_winnersMoreThanTotal_replacementsEmpty() {
        DrawController.DrawCallback cb = mock(DrawController.DrawCallback.class);

        controller.runDraw("E", 5, 3, cb);

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

        assertEquals(2, winners.size());
        assertTrue(reps.isEmpty());
        assertMembersOf(winners, "X","Y");

        mwCap.getValue().onSuccess(null);
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
        waitlist.add(Collections.singletonMap("deviceId", null));
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

        assertTrue(winners.size() <= 3);
        assertMembersOf(winners, "A","B","C","D");
        assertMembersOf(reps, "A","B","C","D");
        assertDisjoint(winners, reps);

        mwCap.getValue().onSuccess(null);
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

        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> wlCap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getWaitlist(eq("E"), wlCap.capture());
        wlCap.getValue().onSuccess(ids("A","B"));

        ArgumentCaptor<EventDB.Callback<Void>> mwCap = ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).markWinners(eq("E"), anyList(), anyList(), mwCap.capture());

        Exception boom = new RuntimeException("markWinners fail");
        mwCap.getValue().onError(boom);

        verify(cb).onError(boom);
        verify(cb, never()).onSuccess(anyList(), anyList());
    }

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
