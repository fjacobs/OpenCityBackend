package com.dynacore.livemap.traveltime.service;

import com.dynacore.livemap.core.service.ServiceConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("traveltime")
@Configuration
@ConfigurationProperties("traveltime")
public class TravelTimeServiceConfig extends ServiceConfiguration {}

