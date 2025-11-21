package ca.ualberta.codarc.codarc_events.controllers;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.ualberta.codarc.codarc_events.data.EventDB;
import ca.ualberta.codarc.codarc_events.models.Event;
import ca.ualberta.codarc.codarc_events.utils.TagHelper;

/**
 * Controller for filtering events based on tags and availability.
 * Handles all filtering business logic following MVC architecture.
 */
public class FilterEventsController {

    /**
     * Represents filter criteria for event filtering.
     */
    public static class FilterCriteria {
        private List<String> selectedTags;
        private boolean availableOnly;

        /**
         * Creates filter criteria.
         *
         * @param selectedTags list of selected tags (null or empty = no tag filter)
         * @param availableOnly true to show only events with open registration and available capacity
         */
        public FilterCriteria(List<String> selectedTags, boolean availableOnly) {
            this.selectedTags = selectedTags != null ? new ArrayList<>(selectedTags) : null;
            this.availableOnly = availableOnly;
        }

        /**
         * @return list of selected tags, or null if no tag filter
         */
        public List<String> getSelectedTags() {
            return selectedTags;
        }

        /**
         * @return true if availability filter is enabled
         */
        public boolean isAvailableOnly() {
            return availableOnly;
        }

        /**
         * @return true if any tag filter is applied
         */
        public boolean hasTagFilter() {
            return selectedTags != null && !selectedTags.isEmpty();
        }

        /**
         * @return true if any filter is applied
         */
        public boolean isEmpty() {
            return !hasTagFilter() && !availableOnly;
        }
    }

    /**
     * Result of filtering operation.
     */
    public static class FilterResult {
        private final boolean success;
        private final List<Event> filteredEvents;
        private final String errorMessage;

        private FilterResult(boolean success, List<Event> filteredEvents, String errorMessage) {
            this.success = success;
            this.filteredEvents = filteredEvents != null ? new ArrayList<>(filteredEvents) : new ArrayList<>();
            this.errorMessage = errorMessage;
        }

        /**
         * Creates a successful filter result.
         *
         * @param events the filtered list of events
         * @return FilterResult with success status
         */
        public static FilterResult success(List<Event> events) {
            return new FilterResult(true, events, null);
        }

