package io.reconark.services.webbff;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code reconark.bff.*}: the route table (API context to backend base URL) and the UI manifest. Both are
 * configuration — adding a service or a UI module is a config change, not a code change.
 *
 * @param devMode local development without an IdP (refused when environment is prod)
 * @param environment environment name
 * @param routes context (e.g. {@code admin}) to backend base URI
 * @param modules UI modules the shell may load
 */
@ConfigurationProperties(prefix = "reconark.bff")
record BffProperties(boolean devMode, String environment, Map<String, URI> routes, List<UiModule> modules) {

    BffProperties {
        environment = environment == null ? "local" : environment;
        routes = routes == null ? Map.of() : Map.copyOf(routes);
        modules = modules == null ? List.of() : List.copyOf(modules);
        if (devMode && "prod".equalsIgnoreCase(environment)) {
            throw new IllegalStateException("RK-SEC-0001 reconark.bff.dev-mode must not be enabled in prod");
        }
    }

    /**
     * One UI module entry of the manifest.
     *
     * @param id module id; must match a module registered in the web-app shell
     * @param title navigation title
     * @param path route prefix
     * @param roles any of these roles grants access; empty = any authenticated user
     * @param enabled switch
     * @param order navigation order
     * @param flag optional feature flag that must also be on
     */
    record UiModule(String id, String title, String path, List<String> roles, boolean enabled, int order, String flag) {
        UiModule {
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }
}
