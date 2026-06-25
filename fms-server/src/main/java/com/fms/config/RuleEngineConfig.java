package com.fms.config;

import com.fms.ruleengine.DefaultRuleEngine;
import com.fms.ruleengine.RuleEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuleEngineConfig {

    @Bean
    RuleEngine ruleEngine() {
        return new DefaultRuleEngine();
    }
}