        /**
         * Creates a failed filter result.
         *
         * @param errorMessage the error message
         * @return FilterResult with failure status
         */
        public static FilterResult failure(String errorMessage) {
            return new FilterResult(false, new ArrayList<>(), errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public List<Event> getFilteredEvents() {
            return filteredEvents;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Callback interface for async filtering operations.
     */
    public interface Callback {
        void onResult(FilterResult result);
    }

    /**
     * Applies filters synchronously to a list of events.
     * Requires accepted participant counts to be pre-fetched for availability filtering.
     *
     * @param allEvents the complete list of events to filter
     * @param criteria the filter criteria
     * @param acceptedCounts map of event ID to accepted participant count
     * @return FilterResult containing filtered events
     */
    public FilterResult applyFilters(List<Event> allEvents, FilterCriteria criteria, Map<String, Integer> acceptedCounts) {
        if (allEvents == null) {
            return FilterResult.failure("Event list is null");
        }

        if (criteria == null || criteria.isEmpty()) {
            return FilterResult.success(new ArrayList<>(allEvents));
        }

        List<Event> filtered = new ArrayList<>();

        for (Event event : allEvents) {
            if (event == null) {
                continue;
            }

            boolean matchesTagFilter = matchesTagFilter(event, criteria);
            boolean matchesAvailabilityFilter = matchesAvailabilityFilter(event, criteria, acceptedCounts);

            // Event must match both filters (AND logic)
            if (matchesTagFilter && matchesAvailabilityFilter) {
                filtered.add(event);
            }
        }

        return FilterResult.success(filtered);
    }

    /**
     * Applies filters asynchronously, fetching waitlist counts internally.
     *
     * @param allEvents the complete list of events to filter
     * @param criteria the filter criteria
     * @param eventDB EventDB instance for fetching waitlist counts
     * @param callback callback to receive the filter result
     */
    public void applyFiltersAsync(List<Event> allEvents, FilterCriteria criteria, EventDB eventDB, Callback callback) {
        if (allEvents == null) {
            callback.onResult(FilterResult.failure("Event list is null"));
            return;
        }

        if (criteria == null || criteria.isEmpty()) {
            callback.onResult(FilterResult.success(new ArrayList<>(allEvents)));
            return;
        }

        // If availability filter is not enabled, we can filter synchronously
        if (!criteria.isAvailableOnly()) {
            Map<String, Integer> emptyCounts = new HashMap<>();
            FilterResult result = applyFilters(allEvents, criteria, emptyCounts);
            callback.onResult(result);
            return;
        }

        // Fetch accepted participant counts for all events
        fetchAcceptedCounts(allEvents, eventDB, new AcceptedCountsCallback() {
            @Override
            public void onCountsReady(Map<String, Integer> counts) {
                FilterResult result = applyFilters(allEvents, criteria, counts);
                callback.onResult(result);
            }

            @Override
            public void onError(Exception e) {
                // On error, treat all events as unavailable (safer default)
                callback.onResult(FilterResult.success(new ArrayList<>()));
            }
        });
    }

    /**
     * Collects all unique tags from a list of events.
     *
     * @param events the list of events
     * @return list of unique tag strings
     */
    public List<String> getAllAvailableTags(List<Event> events) {
        return new ArrayList<>(TagHelper.collectAllUniqueTags(events));
    }

    /**
     * Checks if an event matches the tag filter criteria.
     *
     * @param event the event to check
     * @param criteria the filter criteria
     * @return true if event matches tag filter (or no tag filter is applied)
     */
    private boolean matchesTagFilter(Event event, FilterCriteria criteria) {
        if (!criteria.hasTagFilter()) {
            return true; // No tag filter applied
        }

        List<String> eventTags = event.getTags();
        if (eventTags == null || eventTags.isEmpty()) {
            return false; // Event has no tags, doesn't match
        }

        // Event matches if it has ANY of the selected tags (OR logic)
        for (String selectedTag : criteria.getSelectedTags()) {
            String normalizedSelected = TagHelper.normalizeTag(selectedTag);
            for (String eventTag : eventTags) {
                if (eventTag != null && TagHelper.normalizeTag(eventTag).equals(normalizedSelected)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks if an event matches the availability filter criteria.
     *
     * @param event the event to check
     * @param criteria the filter criteria
     * @param acceptedCounts map of event ID to accepted participant count
     * @return true if event matches availability filter (or no availability filter is applied)
     */
    private boolean matchesAvailabilityFilter(Event event, FilterCriteria criteria, Map<String, Integer> acceptedCounts) {
        if (!criteria.isAvailableOnly()) {
            return true; // No availability filter applied
        }

        // Check registration window
        if (!EventValidationHelper.isWithinRegistrationWindow(event)) {
            return false;
        }

        // Check capacity (based on accepted participants, not waitlist)
        Integer acceptedCount = acceptedCounts != null ? acceptedCounts.get(event.getId()) : null;
        if (acceptedCount == null) {
            // If count is unavailable, treat as unavailable (safer default)
            return false;
        }

        return EventValidationHelper.hasCapacity(event, acceptedCount);
    }

    /**
     * Fetches accepted participant counts for all events.
     */
    private void fetchAcceptedCounts(List<Event> events, EventDB eventDB, AcceptedCountsCallback callback) {
        if (events == null || events.isEmpty()) {
            callback.onCountsReady(new HashMap<>());
            return;
        }

        Map<String, Integer> counts = new HashMap<>();
        final int[] pendingRequests = {events.size()};
        final boolean[] hasError = {false};

        for (Event event : events) {
            if (event == null || event.getId() == null) {
                synchronized (pendingRequests) {
                    pendingRequests[0]--;
                    if (pendingRequests[0] == 0 && !hasError[0]) {
                        callback.onCountsReady(counts);
                    }
                }
                continue;
            }

            eventDB.getAcceptedCount(event.getId(), new EventDB.Callback<Integer>() {
                @Override
                public void onSuccess(Integer count) {
                    synchronized (counts) {
                        counts.put(event.getId(), count != null ? count : 0);
                    }
                    synchronized (pendingRequests) {
                        pendingRequests[0]--;
                        if (pendingRequests[0] == 0 && !hasError[0]) {
                            callback.onCountsReady(counts);
                        }
                    }
                }

                @Override
                public void onError(@NonNull Exception e) {
                    synchronized (pendingRequests) {
                        pendingRequests[0]--;
                        if (pendingRequests[0] == 0) {
                            if (!hasError[0]) {
                                hasError[0] = true;
                                callback.onError(e);
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Callback interface for accepted participant counts fetching.
     */
    private interface AcceptedCountsCallback {
        void onCountsReady(Map<String, Integer> counts);
        void onError(Exception e);
    }
}

