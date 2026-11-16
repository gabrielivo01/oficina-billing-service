package io.github.gabrielivo.oficina.billing.infrastructure.config;

import io.github.gabrielivo.oficina.billing.application.mensageria.BillingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BillingProperties.class)
public class AplicacaoConfiguration {}
