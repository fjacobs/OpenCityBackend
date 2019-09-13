package com.dynacore.livemap.guidancesign;

import com.dynacore.livemap.common.service.GeoJsonRequestConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("guidancesign")
public class GuidanceSignConfiguration extends GeoJsonRequestConfiguration {
}
