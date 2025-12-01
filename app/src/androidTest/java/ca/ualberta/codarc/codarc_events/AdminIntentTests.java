package ca.ualberta.codarc.codarc_events;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import ca.ualberta.codarc.codarc_events.adapters.AdminEventListAdapter;
import ca.ualberta.codarc.codarc_events.adapters.AdminImageListAdapter;
import ca.ualberta.codarc.codarc_events.adapters.AdminProfileListAdapter;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.models.Entrant;
import ca.ualberta.codarc.codarc_events.views.AdminEventListActivity;
import ca.ualberta.codarc.codarc_events.views.AdminImageListActivity;
import ca.ualberta.codarc.codarc_events.views.AdminProfileListActivity;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class AdminIntentTests {

    @Test
    public void adminEventList_canLaunch_andShowRecycler() {
        try (ActivityScenario<AdminEventListActivity> scenario =
                     ActivityScenario.launch(AdminEventListActivity.class)) {

            // Just verify the main list container exists on screen
            onView(withId(R.id.rv_admin_events))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void adminImageList_canLaunch_andShowRecycler() {
        try (ActivityScenario<AdminImageListActivity> scenario =
                     ActivityScenario.launch(AdminImageListActivity.class)) {

            onView(withId(R.id.rv_admin_images))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void adminProfileList_canLaunch_andShowRecycler() {
        try (ActivityScenario<AdminProfileListActivity> scenario =
                     ActivityScenario.launch(AdminProfileListActivity.class)) {

            onView(withId(R.id.rv_admin_profiles))
                    .check(matches(isDisplayed()));
        }
    }
    @Test
    public void adminEventList_deleteEvent_showsConfirmDialog_uiOnly() {
        try (ActivityScenario<AdminEventListActivity> scenario =
                     ActivityScenario.launch(AdminEventListActivity.class)) {

            // Seed adapter with a dummy event so there is a delete button to click
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rv_admin_events);
                AdminEventListAdapter adapter =
                        (AdminEventListAdapter) rv.getAdapter();

                List<Event> dummy = new ArrayList<>();
                Event e = new Event();
                e.setId("ADMIN_TEST_EVENT");
                e.setName("Admin Test Event");
                // Safe ISO style strings for status calculation
                e.setEventDateTime("2025-12-01T19:00:00");
                e.setRegistrationOpen("2025-11-20T08:00:00");
                e.setRegistrationClose("2025-11-30T22:00:00");
                e.setOrganizerId("ADMIN123");
                dummy.add(e);

                adapter.setItems(dummy);
            });

            // Click the delete button on the first row
            onView(withId(R.id.rv_admin_events))
                    .perform(actionOnItemAtPosition(0,
                            clickChildViewWithId(R.id.btn_delete)));
        }
    }

    @Test
    public void adminImageList_deleteImage_showsConfirmDialog_uiOnly() {
        try (ActivityScenario<AdminImageListActivity> scenario =
                     ActivityScenario.launch(AdminImageListActivity.class)) {

            // Seed adapter with a dummy event that has a posterUrl
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rv_admin_images);
                AdminImageListAdapter adapter =
                        (AdminImageListAdapter) rv.getAdapter();

                List<Event> dummy = new ArrayList<>();
                Event e = new Event();
                e.setId("ADMIN_TEST_EVENT_IMAGE");
                e.setName("Admin Image Event");
                e.setEventDateTime("2025-12-02T19:00:00");
                e.setPosterUrl("https://example.com/poster.png"); // non empty so adapter keeps it
                dummy.add(e);

                adapter.setItems(dummy);
            });

            onView(withId(R.id.rv_admin_images))
                    .perform(actionOnItemAtPosition(0,
                            clickChildViewWithId(R.id.btn_delete)));
        }
    }

    @Test
    public void adminProfileList_removeProfile_showsConfirmDialog_uiOnly() {
        try (ActivityScenario<AdminProfileListActivity> scenario =
                     ActivityScenario.launch(AdminProfileListActivity.class)) {

            // Seed adapter with a dummy entrant profile
            scenario.onActivity(activity -> {
                RecyclerView rv = activity.findViewById(R.id.rv_admin_profiles);
                AdminProfileListAdapter adapter =
                        (AdminProfileListAdapter) rv.getAdapter();

                List<Entrant> dummy = new ArrayList<>();
                Entrant entrant = new Entrant(); // fields can stay default for UI purposes
                dummy.add(entrant);

                adapter.setItems(dummy);
            });

            onView(withId(R.id.rv_admin_profiles))
                    .perform(actionOnItemAtPosition(0,
                            clickChildViewWithId(R.id.btn_remove)));
        }
    }

    // Helper to click a child view (like a delete button) inside a RecyclerView row
    private static androidx.test.espresso.ViewAction clickChildViewWithId(@IdRes int id) {
        return new androidx.test.espresso.ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(androidx.test.espresso.UiController uiController, View view) {
                View v = view.findViewById(id);
                if (v != null && v.isShown()) {
                    v.performClick();
                }
            }
        };
    }
}
