package com.finance.payment.stripe;

import com.finance.payment.common.processor.ProcessorProperties;
import com.stripe.StripeClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/** Bean Stripe per verifica/retrieve webhook inbound sul payment-service. */
@Configuration
@EnableConfigurationProperties({StripeWebhookProperties.class, ProcessorProperties.class})
public class StripeWebhookConfiguration {

    @Bean
    @ConditionalOnProperty(name = "payment.processor.provider", havingValue = "stripe")
    StripeClient stripeClient(ProcessorProperties properties) {
        String key = properties.getStripe().getApiKey();
        if (!StringUtils.hasText(key)) {
            throw new IllegalStateException("STRIPE_API_KEY required when payment.processor.provider=stripe");
        }
        return new StripeClient(key);
    }
}
