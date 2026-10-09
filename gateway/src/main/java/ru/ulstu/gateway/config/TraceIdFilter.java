package ru.ulstu.gateway.config;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TraceIdFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        final String finalTraceId = traceId;
        MDC.put("traceId", finalTraceId);
        exchange.getResponse().getHeaders().add(TRACE_ID_HEADER, finalTraceId);

        // Пробрасываем traceId дальше в целевой сервис
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.header(TRACE_ID_HEADER, finalTraceId))
                .build();

        return chain.filter(mutated).doFinally(signal -> MDC.clear());
    }

    @Override
    public int getOrder() {
        return -1;
    }
}