package com.dmg.movieticketing.hold.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HoldProperties.class)
public class HoldConfiguration {
}
