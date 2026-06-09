package com.finance.payment.common.saga;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/** Auto-configurazione del servizio di deduplicazione eventi saga su PostgreSQL. */
@Configuration
public class SagaEventDedupAutoConfiguration {

    @Bean
    SagaEventDedupService sagaEventDedupService(DataSource dataSource) {
        return new SagaEventDedupService(new JdbcTemplate(dataSource));
    }
}
