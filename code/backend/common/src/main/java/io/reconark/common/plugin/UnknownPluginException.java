package io.reconark.common.plugin;

import java.util.Set;

/** A definition refers to a plugin id that is not installed or is switched off. */
public class UnknownPluginException extends IllegalArgumentException {

    public UnknownPluginException(String kind, String id, Set<String> available) {
        super("Unknown " + kind + " plugin '" + id + "'. Available: " + available
                + ". Check the id, or enable the plugin with reconark.plugins.<name>.enabled=true.");
    }
}
