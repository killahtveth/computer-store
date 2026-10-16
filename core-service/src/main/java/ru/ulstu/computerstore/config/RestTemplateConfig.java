package ru.ulstu.computerstore.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Отключаем keep-alive: каждый запрос — новое соединение,
        // Docker DNS будет каждый раз резолвить имя и вернуть разный IP
        System.setProperty("http.keepAlive", "false");

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        RestTemplate restTemplate = new RestTemplate(factory);

        ClientHttpRequestInterceptor traceIdInterceptor = (request, body, execution) -> {
            String traceId = MDC.get("traceId");
            if (traceId != null) {
                request.getHeaders().add("X-Trace-Id", traceId);
            }
            return execution.execute(request, body);
        };

        restTemplate.setInterceptors(List.of(traceIdInterceptor));
        return restTemplate;
    }
}