package com.prumo.gateway;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {
    private static final String HEADER = "X-Correlation-Id";
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String id = correlationId;
        log.info("Encaminhando requisição correlationId={} method={} path={}",
                id, exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath());
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(HEADER, id);
            return Mono.empty();
        });
        return chain.filter(exchange.mutate().request(
                exchange.getRequest().mutate().headers(headers -> headers.set(HEADER, id)).build()).build());
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
