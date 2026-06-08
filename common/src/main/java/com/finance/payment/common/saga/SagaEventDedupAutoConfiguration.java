package com.finance.payment.common.saga;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnSingleCandidate(JdbcTemplate.class)
@Import(SagaEventDedupService.class)
public class SagaEventDedupAutoConfiguration {
}
