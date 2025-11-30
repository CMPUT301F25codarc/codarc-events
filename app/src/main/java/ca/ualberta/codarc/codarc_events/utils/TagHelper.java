package ca.ualberta.codarc.codarc_events.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ca.ualberta.codarc.codarc_events.models.Event;

/**
 * Utility class for managing event tags.
 */
public class TagHelper {

    /**
     * Predefined tags available for event categorization.
     */
    private static final List<String> PREDEFINED_TAGS = new ArrayList<>();
    
    static {
        PREDEFINED_TAGS.add("sports");
        PREDEFINED_TAGS.add("music");
        PREDEFINED_TAGS.add("academic");
        PREDEFINED_TAGS.add("social");
        PREDEFINED_TAGS.add("arts");
        PREDEFINED_TAGS.add("technology");
        PREDEFINED_TAGS.add("food");
        PREDEFINED_TAGS.add("outdoor");
    }

    /**
     * Returns the list of predefined tags.
     *
     * @return list of predefined tag strings
     */
    public static List<String> getPredefinedTags() {
        return new ArrayList<>(PREDEFINED_TAGS);
    }

    /**
     * Filters available tags to find matches for the given query.
     *
     * @param query the search query string
     * @param availableTags the list of available tags to search in
     * @return list of tags that match the query
     */
    public static List<String> filterMatchingTags(String query, List<String> availableTags) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>(availableTags);
        }

        String normalizedQuery = normalizeTag(query);
        List<String> matches = new ArrayList<>();

        for (String tag : availableTags) {
            if (tag != null && normalizeTag(tag).contains(normalizedQuery)) {
                matches.add(tag);
            }
        }

        return matches;
    }

    /**
     * Checks if a tag is one of the predefined tags.
     *
     * @param tag the tag to check
     * @return true if the tag is predefined, false otherwise
     */
    public static boolean isPredefinedTag(String tag) {
        if (tag == null) {
            return false;
        }
        return PREDEFINED_TAGS.contains(normalizeTag(tag));
    }

    /**
     * Normalizes a tag string by converting to lowercase and trimming whitespace.
     *
     * @param tag the tag string to normalize
     * @return normalized tag string, or empty string if input is null
     */
    public static String normalizeTag(String tag) {
        if (tag == null) {
            return "";
        }
        return tag.trim().toLowerCase(Locale.US);
    }

    /**
     * Collects all unique tags from a list of events.
     *
     * @param events the list of events to extract tags from
     * @return set of unique tag strings
     */
    public static Set<String> collectAllUniqueTags(List<Event> events) {
        Set<String> allTags = new HashSet<>();

        for (String predefinedTag : PREDEFINED_TAGS) {
            allTags.add(normalizeTag(predefinedTag));
        }

        if (events != null) {
            for (Event event : events) {
                if (event != null && event.getTags() != null) {
                    for (String tag : event.getTags()) {
                        if (tag != null && !tag.trim().isEmpty()) {
                            allTags.add(normalizeTag(tag));
                        }
                    }
                }
            }
        }

        return allTags;
    }
}

