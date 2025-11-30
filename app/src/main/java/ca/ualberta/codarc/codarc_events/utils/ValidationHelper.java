package ca.ualberta.codarc.codarc_events.utils;

/**
 * Utility class for common validation patterns.
 * Provides methods to validate input parameters and throw appropriate exceptions.
 */
public class ValidationHelper {

    /**
     * Validates that a string is not null or empty.
     *
     * @param value the string to validate
     * @param fieldName the name of the field for error messages
     * @throws IllegalArgumentException if value is null or empty
     */
    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    /**
     * Validates that an object is not null.
     *
     * @param value the object to validate
     * @param fieldName the name of the field for error messages
     * @throws IllegalArgumentException if value is null
     */
    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }

    /**
     * Validates that a string is not null or empty, and returns the trimmed value.
     *
     * @param value the string to validate and trim
     * @param fieldName the name of the field for error messages
     * @return the trimmed string
     * @throws IllegalArgumentException if value is null or empty
     */
    public static String requireNonEmptyAndTrim(String value, String fieldName) {
        requireNonEmpty(value, fieldName);
        return value.trim();
    }
}
