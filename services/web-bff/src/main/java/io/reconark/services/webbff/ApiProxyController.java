package io.reconark.services.webbff;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

/**
 * Forwards {@code /api/{context}/**} to the backend named in the route table, attaching the user's access token.
 * Targets come only from configuration (no SSRF); only an allow-list of headers is forwarded in each direction.
 */
@RestController
class ApiProxyController {

    private static final Set<String> REQUEST_HEADERS = Set.of(
            HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, HttpHeaders.IF_MATCH, "Idempotency-Key", "traceparent");
    private static final Set<String> RESPONSE_HEADERS = Set.of(
            HttpHeaders.CONTENT_TYPE, HttpHeaders.ETAG, HttpHeaders.LOCATION, HttpHeaders.RETRY_AFTER);

    private final BffProperties props;
    private final RestClient http;
    private final ObjectProvider<OAuth2AuthorizedClientService> clients;

    ApiProxyController(BffProperties props, RestClient.Builder builder, ObjectProvider<OAuth2AuthorizedClientService> clients) {
        this.props = props;
        this.http = builder.build();
        this.clients = clients;
    }

    @RequestMapping("/api/{context}/**")
    ResponseEntity<byte[]> forward(
            @PathVariable String context,
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body,
            Authentication auth) {
        URI base = props.routes().get(context);
        if (base == null || !context.matches("[a-z]{2,20}")) {
            return ResponseEntity.notFound().build();
        }
        String query = request.getQueryString();
        URI target = base.resolve(request.getRequestURI() + (query == null ? "" : "?" + query));
        RestClient.RequestBodySpec spec = http.method(HttpMethod.valueOf(request.getMethod()))
                .uri(target)
                .headers(h -> {
                    REQUEST_HEADERS.forEach(name -> {
                        String v = request.getHeader(name);
                        if (v != null) {
                            h.set(name, v);
                        }
                    });
                    bearer(auth).ifPresent(h::setBearerAuth);
                    if (props.devMode()) {
                        h.set("X-Dev-User", auth.getName());
                    }
                });
        if (body != null && body.length > 0) {
            spec.body(body);
        }
        return spec.exchange((req, res) -> {
            HttpHeaders out = new HttpHeaders();
            RESPONSE_HEADERS.forEach(name -> {
                String v = res.getHeaders().getFirst(name);
                if (v != null) {
                    out.set(name, v);
                }
            });
            return ResponseEntity.status(res.getStatusCode()).headers(out).body(res.getBody().readAllBytes());
        });
    }

    private java.util.Optional<String> bearer(Authentication auth) {
        OAuth2AuthorizedClientService service = clients.getIfAvailable();
        if (service == null || !(auth instanceof OAuth2AuthenticationToken token)) {
            return java.util.Optional.empty();
        }
        OAuth2AuthorizedClient client = service.loadAuthorizedClient(token.getAuthorizedClientRegistrationId(), token.getName());
        return client == null ? java.util.Optional.empty() : java.util.Optional.of(client.getAccessToken().getTokenValue());
    }
}
