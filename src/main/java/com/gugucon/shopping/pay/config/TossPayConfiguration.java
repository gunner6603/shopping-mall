package com.gugucon.shopping.pay.config;

import com.gugucon.shopping.pay.infrastructure.OrderIdBase64Translator;
import com.gugucon.shopping.pay.infrastructure.OrderIdTranslator;
import com.gugucon.shopping.pay.infrastructure.TossPayValidator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
@Getter
public class TossPayConfiguration {

    private final RestTemplate restTemplate;
    @Value("${pay.callback.success-url}")
    private String successUrl;
    @Value("${pay.callback.fail-url}")
    private String failUrl;

    @Bean
    public OrderIdTranslator orderIdTranslator() {
        return new OrderIdBase64Translator();
    }

    @Bean
    public TossPayValidator payValidator(@Value("${pay.toss.secret-key}") final String secretKey) {
        return new TossPayValidator(restTemplate, secretKey);
    }
}
