/**
 * The plugin TCK. A plugin's tests extend {@link io.reconark.testing.tck.PluginContractTck} plus the TCK of each
 * extension point it provides; CI refuses to release a plugin whose TCK fails. This is what makes bricks
 * interchangeable.
 */
package io.reconark.testing.tck;
