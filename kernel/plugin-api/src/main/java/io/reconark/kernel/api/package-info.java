/**
 * The reconArk plugin contract.
 *
 * <p>Everything a plugin needs to plug into the platform lives here: the {@link io.reconark.kernel.api.ReconArkPlugin}
 * entry point, its {@link io.reconark.kernel.api.PluginDescriptor}, typed {@link io.reconark.kernel.api.ExtensionPoint}s
 * and the {@link io.reconark.kernel.api.ExtensionRegistrar} through which a plugin contributes implementations.
 * This package has no dependencies beyond the JDK and is versioned by {@link io.reconark.kernel.api.KernelApi#VERSION}.
 */
package io.reconark.kernel.api;
