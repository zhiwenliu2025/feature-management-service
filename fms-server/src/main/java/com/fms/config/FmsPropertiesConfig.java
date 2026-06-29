package com.fms.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({FmsSyncProperties.class, FmsSecurityProperties.class, FmsObservabilityProperties.class})
public class FmsPropertiesConfig {
}
