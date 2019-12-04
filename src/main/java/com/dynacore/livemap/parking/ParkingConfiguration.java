package com.dynacore.livemap.parking;

import com.dynacore.livemap.core.service.GeoJsonRequestConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("parking")
public class ParkingConfiguration extends GeoJsonRequestConfiguration {

}
