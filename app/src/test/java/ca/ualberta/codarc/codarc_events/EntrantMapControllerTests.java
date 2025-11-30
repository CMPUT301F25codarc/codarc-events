package ca.ualberta.codarc.codarc_events;

import android.util.Log;
import ca.ualberta.codarc.codarc_events.controllers.EntrantMapController;
import ca.ualberta.codarc.codarc_events.data.EntrantDB;
import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import com.google.firebase.firestore.GeoPoint;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class EntrantMapControllerTests {

    private EventDB mockEventDb;
    private EntrantDB mockEntrantDb;
    private EntrantMapController controller;
    private MockedStatic<Log> logMock;

    private static final String EVENT_ID = "event-123";
    private static final String DEVICE_ID = "dev-456";

    @Before
    public void setUp() {
        logMock = Mockito.mockStatic(Log.class);
        mockEventDb = mock(EventDB.class);
        mockEntrantDb = mock(EntrantDB.class);
        controller = new EntrantMapController(mockEventDb, mockEntrantDb);
    }

    @After
    public void tearDown() {
        if (logMock != null) {
            logMock.close();
        }
    }

    @Test
    public void loadMapData_emptyList_returnsEmpty() {
        EntrantMapController.MapDataCallback cb = mock(EntrantMapController.MapDataCallback.class);

        controller.loadMapData(EVENT_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEntrantsWithLocations(eq(EVENT_ID), cap.capture());

        cap.getValue().onSuccess(new ArrayList<>());

        ArgumentCaptor<List<EntrantMapController.MapMarkerData>> markersCap =
                ArgumentCaptor.forClass(List.class);
        verify(cb).onSuccess(markersCap.capture());

        assertTrue(markersCap.getValue().isEmpty());
    }

    @Test
    public void loadMapData_success_returnsMarkersWithResolvedNames() {
        EntrantMapController.MapDataCallback cb = mock(EntrantMapController.MapDataCallback.class);

        controller.loadMapData(EVENT_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEntrantsWithLocations(eq(EVENT_ID), cap.capture());

        List<Map<String, Object>> entries = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("deviceId", DEVICE_ID);
        entry.put("joinLocation", new GeoPoint(53.5461, -113.4938));
        entry.put("timestamp", 1234567890L);
        entries.add(entry);
        cap.getValue().onSuccess(entries);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Entrant>> entrantCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq(DEVICE_ID), entrantCap.capture());

        Entrant entrant = new Entrant();
        entrant.setName("Test User");
        entrantCap.getValue().onSuccess(entrant);

        ArgumentCaptor<List<EntrantMapController.MapMarkerData>> markersCap =
                ArgumentCaptor.forClass(List.class);
        verify(cb).onSuccess(markersCap.capture());

        List<EntrantMapController.MapMarkerData> markers = markersCap.getValue();
        assertEquals(1, markers.size());
        assertEquals(DEVICE_ID, markers.get(0).getDeviceId());
        assertEquals("Test User", markers.get(0).getEntrantName());
        assertEquals(53.5461, markers.get(0).getLatitude(), 0.0001);
        assertEquals(-113.4938, markers.get(0).getLongitude(), 0.0001);
        assertEquals(1234567890L, markers.get(0).getJoinedAt());
    }

    @Test
    public void loadMapData_getEntrantsError_propagates() {
        EntrantMapController.MapDataCallback cb = mock(EntrantMapController.MapDataCallback.class);

        controller.loadMapData(EVENT_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEntrantsWithLocations(eq(EVENT_ID), cap.capture());

        Exception error = new RuntimeException("DB error");
        cap.getValue().onError(error);

        ArgumentCaptor<Exception> exCap = ArgumentCaptor.forClass(Exception.class);
        verify(cb).onError(exCap.capture());
        assertSame(error, exCap.getValue());
    }

    @Test
    public void loadMapData_getEntrantError_usesUnknownName() {
        EntrantMapController.MapDataCallback cb = mock(EntrantMapController.MapDataCallback.class);

        controller.loadMapData(EVENT_ID, cb);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EventDB.Callback<List<Map<String, Object>>>> cap =
                ArgumentCaptor.forClass(EventDB.Callback.class);
        verify(mockEventDb).getEntrantsWithLocations(eq(EVENT_ID), cap.capture());

        List<Map<String, Object>> entries = new ArrayList<>();
        Map<String, Object> entry = new HashMap<>();
        entry.put("deviceId", DEVICE_ID);
        entry.put("joinLocation", new GeoPoint(53.5461, -113.4938));
        entry.put("timestamp", 1234567890L);
        entries.add(entry);
        cap.getValue().onSuccess(entries);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<EntrantDB.Callback<Entrant>> entrantCap =
                ArgumentCaptor.forClass(EntrantDB.Callback.class);
        verify(mockEntrantDb).getProfile(eq(DEVICE_ID), entrantCap.capture());

        entrantCap.getValue().onError(new RuntimeException("Not found"));

        ArgumentCaptor<List<EntrantMapController.MapMarkerData>> markersCap =
                ArgumentCaptor.forClass(List.class);
        verify(cb).onSuccess(markersCap.capture());

        List<EntrantMapController.MapMarkerData> markers = markersCap.getValue();
        assertEquals(1, markers.size());
        assertEquals(DEVICE_ID, markers.get(0).getEntrantName());
    }
}
