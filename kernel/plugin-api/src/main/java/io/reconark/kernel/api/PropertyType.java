package io.reconark.kernel.api;

/** Types a plugin configuration property may take. */
public enum PropertyType {
    STRING,
    INTEGER,
    NUMBER,
    BOOLEAN,
    /** ISO-8601 duration such as {@code PT5M}. */
    DURATION,
    LIST,
    MAP
}
