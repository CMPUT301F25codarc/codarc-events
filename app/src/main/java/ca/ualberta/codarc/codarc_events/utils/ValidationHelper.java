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
}