package com.prumo.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

class CorrelationIdFilterTest {
    @Test
    void forwardsExistingIdAndReturnsItToCaller() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/accounts")
                .header("X-Correlation-Id", "flow-123"));
        var filter = new CorrelationIdFilter();
        filter.filter(exchange, forwarded -> {
            assertEquals("flow-123", forwarded.getRequest().getHeaders().getFirst("X-Correlation-Id"));
            return Mono.empty();
        }).block();
        exchange.getResponse().setComplete().block();
        assertEquals("flow-123", exchange.getResponse().getHeaders().getFirst("X-Correlation-Id"));
    }
}
