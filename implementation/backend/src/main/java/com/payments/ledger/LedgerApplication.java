package com.payments.ledger;

import com.payments.ledger.config.LedgerProperties;
import com.payments.ledger.config.SampleDataLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class LedgerApplication {

  public static void main(String[] args) {
    ConfigurableApplicationContext context = SpringApplication.run(LedgerApplication.class, args);
    LedgerProperties properties = context.getBean(LedgerProperties.class);
    if (properties.getSampleData().isEnabled()) {
      context.getBean(SampleDataLoader.class).load();
    }
  }
}
