package com.prumo.transaction.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class CorrelationPropagationTest {
    @Test
    void forwardsSameIdToAccountAndAuth() throws Exception {
        var seen = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        UUID owner = UUID.randomUUID();
        server.createContext("/accounts/", exchange -> {
            seen.set(exchange.getRequestHeaders().getFirst("X-Correlation-Id"));
            byte[] body = ("{\"ownerId\":\"" + owner + "\",\"currency\":\"BRL\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.createContext("/internal/sessions/introspect", exchange -> {
            seen.set(exchange.getRequestHeaders().getFirst("X-Correlation-Id"));
            byte[] body = ("{\"userId\":\"" + owner + "\"}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            MDC.put("correlationId", "flow-123");
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            assertEquals(owner, new AccountClient(base).requireOwner(UUID.randomUUID(), "Bearer token"));
            assertEquals("flow-123", seen.get());
            assertEquals(owner, new IdentityClient(base, "12345678901234567890123456789012")
                    .requireUser("Bearer token"));
            assertEquals("flow-123", seen.get());
        } finally {
            MDC.remove("correlationId");
            server.stop(0);
        }
    }
}
