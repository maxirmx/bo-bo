package org.webswing.toolkit.port;

import java.util.HashMap;
import java.util.Map;

/**
 * A singleton utility class for handling focus causes.
 * It provides methods to translate symbolic names to corresponding {@link FocusCause} enum values.
 */
public class FocusEventCause {

    /**
     * Enum representing different causes of focus events.
     */
    public enum FocusCause {
        MOUSE_EVENT,
        KEYBOARD_EVENT,
        WINDOW_EVENT,
        ACTIVATION,
        NATIVE_SYSTEM,
        UNKNOWN, FOCUS_LOST, FOCUS_GAINED
    }

    private static final FocusEventCause INSTANCE = new FocusEventCause();

    private final Map<String, FocusCause> symbolicNameToFocusCauseMap = new HashMap<>();

    private FocusEventCause() {
        // Populate the map with known focus causes
        for (FocusCause cause : FocusCause.values()) {
            symbolicNameToFocusCauseMap.put(cause.name(), cause);
        }
    }

    /**
     * Provides the singleton instance of FocusEventCause.
     *
     * @return the singleton instance
     */
    public static FocusEventCause getInstance() {
        return INSTANCE;
    }

    /**
     * Translates a symbolic name to its corresponding FocusCause enum.
     *
     * @param symbolicName the symbolic name of the focus cause
     * @return the FocusCause enum item, or UNKNOWN if not found
     */
    public FocusCause translateSymbolicName(String symbolicName) {
        if (symbolicName == null) {
            return FocusCause.UNKNOWN;
        }
        return symbolicNameToFocusCauseMap.getOrDefault(symbolicName, FocusCause.UNKNOWN);
    }  
}
