package io.reconark.common.plugin;

/** What the UI's plugin catalogue shows about one plugin. */
public record PluginInfo(String kind, String id, String description) {

    public static PluginInfo of(String kind, Plugin plugin) {
        return new PluginInfo(kind, plugin.id(), plugin.description());
    }
}
