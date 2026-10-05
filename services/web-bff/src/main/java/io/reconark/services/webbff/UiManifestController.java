package io.reconark.services.webbff;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code /bff/me} and {@code /bff/ui-manifest}: which UI bricks this user sees in this environment. */
@RestController
@RequestMapping("/bff")
class UiManifestController {

    private final BffProperties props;

    UiManifestController(BffProperties props) {
        this.props = props;
    }

    record Me(String name, Set<String> roles) {}

    record Manifest(String environment, List<BffProperties.UiModule> modules) {}

    @GetMapping("/me")
    Me me(Authentication auth) {
        return new Me(auth.getName(), roles(auth));
    }

    @GetMapping("/ui-manifest")
    Manifest manifest(Authentication auth) {
        Set<String> roles = roles(auth);
        List<BffProperties.UiModule> visible = props.modules().stream()
                .filter(BffProperties.UiModule::enabled)
                .filter(m -> m.roles().isEmpty() || m.roles().stream().anyMatch(roles::contains))
                .sorted(Comparator.comparingInt(BffProperties.UiModule::order))
                .toList();
        return new Manifest(props.environment(), visible);
    }

    private static Set<String> roles(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .collect(Collectors.toUnmodifiableSet());
    }
}
