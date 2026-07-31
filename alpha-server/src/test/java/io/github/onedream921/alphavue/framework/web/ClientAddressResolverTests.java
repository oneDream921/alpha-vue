package io.github.onedream921.alphavue.framework.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientAddressResolverTests {
    @Test
    void ignoresForwardedAddressUnlessRemotePeerIsTrusted() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.8");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.4");

        assertThat(new ClientAddressResolver("").resolve(request)).isEqualTo("10.0.0.8");
        assertThat(new ClientAddressResolver("10.0.0.8").resolve(request)).isEqualTo("203.0.113.4");
    }

}
