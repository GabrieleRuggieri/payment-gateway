package com.finance.payment.common.processor;

import com.stripe.StripeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Registra {@link PaymentProcessor}: Stripe se {@code payment.processor.provider=stripe} e API key presente,
 * altrimenti mock (anche in fallback se la key manca).
 */
@AutoConfiguration
@EnableConfigurationProperties(ProcessorProperties.class)
public class ProcessorAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ProcessorAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    PaymentProcessor paymentProcessor(ProcessorProperties properties) {
        if (properties.isStripe()) {
            String apiKey = properties.getStripe().getApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("payment.processor.provider=stripe ma stripe.api-key assente — fallback a mock");
                return new MockPaymentProcessor(properties);
            }
            log.info("Payment processor: Stripe (default PM={})", properties.getStripe().getDefaultPaymentMethod());
            StripeClient client = new StripeClient(apiKey);
            return new StripePaymentProcessor(client, properties.getStripe().getDefaultPaymentMethod());
        }
        log.info("Payment processor: mock");
        return new MockPaymentProcessor(properties);
    }
}
