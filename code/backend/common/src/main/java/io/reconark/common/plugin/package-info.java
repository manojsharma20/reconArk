/**
 * The plugin interfaces: reconArk's "Lego sockets".
 *
 * <p>Each interface is one kind of brick. A plugin is a class that implements one of these interfaces, lives in its own
 * module under {@code backend/plugins/}, and is switched on or off with {@code reconark.plugins.<plugin-name>.enabled}.
 * Engines and services only ever talk to these interfaces, never to a concrete plugin.
 */
package io.reconark.common.plugin;
