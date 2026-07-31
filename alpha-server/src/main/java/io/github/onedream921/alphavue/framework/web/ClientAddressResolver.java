package io.github.onedream921.alphavue.framework.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Resolves client addresses without trusting forwarding headers by default. */
@Component
public class ClientAddressResolver {
    private final Set<String> trustedProxies;

    public ClientAddressResolver(@Value("${alpha.web.trusted-proxies:}") String configuredProxies) {
        trustedProxies = Arrays.stream(configuredProxies.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    public String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (!trustedProxies.contains(remote)) return remote;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) return remote;
        String candidate = forwarded.split(",", 2)[0].trim();
        return validAddress(candidate) ? candidate : remote;
    }

    private static boolean validAddress(String value) {
        if (value.length() > 64 || value.contains(" ")) return false;
        try { InetAddress.getByName(value); return true; }
        catch (Exception ignored) { return false; }
    }
}
