package com.finance.payment.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Bean HTTP per la consegna dei webhook (con token interno opzionale). */
@Configuration
public class AppConfig {

    @Bean
    RestTemplate restTemplate(
            @Value("${payment.internal.token:}") String internalToken,
            @Value("${payment.internal.header:X-Internal-Token}") String internalHeader) {
        RestTemplate restTemplate = new RestTemplate();
        if (StringUtils.hasText(internalToken)) {
            List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
            interceptors.add((request, body, execution) -> {
                request.getHeaders().set(internalHeader, internalToken);
                return execution.execute(request, body);
            });
            restTemplate.setInterceptors(interceptors);
        }
        return restTemplate;
    }
}
