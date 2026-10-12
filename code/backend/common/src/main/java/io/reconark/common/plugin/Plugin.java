package io.reconark.common.plugin;

/** Common to every plugin: a short unique id (used in configuration) and a description (shown in the UI). */
public interface Plugin {

    /** Short, unique, lower-case id used in recon definitions, e.g. {@code csv} or {@code number}. */
    String id();

    /** One sentence for the UI's plugin catalogue. */
    String description();
}
