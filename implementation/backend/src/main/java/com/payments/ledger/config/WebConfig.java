package com.payments.ledger.config;

import com.payments.ledger.api.auth.DemoAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final DemoAuthInterceptor demoAuthInterceptor;

  public WebConfig(DemoAuthInterceptor demoAuthInterceptor) {
    this.demoAuthInterceptor = demoAuthInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(demoAuthInterceptor)
        .excludePathPatterns("/health", "/auth/login");
  }
}
