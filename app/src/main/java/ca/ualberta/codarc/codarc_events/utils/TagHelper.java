package ca.ualberta.codarc.codarc_events.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Utility class for managing event tags.
 * Provides predefined tags, tag matching, and normalization functionality.
 */
public class TagHelper {

    /**
     * Predefined tags available for event categorization.
     */
    private static final List<String> PREDEFINED_TAGS = new ArrayList<String>() {{
        add("sports");
        add("music");
        add("academic");
        add("social");
        add("arts");
        add("technology");
        add("food");
        add("outdoor");
    }};

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
     * Performs case-insensitive partial matching.
     *
     * @param query the search query string
     * @param availableTags the list of available tags to search in
     * @return list of tags that match the query (case-insensitive)
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
     * Combines predefined tags with custom tags found in events.
     *
     * @param events the list of events to extract tags from
     * @return set of unique tag strings (normalized)
     */
    public static Set<String> collectAllUniqueTags(List<? extends ca.ualberta.codarc.codarc_events.models.Event> events) {
        Set<String> allTags = new HashSet<>();

        // Add all predefined tags
        for (String predefinedTag : PREDEFINED_TAGS) {
            allTags.add(normalizeTag(predefinedTag));
        }

        // Add tags from events
        if (events != null) {
            for (ca.ualberta.codarc.codarc_events.models.Event event : events) {
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

