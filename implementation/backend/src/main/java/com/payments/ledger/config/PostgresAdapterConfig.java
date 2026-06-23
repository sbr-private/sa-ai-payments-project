package com.payments.ledger.config;

import com.payments.ledger.adapter.postgres.PostgresLedgerRepository;
import com.payments.ledger.repository.LedgerRepository;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@ConditionalOnProperty(name = "ledger.database", havingValue = "postgres")
@EnableConfigurationProperties(DataSourceProperties.class)
public class PostgresAdapterConfig {

  @Bean
  public DataSource dataSource(DataSourceProperties properties) {
    return properties.initializeDataSourceBuilder().build();
  }

  @Bean
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  public LedgerRepository ledgerRepository(JdbcTemplate jdbcTemplate) {
    return new PostgresLedgerRepository(jdbcTemplate);
  }
}
