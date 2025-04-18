package com.example.api_gateway.configs;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Service
public class RouterValidator {

    private static final List<Pattern> openEndpointPatterns = List.of(
            Pattern.compile("^/api/v1/auth/.*$"),
            Pattern.compile("^/api/v1/posts/.*$"),
            Pattern.compile("^/api/v1/p/search$")
    );

    public Predicate<ServerHttpRequest> isSecured =
            request -> openEndpointPatterns.stream()
                    .noneMatch(pattern -> pattern.matcher(request.getURI().getPath()).matches());
}
